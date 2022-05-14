{:layout :post
 :title "Poor Man's LDAP"
 :date #inst "2012-08-11T00:00:00.00Z"}

In addition to being a researcher and backend web developer, I've also
worn the system administrator hat for a number of years.  While the
likes of [`LDAP`][1], [`Active Directory`][2], [`NIS`][3], and their
ilk can work quite well for managing medium-to-large networks, I've
more often been tasked with managing small-scale (< 20 machines)
heterogeneous Linux networks, where deploying `LDAP` with full
`Kerberos` authentication would be overkill.  Typical requirements
I've encountered in small lab settings are simple user account and
home folder sharing, and (relatively) similar package installations.

With this in mind, I did what probably every sysadmin in the same
situation would do: scrape together a simple set of scripts to handle
basic file synchronization for me.  Specifically, I noticed two
prevalent requirements among config files being synced:

* machines and/or distros have a common header or footer that must be
  included (e.g., a list of system users in `/etc/passwd`), and

* specific machines (e.g., servers) shouldn't have some files synced
  with the rest of the machines (e.g., file shares might be different
  on a server).

Thus, [`Poor Man's LDAP`][4] was born.

While nothing more than a collection of scripts--no different than
what many other sysadmins have implemented, in all likelihood--they
will hopefully be of use for those who, like me, are graduate students
or otherwise non-full-time sysadmins that don't have time to do things
the "right" way.

I'm dogfooding `pmldap` on my research lab's network, where we
(currently) have 5 Fedora machines (various versions between 10 and
16) and 5 Debian machines (all on stable).  Since my recent
[patch][5], `pmldap` now supports groups, which are useful for running
`yum` commands only on the Fedora machines and `apt` commands on only
the Debian boxes.  Files being synchronized include: `fstab`, `group`,
`hosts`, `hosts.allow`, `hosts.deny`, `passwd`, `shadow`, and
`sudoers`.

Also in the repo are a few convenience tools that I've found useful:

* `authorize-machine` bootstraps a machine by setting up ssh keys

* `setup` bootstraps config files from a remote machine so they can be
  merged with the desired additions

* `cmd` runs an arbitrary command on all machines (or a particular
  group of machines)

* `useradd` is a feature-incomplete reimplementation of the native
  `useradd` command that works on local `passwd`, `shadow`, and
  `group` files to add new users that can later be synchronized across
  the network

Since I hadn't stumbled across something of this scope to fit the
small-scale-network use case, I'm hopeful that `pmldap` will be of use
to anyone in a similar situation.

You'll find it on gitub [here][4].

[1]: https://en.wikipedia.org/wiki/Ldap
[2]: https://en.wikipedia.org/wiki/Active_directory
[3]: https://en.wikipedia.org/wiki/Network_Information_Service
[4]: https://github.com/malloc47/pmldap
[5]: https://github.com/malloc47/pmldap/commit/ab8918c17f22d2a9dabd6ea9ca97b39c9cdc968a
