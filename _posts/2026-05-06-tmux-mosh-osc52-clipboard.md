---
layout: post
title: Tracking Down a Broken Copy/Paste Across tmux, mosh, and Ghostty
date: 2026-05-06 00:00:00
published: false
---

I keep most of my interactive work inside a tmux session on a Linux
box that I reach with [mosh][mosh] from a [Ghostty][ghostty] terminal
on macOS. For a long time, I'd put up with the fact that mouse-selecting
text in tmux didn't make it to the macOS clipboard---I'd reach for
`tmux save-buffer - | pbcopy` over a separate ssh, or just retype short
strings.  Eventually I got tired of it and decided to actually fix it.

What started as a one-line tmux config change turned into a multi-hour
detour through the [OSC 52][osc52] specification, tmux's
`terminal-features`, mosh's escape-sequence filter, and the
default keybindings of `copy-mode`. I want to write this up because
several of the dead ends had compelling-sounding explanations that
turned out to be wrong, and the ultimate fix is a single config block
that's worth lifting verbatim if you're in the same situation.

# The setup

The chain of programs is:

- **Ghostty** on macOS---the host terminal emulator. Owns the system
  clipboard.
- **mosh** as the remote-shell transport. UDP-based, roaming-friendly,
  and crucially *not* byte-transparent: mosh runs a state machine on
  both ends and selectively forwards escape sequences.
- **tmux 3.5a** running on the remote Linux machine.
- Whatever process is running inside a tmux pane (a shell, an editor,
  a TUI).

The goal is for a mouse drag in tmux to put the selection on macOS's
clipboard. The mechanism is [OSC 52][osc52], a standard X-term-derived
escape sequence that lets a terminal-side application say "please put
this base64-encoded blob into the system clipboard":

```
ESC ] 52 ; <selection> ; <base64-data> BEL
```

The `<selection>` field tells the terminal which clipboard buffer to
target: `c` for the system clipboard, `s` for primary selection, `p`
for an X11-style selection, or---tellingly for what comes later---an
empty string.

# False start: just turn on `set-clipboard`

The advice you'll find on most blog posts is some variant of:

```
# in ~/.tmux.conf
set -s set-clipboard on
set -ag terminal-overrides ',*:Ms=\E]52;c;%p2%s\7'
```

The `set-clipboard on` part tells tmux that the outer terminal
supports OSC 52 and that tmux should both *accept* it from inner
programs and *emit* it for its own copy-mode operations. The
`terminal-overrides` line patches the `Ms` capability into tmux's
copy of the outer terminfo entry, because `tmux-256color` from ncurses
omits `Ms` and tmux refuses to emit OSC 52 without it.

I deployed that. Restarted the tmux server. Confirmed both options
were live:

```
$ tmux show-options -sv set-clipboard
on
$ tmux show-options -sv terminal-overrides
linux*:AX@
xterm*:Tc:smcup@:rmcup@
*256col*:Tc
*:Ms=\E]52;c;%p2%s\7
```

Mouse selection still didn't reach the clipboard. Neither did a manual
OSC 52 emitted by `printf` from inside a pane.

# Diversion: `terminal-features`

Suspicion fell on the terminfo `Ms` override syntax---specifically the
`\7` at the end, which is non-standard (terminfo wants `\007` or
`^G`).  The modern, more idiomatic way to tell tmux that the outer
terminal supports OSC 52 is `terminal-features`, which was added in
tmux 3.2 to bypass the terminfo path entirely:

```
set -s set-clipboard on
set -as terminal-features ',*:clipboard'
```

I made the swap. `tmux info` now showed `Ms` as
`\E]52;%p1%s;%p2%s\a`---the canonical two-parameter form, derived from
the feature flag instead of my hand-rolled override. This was an
improvement on principle, but it didn't fix the symptom on its own.

# The first real datapoint

The next thing I tried was bypassing tmux's own copy machinery
entirely and sending OSC 52 directly from a shell inside the pane:

```
printf '\033]52;c;%s\a' "$(printf 'osc52-via-tmux' | base64 | tr -d '\n')"
```

That worked. The string landed in the macOS clipboard. So:

