{:layout :post
 :title "Migrating to NixOS"
 :date "2019-03-14"}

After running [Arch Linux][1] for the last decade, I've finally made
the jump to [NixOS][2]. For me, this means updating two VMs
(VirtualBox and VMWare) and a bare-metal install (an aging MacBook
Air).

I've repurposed my old [config repo][3] to store both my dotfiles as
 well as the NixOS `configuration.nix` files.

Since I was already making a big transition, I decided to take the
opportunity to retool a few more things in my dev setup:

|                | Old        | New       |
|----------------|------------|-----------|
| OS             | Arch Linux | NixOS     |
| Shell          | Bash       | Zsh       |
| Terminal       | urxvt      | Alacritty |
| Multiplexer    | screen     | tmux      |
| Window Manager | XMonad     | i3        |
| Editor         | Emacs      | Emacs     |

I initially wanted to make the jump from X11 to Wayland, but NixOS
[isn't quite ready][4] just yet.

My goal for this writeup is to document the rationale for making the
switch, capture the stuff I wish I had known before diving into the
Nix language, and describe the particulars of how I organize my new
setup.

# Motivation

While I lack a single compelling reason to make the jump, there are a
few pain points with my Arch setup that, together, pushed me to give
NixOS a shot:

- **Falling behind on Arch changes.** While I benefited a few times
  from Arch's rolling update process, in practice I've rarely found it
  was something I needed. Not staying on top of Arch updates
  invariably leads to painful upgrades that take time to work
  through. Taking snapshots of my VMs reduced a lot of this upgrade
  risk, but it takes more time than I'm willing to spend to upgrade my
  bare-metal Arch install after neglecting it for extended periods.

- **Package drift among machines.** Having my VMs get slightly
  different versions of packages from my Linux laptop, or forgetting
  to install the same set of packages across all machines was a minor
  but consistent annoyance. I kept a list of arch packages that I'd
  move from machine to machine, but nothing forced me to audit that
  the installed packages matched the list.

- **Limited local install options.** I've grown reliant on Docker for
  infrastructural components (e.g. Postgres), but being able to
  install specific dev tools on a per-project basis (I've been playing
  with [QGIS][10] recently) is something I've constantly found
  painful, the few times I've bothered at all.

# Nix

The big ideas behind the Nix ecosystem are covered in [detail
elsewhere][5]; what was appealing to me in particular was Nix's
emphasis on reproducibility, file-driven configuration, and functional
approach to its package repository, nixpkgs. You can think of the Nix
package manager as a hybrid of `apt-get` and Python's `virtualenv`
with a sprinkling of `git`; you can use Nix to build multiple,
isolated sets of packages on, say, a per-project basis, with the
guarantee that Nix only needs to fetch (or build) shared dependencies
once. Nix stores all built packages in the Nix store which serves as a
local cache. Nix grafts together a collection of Linux directories
(`bin`, `usr`, etc.) by symlinking the appropriate files contained in
the packages that live in the Nix store. This isolated environment can
be system-wide (in the case of NixOS), local to your user (`nix-env`)
or tailed for a specific project (`nix-shell`).

`nix-shell` serves a few different roles in the Nix ecosystem, but one
of those roles is to make dependencies defined in a "derivation"
(Nix's version of a makefile) available for use in a shell. These
derivations are used to define a hermetically-sealed environment for
building a package as well as collecting the commands to configure and
run a build. We can re-use just the environment-prep part of a
derivation along with `nix-shell` to drop us into a terminal that has
exactly the packages we want. Here's an example of a derivation for a
TeX project:

```nix
with import <nixpkgs> {};

stdenv.mkDerivation {
  name = "my-document";
  buildInputs = with pkgs; [
    texlive.combined.scheme-full
  ];
  shellHook = "pdflatex document.tex"
}
```

With this derivation placed in `shell.nix`, running a `nix-shell` in
the same directory will fetch the entirety of TeX Live (which is [not
small](https://tex.stackexchange.com/questions/302676/how-large-is-the-full-install-of-texlive/323739))
and make all the related files, configuration, tools, fonts, commands,
etc. available in the shell. It then uses one of these tools
(`pdflatex`) to run the "build" of `document.tex` to generate a
PDF. Writing a full derivation file isn't necessary if you don't need
to be dropped into a shell for further work. The following is
equivalent to the derivation above, but does not keep TeX Live
available in the shell after it is done building the document:

```nix
nix run nixpkgs.texlive.combined.scheme-full -c pdflatex document.tex
```

I only rarely need TeX, so being able to make TeX available on a
per-project basis without having all its commands pollute my `PATH`
when doing non-TeX work is useful. Going further, I can mix-and-match
versions of Python, the JVM, Postgres, etc. independently for each
project I have without having to use `sudo`.

# nixpkgs

While the Nix Expression Language is somewhat esoteric, the big ideas
aren't far removed from features in mainstream functional
languages. nixpkgs in particular can be conceptualized as a single
large map (called an Attribute Set or attrset in Nix) from keys to
derivations:

```nix
{
  # <snip>
  tmux = callPackage ../tools/misc/tmux { };
  # <snip>
}
```

You can see a meaty example of nixpkg's package list [here][6]. This
would normally be an unwieldy thing to build in memory on every
interaction with the package manager, however Nix lazily loads the
contents of this attrset. Nix even provides the option to make these
attribute sets "recursive" allowing the values to reference sibling
keys, e.g.

```nix
rec { a = 2; b = a+3; }
```

nixpkgs provides [facilities][11] to change or update existing
packages with custom configuration, and add new entries to the package
attrset. It does this by way of "overlays" which are a [fixed
point][12] over the package attrset. Nix's approach of effectively
rebuilding a facsimile of the [FHS][13] on every run means that
"manual" intervention to install things outside of a package manager
(say, copying a `ttf` font into `/usr/share/fonts`) is not feasible,
so having an easy way to fold your own set of custom packages into the
package attrset is vital.

The other important aspect to nixpkgs is that it is versioned in git
(conveniently alongside NixOS in the same repo). The Nix CLI tools can
fetch and install the latest set of packages by rolling the local
clone of nixpkgs forward and then rebuilding your packages. Such a
rebuild can apply to all the packages on your entire system, or just a
particular derivation's local packages. This can work the other
direction as well: If you prefer your package set to remain completely
fixed, you can pin the nixpkgs clone to a particular git SHA. Stable
releases of NixOS are handled as branches of the nixpkgs repo, which
do get critical updates but avoid all the bleeding-edge changes that
the `master` branch has.

# NixOS

NixOS goes a step further and utilizes attrsets to configure the OS
itself. Not unlike application configuration (for which there
[are](https://github.com/lightbend/config)
[numerous](https://github.com/markbates/configatron)
[libraries](https://github.com/weavejester/environ)), NixOS defines
your OS in a series of one or more attrsets that are merged together;
unlike traditional configuration approaches that use a
last-merged-wins strategy, however, NixOS's [properties][7] provide
per-field control over the priority of merges along with conditionals
that control whether an option is merged or not.

This approach to OS configuration is useful for defining options
amongst a set of similar but not identical OSs. For my NixOS config,
I've created a base [`configuration.nix`][8] file that contains common
options that I want set across all my machines (abbreviated example
here):

```nix
{ config, pkgs, ... }:
{
  time.timeZone = "America/Chicago";
  environment.systemPackages = with pkgs; [feh vim wget];
  programs.zsh.enable = true;
  users.users.johndoe.shell = pkgs.zsh;
  # <snip>
}
```

I then import this common file into host-specific files that each
contain options specific to that particular machine, e.g. a VM host:

```nix
{ config, pkgs, ... }:
{
  imports = [ ./configuration.nix ];
  services.vmwareGuest.enable = true;
  users.users.johndoe.shell = mkOptionDefault pkgs.bash;
  # <snip>
}
```

Note the `mkOptionDefault` function that reduces the priority of the
`pkgs.bash` value from the default of 100 to 1500. Had I left off
`mkOptionDefault`, NixOS would complain that `johndoe.shell` was
declared twice. However, by reducing its priority, the
`configuration.nix`'s definition of `johndoe.shell = pkgs.zsh` will
take priority, despite it not being the "last" merged. In actuality,
NixOS builds the configuration as a whole without any notion of
ordering, and will fail loudly if it gets two property values with
equal priority.

Notice above that the NixOS configuration includes option values that
range from plain strings (e.g. `time.timeZone`) to more complex
`services` that wire up nontrivial operations (schedule daemons to
auto start, create systemd services, modprobe kernel modules,
etc.). Unlike nixpkgs, NixOS doesn't try to specify all these
configuration options in a giant flat file; rather, it splits options
into [modules][16] which keep options grouped into logical
units. Modules let you create new options easily, as well at attach a
meaning to each option by doing things such as configuring other
module's options, composing other modules together, writing files
(also done through options, interestingly), and assorted other
activities.

To introduce new options that vary among my work VMs and my personal
laptop, I've written a [custom NixOS module][17], which looks like

```nix
{config, pkgs, lib, ...}:

with lib;

{
  options = {
    settings = {
      username = mkOption {
        default = "malloc47";
        type = with types; uniq string;
      };
      email = mkOption {
        default = "malloc47@gmail.com";
        type = with types; uniq string;
      };
      # more options
    }
  }
}
```

This module lets me set a username for the machine being built, the
keyboard layout I want to use, the email I want to use (for my git
configuration), and many other options. I've written this module as a
container of values for other modules to read, but takes no action
itself (this is a trick so I can re-use the module for home-manger,
discussed below).  However, upon importing this module elsewhere, I
can set or retrieve values for these options to parameterize the rest
of my configuration. E.g.,

```nix
users.users.${config.settings.username}.shell = pkgs.zsh;
```

NixOS helpfully keeps a [large index][20] of all options across all
modules defined in the base NixOS system, which is also available in
`man` page form on an installed system:

```bash
> man configuration.nix
```

To utilize this declarative system configuration, NixOS provides the
`nixos-rebuild` command which reads the `configuration.nix` file to
find out what nixpkgs packages it requests, templates configuration
files with the option values given, and eventually builds the entire
file tree (as usual, symlinked back to the Nix store). NixOS persists
every rebuild of your system as a sequentially numbered "generation,"
which makes it easy to examine or roll back your entire system's
configuration to a prior state. These generations are listed in the
bootloader, so if you break something in your most recent generation,
you can boot into a prior generation to find out what went wrong.

# home-manager

I've traditionally versioned my home folder's dotfiles in a git repo
and deployed it with a [hand-rolled
script](/git-dotfile-versioning-across-systems). Using a lightweight
window manager (formerly XMonad) means that significant portions of my
UI configuration live in my dotfiles, and this has led to increasingly
awkward workarounds to make this configuration portable across the
different hosts I regularly use. One example is controlling the Linux
HiDPI settings which are, to put it mildly, [a mess][14]. I specify a
slew of font tweaks, scaling factors, and DPI settings among half a
dozen dotfiles. This makes it difficult to port my dotfiles from one
machine to another.

The formal Nix ecosystem doesn't ([yet][15]) have a systematic
approach for writing files directly to a home folder. It *can* place
arbitrary files in an `/etc` folder. If you're the sole user of your
machine and the application you want to configure looks at an `/etc`
directory, you could have NixOS write your dotfiles there and forego
keeping them in your home folder at all. My use case unfortunately
doesn't fit neatly into these constraints; I have enough
home-folder-only applications that an `/etc`-based approach isn't
viable.

The most Nix-native experience I've found for managing dotfiles is
[home-manager][18]. It is not only written and managed via the Nix
Expression Language, but it follows the same philosophy as the rest of
NixOS. This includes a similar approach for splitting configuration
into modules and, in fact, it supports importing my custom module
mentioned above. Though home-manager can be run with a separate
`home.nix` file and a `home-manager` CLI utility to trigger "rebuilds"
of your home folder, it additionally exposes a [NixOS module][19] that
can be used in a system-level `configuration.nix` file to rebuild your
home folder following a system-wide rebuild. Being the sole user of my
systems, having NixOS and home-manager work in lockstep is preferable
for me.

home-manager encompasses more than just copying dotfiles to your home
folder. Some broad use cases include:
- Installing packages locally for your user
- Placing dotfiles in your home folder
- Generating dotfiles from a declarative configuration
- Creating per-user systemd services (I use this for `emacs --daemon`,
  and it is quite handy).

It does all this by building a single package, `home-manager-path`,
that includes all the configured local packages and dotfiles. It then
installs this package into your local Nix environment (traditionally
managed by `nix-env`). Similar to how the rest of Nix works, each
dotfile is symlinked into your home folder from the
`home-manager-path` package contained in the Nix store. This works
similarly to how my old, hacky script managed my dotfiles.

The choice between having home-manager generate your dotfiles
whole-cloth, or writing your dotfiles by hand is entirely up to
you. If you're like me and have pre-written dotfiles sitting around,
it's easy to re-use these by

```nix
home.file.".inputrc".source = ./.inputrc;
```

which insures that the `.inputrc` file in the same folder as the
`home.nix` file is deployed to `~/.inputrc` in your home
folder. home-manager supports more complex parameters--my emacs
configuration has too many files to enumerate explicitly, and
home-manager can symlink the entire directory to my home folder,
creating nested directories as necessary:

```nix
home.file.".emacs.d" = {
  source = ./.emacs.d;
  recursive = true;
};
```

home-manager lets me specify file contents directly inside of
`home.nix`, which is useful if I want to reference options defined in
the aforementioned custom module:

```nix
home.file."fonts.el" = {
  target = ".emacs.d/config/fonts.el";
  text = ''
    (provide 'fonts)
    (set-frame-font "${config.settings.fontName}-${toString config.settings.fontSize}")
    (setq default-frame-alist '((font . "${config.settings.fontName}-${toString config.settings.fontSize}")))
  '';
};
```

Since I've never had an extensive `.tmux.conf` file, I can use
home-manger to generate it for me:

```nix
programs.tmux = {
  enable = true;
  terminal = "tmux-256color";
  shortcut = "u";
};
```

which creates a `~/.tmux.conf` file with (among other contents):

```
set  -g default-terminal "tmux-256color"

# rebind main key: C-u
unbind C-b
set -g prefix C-u
bind u send-prefix
bind C-u last-window
```

The ability to have disparate applications with varied configuration
languages wrapped by a single, type safe, functional meta-language is
cool. If the idea of writing Nix code to generate your dotfiles is too
weird, you can always fall back to having it symlink your hand-rolled
dotfiles. If you prefer a hybrid, most home-manager modules have an
`extra` option (or similar) to interleave arbitrary configuration in
the dotfiles it generates.

# Layout

My newly restructured [config repo][3] is now laid out with the
following directories:

- `/nixos/configuration.nix` : general OS configuration that applies
  to all hosts
  - Imports `home.nix` to build my home folder
  - Imports overlays from `pkgs/`
- `hosts/` : host specific configuration:
  - Imports hardware configuration from `hardware/`
  - Imports general NixOS configuration from `nixos/`
  - Imports custom modules from `modules/`
- `hardware/` : low-level configuration (file systems, kernel modules,
  etc.) for use by individual hosts
- `config/home.nix` + dotfiles
  - Imports keyboard layout from `xkb/`
  - Imports custom modules from `modules/`
- `modules/` : my custom configuration module, and any future modules
- `personal/` : private git submodule for non-public dotfiles
- `pkgs/` : overlays for custom packages
- `xkb/` : keyboard layouts

To bootstrap a new host after doing a vanilla install of NixOS, I need
to:
1. Generate the appropriate `hardware/` file (or re-use an existing
   one if the hardware matches).
2. Customize a new `host/` file, including the options defined in
   `modules/settings.nix` to match the needs of the new machine
   (e.g. set a work email or change the default font size for HiDPI
   screens).
3. Following this, I generally symlink the `host/<hostname>.nix` file
   to `/etc/nixos/configuration.nix` so that NixOS rebuilds don't have
   to be passed the file explicitly.
4. Finally, running `nixos-rebuild` will construct the complete OS and
   my home folder with the exact set of packages and dotfiles I've
   defined for all of my machines.

Alternatively, I could inject the configuration into the machine prior
to doing a NixOS install or even build a custom NixOS ISO that
includes my configuration in the image. Since bootstrapping my
configuration is only something I've had to do once per platform, I
haven't been compelled to optimize further yet.

# Conclusion

So far I've been happy with my NixOS setup; I do miss the ease of the
[AUR][21] and the extensively documented [ArchWiki][22]. Perhaps the
most important change I've noticed is how much bolder I can be with
toying on bare hardware; the few times I've messed up my system, I
just boot back into the previous generation.

[1]: https://www.archlinux.org/
[2]: https://nixos.org/
[3]: https://github.com/malloc47/config
[4]: https://github.com/NixOS/nixpkgs/projects/11
[5]: https://nixos.org/nix/
[6]: https://github.com/NixOS/nixpkgs/blob/master/pkgs/top-level/all-packages.nix
[7]: https://nixos.wiki/wiki/NixOS:Properties
[8]: https://github.com/malloc47/config/blob/cd6d1568f50c9b839f5146b45362cd6c4d857882/nixos/configuration.nix
[9]: https://github.com/malloc47/config/blob/cd6d1568f50c9b839f5146b45362cd6c4d857882/hosts/rally.nix
[10]: https://www.qgis.org/
[11]: https://nixos.org/nixos/nix-pills/nixpkgs-overriding-packages.html
[12]: https://r6.ca/blog/20140422T142911Z.html
[13]: https://www.pathname.com/fhs/
[14]: https://wiki.archlinux.org/index.php/HiDPI
[15]: https://github.com/NixOS/nixpkgs/pull/9250
[16]: https://nixos.wiki/wiki/NixOS_Modules
[17]: https://github.com/malloc47/config/blob/95eafec8373d9da302c5778964d4ce6e9c67ed22/modules/settings.nix
[18]: https://github.com/rycee/home-manager
[19]: https://github.com/rycee/home-manager/pull/97
[20]: https://nixos.org/nixos/manual/options.html
[21]: https://aur.archlinux.org/
[22]: https://wiki.archlinux.org/
