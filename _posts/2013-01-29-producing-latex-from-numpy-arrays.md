---
layout: post
title: Producing LaTeX from NumPy Arrays
date: 2013-01-29 20:00:00
published: false
---

For my comprehensive exam, I had need of quickly converting some NumPy
arrays into nice-looking LaTeX `array` elements.  The TeX Stack
Exchange site has a good [answer][1] for `tabular` environments, but
wasn't quite suited to what I needed for the `array` environment.  The
usual answer here would be [Pweave][2] but, being short on time, I
ended up rolling my own function instead:

{% highlight python %}
def to_latex(a,label='A'):
    sys.stdout.write('\[ '+label+' = \\left| \\begin{array}{' + ('c'*a.shape[1]) + '}\n' )
    for r in a:
        sys.stdout.write(str(r[0]))
        for c in r[1:]:
            sys.stdout.write(' & '+str(c))
        sys.stdout.write('\\\\\n')
    sys.stdout.write('\\end{array} \\right| \]\n')
{% endhighlight %}

Here's an incomplete snippet of it in action, where I convolve an
array `t` with four different filters, producing a latex formula for
each result:

{% highlight python %}
filters = (('A \\oplus H_1',h1)
           , ('A \\oplus H_2',h2)
           , ('A \\oplus H_3',h3)
           , ('A \\oplus H_4',h4))

for label,f in filters:
    t2 = scipy.signal.convolve(t,f,'same')
    to_latex(t2.astype('uint8'),label=label)
{% endhighlight %}

I'll likely get around to expanding this into a full package sometime
in the future, since there's a lot that is hard coded (the `\[ \]`
environment, stringification of the array, the fact that all columns
are centered, etc.).  A gist of the function is available [here][3].

[1]: http://tex.stackexchange.com/questions/54990/convert-numpy-array-into-tabular
[2]: http://mpastell.com/pweave/
[3]: https://gist.github.com/4665827