- Ghostty handles OSC 52 correctly.
- mosh forwards OSC 52 from the remote side to Ghostty.
- tmux passes OSC 52 from inside a pane out to its outer client (or
  intercepts and re-emits it---more on this in a moment).
- The terminfo / terminal-features setup is functional.

But mouse selection still produced nothing. Two paths through the
same plumbing: one works, one doesn't. That's the whole rest of the
post.

# What `set-clipboard on` is actually doing

The tmux man page sounds straightforward:

> If set to `on`, tmux will both accept the escape sequence to create
> a buffer and attempt to set the terminal clipboard.

I assumed "attempt to set the terminal clipboard" applied to *both*
the inner-program OSC 52 path and tmux's own copy-mode operations. To
verify, I ran the manual `printf` test and then immediately checked
the tmux paste buffer:

```
$ printf '\033]52;c;%s\a' "$(printf 'osc52-via-tmux' | base64 | tr -d '\n')"
$ tmux show-buffer
osc52-via-tmux
```

So tmux *was* intercepting the OSC 52 from the inner pane, decoding
it into a paste buffer, and (presumably) re-emitting via `Ms` to the
outer client. Both paths---inner-program intercept and tmux's own
copy-mode---should funnel through the same `Ms` emission machinery.
The bytes on the wire should be identical.

They aren't. I confirmed this by digging into the tmux source. The
function `tty_set_selection` is what emits OSC 52 to the outer client;
it takes a `flags` argument that fills in the `<selection>` field.

- For OSC 52 *received* from an inner program, tmux passes the
  inner program's flag through. Our `printf` used `c`, so tmux emits
  `\E]52;c;<b64>\a`.
- For tmux's *own* copy-mode emit, the corresponding caller passes
  an empty string, so tmux emits `\E]52;;<b64>\a`.

The empty-selection form is technically valid in the [original xterm
spec][xterm-ctl-seqs] (the selector field can be a comma-separated
list and an empty list "uses the configured default"), but in
practice many terminals and intermediate filters don't accept it.
Ghostty on its own actually does accept it---I confirmed this later by
testing locally without mosh in the chain---but mosh's OSC 52
parser does not.

# Why mosh is the difference

[mosh added OSC 52 support in 1.4.0][mosh-osc52], released March 2022.
The relevant logic lives in `src/terminal/terminaldispatcher.cc`,
which expects a non-empty selector. With an empty `<selection>`
field, mosh's parser rejects (or silently drops) the sequence, and
the OSC 52 never reaches Ghostty.

This explains the cleanest piece of evidence I had been ignoring:
when I tested the same tmux 3.5a config locally on a macOS box
(no mosh in the path), mouse selection populated the clipboard
without any of the workarounds. Ghostty tolerates the empty-selector
form; mosh does not.

The chain narrows to:

```
inner program
   ↓ OSC 52 with selector "c"
tmux (intercept + re-emit via Ms)
   ↓ OSC 52 with selector "" (empty)
mosh
   ✗ filtered, never forwarded
Ghostty
   never sees the sequence
```

# The fix

There are two principled places this could be patched: tmux could
default to `c` for its own emits, or mosh could accept an empty
selector. Both are reasonable upstream targets. In the meantime, the
working-today fix is to bypass tmux's own emit and route copy-mode
through `copy-pipe`, where we can produce the OSC 52 ourselves with
an explicit selector and write it to the pane's tty---which puts us
back on the inner-program-intercept path that we already know works:

```
# ~/.tmux.conf
set -s set-clipboard on
set -as terminal-features ',*:clipboard'

bind-key -T copy-mode-vi MouseDragEnd1Pane \
  send -X copy-pipe-and-cancel \
  '{ printf "\033]52;c;"; base64 -w0; printf "\a"; } > #{pane_tty}'
bind-key -T copy-mode    MouseDragEnd1Pane \
  send -X copy-pipe-and-cancel \
  '{ printf "\033]52;c;"; base64 -w0; printf "\a"; } > #{pane_tty}'
bind-key -T copy-mode-vi Enter \
  send -X copy-pipe-and-cancel \
  '{ printf "\033]52;c;"; base64 -w0; printf "\a"; } > #{pane_tty}'
bind-key -T copy-mode    Enter \
  send -X copy-pipe-and-cancel \
  '{ printf "\033]52;c;"; base64 -w0; printf "\a"; } > #{pane_tty}'
```

