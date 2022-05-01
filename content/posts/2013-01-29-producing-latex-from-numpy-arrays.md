{:layout :post
 :title "Producing LaTeX from NumPy Arrays"
 :date "2013-01-29T20:00:00"}

For my comprehensive exam, I needed to quickly convert some NumPy
arrays into nice-looking LaTeX `array` elements.  The TeX Stack
Exchange site has a good [answer][1] for `tabular` environments, but
wasn't quite suited to the `array` environment.  The usual answer here
would be [Pweave][2] but, being short on time, I ended up rolling my
own function instead:

```python
def to_latex(a,label='A'):
    sys.stdout.write('\[ '
                     + label
                     + ' = \\left| \\begin{array}{'
                     + ('c'*a.shape[1])
                     + '}\n' )
    for r in a:
        sys.stdout.write(str(r[0]))
        for c in r[1:]:
            sys.stdout.write(' & '+str(c))
        sys.stdout.write('\\\\\n')
    sys.stdout.write('\\end{array} \\right| \]\n')
```

Here's an incomplete snippet of it in action, where I convolve an
array `t` with four different filters, producing a latex formula for
each result:

```python
filters = (('A \\oplus H_1',h1)
           , ('A \\oplus H_2',h2)
           , ('A \\oplus H_3',h3)
           , ('A \\oplus H_4',h4))

for label,f in filters:
    t2 = scipy.signal.convolve(t,f,'same')
    to_latex(t2.astype('uint8'),label=label)
```

I'll likely get around to expanding this into a full package sometime
in the future, since there's a lot that is hard coded (the `\[ \]`
environment, stringification of the array, the fact that all columns
are centered, etc.).  A gist of the function is available [here][3].

[1]: https://tex.stackexchange.com/questions/54990/convert-numpy-array-into-tabular
[2]: https://mpastell.com/pweave/
[3]: https://gist.github.com/4665827
