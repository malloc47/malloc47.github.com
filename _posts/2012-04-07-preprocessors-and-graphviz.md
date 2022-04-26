---
layout: post
title: Preprocessors and Graphviz
date: 2012-04-07 00:00:00
---

`Graphviz` is a useful toolset for describing and rendering graphs.
One of the features the graphviz language doesn't have, however, is a
`C`-like preprocessor to `#include` files.  Which, granted, isn't a
particularly common use case when building graphs, but one I found
desirable when dealing with a large number of graphs with a shared
subset of nodes, differing by how they are connected.

Initially, I grafted together an unwieldy [script][1] that used an
ugly `grep`+`sed` combination to grab the line number and substitute
the included file contents: essentially a poor man's preprocessor.
Thankfully, to the rescue was a particularly useful [gist][2]
(initially illustrated with JavaScript) I serendipitously found 
on [Reddit][5]. The key call being this:

    cpp -P -undef -Wundef -std=c99 -nostdinc -Wtrigraphs \
	    -fdollars-in-identifiers -C < input.dot > output.dot

In your input `.dot` file, standard `C` include syntax

{% highlight c %}
#include "common.doth"
{% endhighlight %}

will work as expected, despite the fact that it is a completely
different language.

This solution is highly verbose if you don't drop it in a build chain
and forget about it.  Which is straightforward using a standard
[makefile][3]:

{% highlight make %}
SOURCES = $(wildcard *.dot)
OBJECTS = $(SOURCES:.dot=.doto)
IMAGES  = $(SOURCES:.dot=.png)

all: $(IMAGES)

%.png: %.doto
	dot -Tpng $< > $@

%.doto: %.dot
	cpp -P -undef -Wundef -std=c99 -nostdinc -Wtrigraphs \
		-fdollars-in-identifiers -C < $< | \
		gvpr -c 'N[$$.degree==0]{delete(NULL,$$)}' > $@

clean:
	-rm $(IMAGES) $(OBJECTS) *~
{% endhighlight %}

In the above example, I introduced an intermediate `doto` file
(analogous to an object file) and `doth` (header file) to recreate a
`C`-like build process.

Another ingredient above is the piped invocation of `gvpr`, which
removes nodes of degree 1 (so that included nodes that are not
attached to anything in the current file will be ignored).  Remember
that in `makefiles`, the `$` must be escaped by using
`$$`. Unfortunately, the `delete` function in `gvpr` is [broken][4] in
a number of Debian-based distros (at least), but the latest version
works bug-free in Arch.

So, given a `nodes.doth` file with these contents:

        node1 [label = "1"];
        node2 [label = "2"];
        node3 [label = "3"];
	
and a `graph.dot` file as such:

    graph g {
        #include "nodes.doth"
    
        node1 -- node2;
        node2 -- node3;
        node3 -- node1;
    }

the `makefile` will use `cpp` to generate the following intermediate
file,

    graph g {
        node1 [label = "1"];
        node2 [label = "2"];
        node3 [label = "3"];
        node1 -- node2;
        node2 -- node3;
        node3 -- node1;
    }
	
which will then be compiled by graphviz's `dot` command into an image.
While obviously not necessary with this toy example, scaling up to
more nodes shared by multiple graphs is much more pleasant when the
nodes don't have to be duplicated in each graph.

Very little of this is exclusive to graphviz, and is reasonable to
extrapolate to other problems fairly easily.  And, since this
literally uses the `C` preprocessor to do the job, there's many more
tricks to be explored.

Quite helpful to have on hand when the need arises, and a testament to
the quality of `cpp` that it can be used for arbitrary metaprogramming
in other languages.

[1]: https://gist.github.com/2324425
[2]: https://gist.github.com/2037497
[3]: https://github.com/open-it-lab/ol-curriculum/blob/master/makefile
[4]: https://bugs.debian.org/cgi-bin/bugreport.cgi?bug=652952
[5]: https://www.reddit.com/r/programming/comments/qxn73/mixing_javascript_and_the_cpreprocessor/