A few notes on what's going on in that block:

- `set-clipboard on` is still required so tmux *accepts* OSC 52
  emitted by inner programs (including the one we generate ourselves
  in the pipe command). Without it, our injected OSC 52 gets dropped
  by tmux before it reaches mosh.
- `terminal-features ',*:clipboard'` populates the `Ms` capability so
  tmux is happy to forward OSC 52 in the first place.
- `copy-pipe-and-cancel` runs a shell command with the selection
  piped to it on stdin. The command emits `\E]52;c;<base64>\a` and
  redirects the output to `#{pane_tty}`---which tmux expands to the
  current pane's slave pty path (e.g.\ `/dev/pts/8`) before exec.
- `> /dev/tty` does *not* work here because the shell tmux spawns for
  `copy-pipe` has no controlling terminal. This was a non-obvious
  pitfall: the manual command-line test
  `echo foo | { ...; } > /dev/tty` works because the test shell *does*
  have a controlling tty, but the tmux-spawned shell doesn't.
- `base64 -w0` is GNU coreutils. macOS's BSD `base64` doesn't have
  `-w` and base64-encodes without wrapping by default, so on macOS
  tmux you'd write `base64` and on Linux you write `base64 -w0`. If
  you sync this config across both, swap in `base64 | tr -d '\n'`
  which is portable.
- The bindings for `Enter` cover the keyboard copy path
  (`prefix [`, select, Enter) for symmetry with mouse drag. Mode
  `vi` vs `emacs` depends on `mode-keys`; binding both is harmless.

Whichever copy command tmux's *default* `Mouse­DragEnd1Pane` runs
(`copy-pipe-and-cancel` with no argument in 3.5a), the empty-selector
emit problem still applies, so simply swapping in
`copy-selection-and-cancel` doesn't help. Routing through
`copy-pipe` with an explicit emission command is what actually moves
the bytes.

# A sanity-checking ladder

If you're debugging this from scratch, the diagnostic ladder I wish
I'd followed is:

1. From an SSH-only (no tmux, no mosh) session, emit OSC 52 with
   `printf` and ⌘V. Tests the terminal emulator's OSC 52 handling.
2. From a mosh session (no tmux), do the same. Tests mosh's OSC 52
   forwarding.
3. From inside a tmux pane (over mosh), emit OSC 52 with `printf`.
   Tests tmux's intercept-and-re-emit path.
4. From inside tmux, mouse-select and check `tmux show-buffer`. If
   the buffer is populated, tmux's *internal* copy works; the
   problem is in the emit stage.
5. From inside tmux, mouse-select and check the macOS clipboard. If
   step 4 passes but this fails, you're in the same situation I was.

This sequence isolates exactly which segment of the pipeline is
dropping bytes, and it's a much better use of time than rotating
through `set-clipboard` / `terminal-overrides` / `terminal-features`
combinations and hoping.

# References

- [OSC 52 in xterm's control sequences documentation][xterm-ctl-seqs]
- [tmux man page: `set-clipboard`, `terminal-features`,
  `copy-pipe-and-cancel`][tmux-man]
- [mosh OSC 52 support landed in 1.4.0 (2022)][mosh-osc52]
- [tmux issue #2425, on `set-clipboard` and selection
  flags][tmux-2425]
- [Ghostty's `clipboard-write` config option][ghostty-clipboard]

[mosh]: https://mosh.org
[mosh-osc52]: https://github.com/mobile-shell/mosh/releases/tag/mosh-1.4.0
[ghostty]: https://ghostty.org
[ghostty-clipboard]: https://ghostty.org/docs/config/reference#clipboard-write
[osc52]: https://invisible-island.net/xterm/ctlseqs/ctlseqs.html#h3-Operating-System-Commands
[xterm-ctl-seqs]: https://invisible-island.net/xterm/ctlseqs/ctlseqs.html
[tmux-man]: https://man7.org/linux/man-pages/man1/tmux.1.html
[tmux-2425]: https://github.com/tmux/tmux/issues/2425
