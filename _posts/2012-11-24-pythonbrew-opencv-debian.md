---
layout: post
title: pythonbrew+opencv+debian
date: 2012-11-24 15:00:00
published: true
---

There are a number of ways to go about building a modern development
environment for scientific computing and computer vision in python.
If you're used to developing on bleeding-edge, however, the latest
debian stable makes it a chore to get started with the latest and
greatest.  It ships with `python2.6` instead of 2.7, and [opencv][1]
is notoriously out of date in a number of distributions, debian
included.  I typically use Arch, but the server-class machines I have
access to were running debian, so I had to bootstrap my setup into
this environment.

Challenge accepted.

Thankfully, [pythonbrew][2] (or [pythonz][4]) comes to the rescue by
making it easy to handle multiple pythons for a single account
(without having to install them system-wide) as well as providing
wrappers around [virtualenv][3].  However, not everything is rosy.
The python you choose has to be built with shared libraries if you
want to install opencv later:

{% highlight bash %}
pythonbrew install --configure="--enable-shared" 2.7.3
{% endhighlight %}

After this, you can bootstrap a `virtualenv` as usual

{% highlight bash %}
pythonbrew venv init
pythonbrew venv create debian
pythonbrew venv use debian
{% endhighlight %}

and install any requisite stuff you might need (minimum numpy/scipy)

{% highlight bash %}
pip install numpy
pip install scipy
pip install pymorph
pip install matplotlib
pip install distutils
{% endhighlight %}

Unfortunately, there's no such `pip` package for opencv.  Thankfully,
the [debian installation guide][5] isn't too far out of date, and many
of the listed packages to `apt-get` are still relevant.

{% highlight bash %}
wget http://downloads.sourceforge.net/project/opencvlibrary/opencv-unix/2.4.3/OpenCV-2.4.3.tar.bz2
tar xjvf OpenCV-2.4.3.tar.bz2
cd OpenCV-2.4.3
mkdir {build,release}
cd release
{% endhighlight %}

At this point, we need to delve into where `pythonbrew` puts all its
related files to configure opencv correctly.  First, your installed
python will be available in one of two places (here python 2.7.3 is
used as an example):

    ~/.pythonbrew/venvs/Python-2.7.3/{venv-name}/bin/python
	~/.pythonbrew/pythons/Python-2.7.3/bin/python

All `virtualenv`s based on a particular version of python will have a
copy of that python binary for use in their own isolated environment.
In addition, the `virtualenv` has an `include` directory that you
should use, since all your additional packages installed into the
`virtualenv` will place their headers in this directory:

    ~/.pythonbrew/venvs/Python-2.7.3/{venv-name}/include/python2.7

The hitch, however, is that the `virtualenv` does not have a
copy/symlink of the shared library we specifically built when first
compiling python using `pythonbrew`, unlike a typical native python
install.  This means that `cmake`'s approach to locate this library
will fail.  Thus we must point opencv to this

    ~/.pythonbrew/pythons/Python-2.7.3/lib/libpython2.7.so

for it to build corectly.

Speaking of `cmake`, there is a bug in the `cmake` included in debian
that prevents it from building opencv correctly.  I was lazy and
simply grabbed a binary of the latest `cmake`,

{% highlight bash %}
wget http://www.cmake.org/files/v2.8/cmake-2.8.9-Linux-i386.tar.gz
{% endhighlight %}

which worked on my debian build, but it's better to compile it if you
plan to continue using it for more than a one-off build.

Finally, understanding opencv's `cmake` flags is important for getting
everything stitched together:

    PYTHON_EXECUTABLE=~/.pythonbrew/venvs/Python-2.7.3/{venv-name}/bin/python
	PYTHON_INCLUDE_DIR=~/.pythonbrew/venvs/Python-2.7.3/debian/include/python2.7
	PYTHON_LIBRARY=~/.pythonbrew/pythons/Python-2.7.3/lib/libpython2.7.so

Additionally, if you find that numpy isn't autodetected, you can specify

    PYTHON_NUMPY_INCLUDE_DIR=~/.pythonbrew/venvs/Python-2.7.3/debian/lib/python2.7/site-packages/numpy/core/include

You can also specify your `virtualenv` path to install the python libraries

    PYTHON_PACKAGES_PATH=~/.pythonbrew/venvs/Python-2.7.3/{venv-name}/lib/python2.7/site-packages

or just symlink/copy the resulting `cv2.so` and `cv.py` files there later.

Putting it all together, I used this command to generate the makefile
which compiles correctly against `pythonbrew`'s python (where `debian`
is my `virtualenv` name):

{% highlight bash %}
~/cmake-2.8.9-Linux-i386/bin/cmake \
-D CMAKE_INSTALL_PREFIX=../build \
-D BUILD_NEW_PYTHON_SUPPORT=ON \
-D BUILD_PYTHON_SUPPORT=ON \
-D BUILD_EXAMPLES=OFF \
-D PYTHON_EXECUTABLE=~/.pythonbrew/venvs/Python-2.7.3/debian/bin/python \
-D PYTHON_INCLUDE_DIR=~/.pythonbrew/venvs/Python-2.7.3/debian/include/python2.7 \
-D PYTHON_LIBRARY=~/.pythonbrew/pythons/Python-2.7.3/lib/libpython2.7.so \
-D PYTHON_NUMPY_INCLUDE_DIR=~/.pythonbrew/venvs/Python-2.7.3/debian/lib/python2.7/site-packages/numpy/core/include \
-D PYTHON_PACKAGES_PATH=~/.pythonbrew/venvs/Python-2.7.3/debian/lib/python2.7/site-packages \
../
make
make install
{% endhighlight %}

Depending on what you're doing, there may be other tricks with
`LD_LIBRARY_PATH` to make specific things work, but your
`pythonbrew`ed python should be primed to access opencv from here.

[1]: https://opencv.org/
[2]: https://github.com/utahta/pythonbrew
[3]: https://virtualenv.pypa.io/en/latest/
[4]: https://github.com/saghul/pythonz
[5]: http://opencv.willowgarage.com/wiki/InstallGuide%20%3A%20Debian
