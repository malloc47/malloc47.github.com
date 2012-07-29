---
layout: post
title: Git Dotfile Versioning Across Systems
date: 2012-07-29 00:00:00
published: true
---

For users of unix-like operating systems, treating your dotfiles like
real code and keeping them in a repository is a supremely good idea.
While there are a myriad of ways to go about this, the typical (albeit
destructive) way to do this is by symlinking files in the repository
to the home folder:

{% highlight bash %}
#!/bin/bash
DEST=$HOME
FILES=$(git ls-files | grep -v .gitignore | grep -v ^$(basename $0)$)
for f in $FILES ; do
    [ -n "$(dirname $f)" \ 
      -a "$(dirname $f)" != "." \
      -a ! -d "$DEST/$(dirname $f)" ] \ 
    && mkdir -p $DEST/$(dirname $f)
    ln -sf $(pwd)/$f $DEST/$f
done
{% endhighlight %}

I specifically chose to have `FILES` populated using `git ls-files` to
prevent any unversioned files from sneaking into the home folder,
additionally filtering out both the `.gitignore` file, and the current
script name (so it can be safely checked in as well).  After this, we
loop over the files, creating appropriate directories if they do not
exist, effectively symlinking the *entire repo* to the home folder,
clobbering any files that are already there (without asking!).

While most dotfiles won't care what system they are on, certain
scripts or settings may be machine-dependent.  To accommodate this, I
include a ``~/.sys/`hostname`/`` folder for every machine with
system-specific files.  Then, when symlinking, we favor files listed
in the ``~/.sys/`hostname`/`` folder rather than the top-level files:

{% highlight bash %}
if [ -e ".sys/$(hostname)/$f" ] ; then
    ln -sf $(pwd)/.sys/$(hostname)/$f $DEST/$f
else
    ln -sf $(pwd)/$f $DEST/$f
fi
{% endhighlight %}

Thus, for example, given `machine1` and `machine2` and a repo in the
`~/dotfiles` directory with these files:
    
	~/dotfiles/.gitconfig
	~/dotfiles/.sys/machine2/.gitconfig

`machine1` will get a symlink from

    ~/dotfiles/.gitconfig 
	
to `~/.gitconfig`, while `machine2` will instead get a symlink from

    ~/dotfiles/.sys/machine2/.gitconfig
	
to `~/.gitconfig`.  This variant of the script doesn't explicitly
ignore the `.sys` folder itself so it will be added to the home folder
as well.  Which, as an aside, can be useful by including something
like this

{% highlight bash %}
[ -d ~/.sys/`hostname`/bin ] && export PATH=~/.sys/`hostname`/bin:$PATH
{% endhighlight %}

in the `.bashrc` file such that specific scripts will be on the `PATH`
for individual machines.

So the final script, with a bit of input checking, looks like this:

{% highlight bash %}
#!/bin/bash
set -e
EXPECTED_ARGS=1
if [ $# -lt $EXPECTED_ARGS ]
then
    echo "Usage: `basename $0` directory"
    echo "WILL clobber existing files without permission: Use at your own risk"
    exit 65 
fi

DEST=$1
FILES=$(git ls-files | grep -v .gitignore | grep -v ^$(basename $0)$)

for f in $FILES ; do
    echo $f
    if [ -n "$(dirname $f)" -a "$(dirname $f)" != "." -a ! -d "$DEST/$(dirname $f)" ] ; then
        mkdir -p $DEST/$(dirname $f)
    fi
	
    if [ -e ".sys/$(hostname)/$f" ] ; then
        ln -sf $(pwd)/.sys/$(hostname)/$f $DEST/$f
    else
        ln -sf $(pwd)/$f $DEST/$f
    fi
done
{% endhighlight %}

By making `DEST` a command-line parameter, a dry-run can be done by
simply giving it an empty folder.  There's no issue doing this inside
the repo's working tree, as only checked-in files will be transferred
to the target directory:

    > mkdir tmp
    > ./deploy tmp/

Doing this, the contents of the `tmp/` directory can be verified with
`ls -al` to see exactly what the script will do to your home folder.
Once satisfied, it can be run again with

    > ./deploy ~
	
to symlink all the files to the home folder proper.

Feel free to grab an up-to-date version of this script from my own
dotfile repo [here][1].

[1]: https://github.com/malloc47/config/blob/master/deploy
