---
layout: post
title: Migrating to NixOS
date: 2019-03-14 00:00:00
published: true
---

After running [Arch Linux][1] for the last decade, I've finally made
the jump to [NixOS][2]. For me, this means updating two VMs
(VirtualBox and VMWare) and a bare-metal install (an aging MacBook
Air).

I've repurposed my old [config repo][3] to store both my user space
configuration as well as the NixOS `configuration.nix` files.

Since I was already making a big transition, I decided to take the
opportunity to retool a few more things in my dev setup:

|                | Old        | New       |
|----------------|------------|-----------|
| OS             | Arch Linux | NixOS     |
| Shell          | Bash       | zsh       |
| Terminal       | urxvt      | alacritty |
| Window Manager | XMonad     | i3        |
| Editor         | Emacs      | Emacs     |

I initially wanted to make the jump from X11 to Wayland, but NixOS
[isn't quite ready][4] just yet.

# Nix

The big ideas behind the Nix ecosystem are covered in [detail
elsewhere][5]; what was appealing to me in particular was Nix's
emphasis on reproducibility, file-driven configuration, and functional
approach to defining its package repository, nixpkgs. You can think of
the Nix package manager as a hybrid of `apt-get` and Python's
`virtualenv`; you can use Nix to build multiple, isolated sets of
packages on, say, a per-project basis, with the guarantee that Nix
only needs to fetch (or build) shared dependencies once.

`nix-shell` serves a few different roles in the Nix ecosystem, but one
of those roles is to make dependencies defined in a "derivation"
(Nix's version of a makefile). These derivations are used to define a
hermetically-sealed environment for building a package as well as
collecting the commands to configure and run a build. We can re-use
just the environment-prep part of a derivation along with nix-shell to
drop us into a terminal that has exactly the packages we want. Here's
an example of a derivation for a TeX project:

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
(`pdflatex`) to actually run the "build" of my document and generate a
resulting PDF. Writing a full derivation file isn't necessary if you
don't need to be dropped into a shell for further work. The following
is equivalent to the derivation above, but returns you to your shell
unchanged:

```nix
nix run nixpkgs.texlive.combined.scheme-full -c pdflatex document.tex
```

I only rarely need TeX, so being able to run the occasional command
without having to keep it installed on my base system indefinitely is
useful.

# nixpkgs

While the Nix Expression Language is somewhat esoteric, the big ideas
aren't far removed from features in mainstream functional
languages. nixpkgs in particular can be conceptualized as a single
large map (called an Attribute Set or attrset in Nix) from keys to
derivations (Nix's `makefile`):

```nix
{
...
  tmux = callPackage ../tools/misc/tmux { };
...
}
```

You can see a meaty example of nixpkg's package list [here][6]. This
would normally be an unwieldy thing to build in memory on every
interaction with the package manager, however Nix lazily loads the
contents of this attrset. Nix even has facilities to make these
attribute sets "recursive" allowing the values to reference sibling
keys, e.g.

```nix
rec { a = 2; b = a+3; }
```

# NixOS

NixOS goes a step further and utilizes attrsets to configure the OS
itself. Not unlike application configuration (for which there
[are](https://github.com/lightbend/config)
[numerous](https://github.com/markbates/configatron)
[libraries](https://github.com/weavejester/environ)), NixOS defines
your OS in a series of one or more attrsets that are merged together;
unlike traditional configuration approaches that use a
last-merged-wins strategy, however, NixOS's [properties][7] provide
per-field control over the priority of merges, and conditionals that
control whether a field is merged or not.

This approach for OS configuration is useful for defining the
properties amongst a set of similar but not identical OSs. For my
NixOS [config][3], I've created a base [`configuration.nix`][8] file
that contains common properties that I want set across all my machines
(toy example here):

```nix
{ config, pkgs, ... }:
{
...
  time.timeZone = "America/Chicago";
  environment.systemPackages = with pkgs; [feh vim wget];
  programs.zsh.enable = true;
  users.users.johndoe.shell = pkgs.zsh;
...
}
```

I then pull this common file into files specific to each host, e.g. a
VM host:

```nix
{ config, pkgs, ... }:
{
...
  imports = [ ./configuration.nix ];
  services.vmwareGuest.enable = true;
  users.users.johndoe.shell = mkOptionDefault pkgs.bash;
...
}
```

Had I left off `mkOptionDefault`, NixOS would complain that
`johndoe.shell` was declared twice. However, by reducing its priority,
the `configuration.nix`'s definition of `johndoe.shell = pkgs.zsh`
will take priority, despite it not being the "last" merged. In
actuality, NixOS builds the configuration as a whole without any
notion of ordering, and will fail loudly if it gets two property
values with equal priority.

# home-manager

I've traditionally versioned everything I needed in my user space in a
git repo and deployed it with a [hand-rolled
script](/git-dotfile-versioning-across-systems). Because I use a
lightweight window manager (formerly XMonad), I found myself having to
come up with increasingly awkward solutions to have this configuration
continue to work across the different hosts I regularly use.

The formal Nix ecosystem doesn't (yet) have an approach for writing
files directly to a home folder. It *can* place arbitrary files in an
`/etc` folder. If you're the only user of your machine and the
application you want to configure looks at an `/etc` directory, you
could have NixOS write your dotfiles there and forego keeping them in
your home folder at all. My use case unfortunately doesn't fit into
these constraints.

The most Nix-native experience I've found for managing user space is
[home-manager](https://github.com/rycee/home-manager).

# Motivation for the switch

While I lack a single compelling reason to make the jump, there are a
few pain points that, together, pushed me to give it a shot:

- **Falling behind on Arch changes.** While I benefited a few times
  from Arch's rolling update process, in practice I've rarely found it
  was something I needed. Not staying on top of Arch updates
  invariably leads to painful upgrades that take time to work
  through. Taking snapshots of my VMs reduced a lot of this upgrade
  risk, but it takes more time than I'm willing to spend to upgrade my
  bare-metal Arch install after neglecting it for extended periods.

- **Package drift among machines.** Having my VMs get slightly
  different versions of packages from my Linux laptop, or forgetting
  to install packages across all machines was a minor but consistent
  annoyance. I kept a list of arch packages that I'd move from machine
  to machine, but nothing forced me to audit that the installed
  packages matched the list.

- **Limited local install options.** I've grown reliant on Docker for
   infrastructural components (e.g. Postgres), but being able to
   install specific dev tools on a per-project basis (I've been
   playing with [QGIS][10] recently) is something I've constantly found
   painful, the few times I've bothered at all.

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
