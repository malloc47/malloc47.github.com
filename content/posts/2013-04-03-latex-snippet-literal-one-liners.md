{:layout :post
 :title "LaTeX Snippet: (Literal) One Liners"
 :date #inst "2013-04-03T00:00:00.00Z"}

There are some truly [impressive][1] LaTeX solutions for doing
PowerPoint-style font-resizing to fit into a fixed width box.  I
recently had need of something more simple: print text on *one line
only*, scaling the size down instead of allowing it to wrap.  The
following LaTeX snippet does exactly this, triggered only if the font
width (before wrapping) exceeds `\textwidth`.

```latex
{
  \def\formattedtext{The no-wrap text to scale}%
  \newdimen{\namewidth}%
  \setlength{\namewidth}{\widthof{\formattedtext}}%
  \ifthenelse{\lengthtest{\namewidth < \textwidth}}%
  {\formattedtext}% do nothing if shorter than text width
  {\resizebox{\textwidth}{!}{\formattedtext}}% scale down
}
```

This requires

```latex
\usepackage{xifthen}
\usepackage{graphicx}
```

to handle the `\ifthenelse`, `\lengthtest`, and `\resizebox`
statements.

It works like you might expect: check the width of the text, and then
use `\resizebox` to scale it down, if needed.  Such logic isn't always
obvious in LaTeX: arbitrary `def`s cannot store length information, so
you have to set the type of the `\namewidth` variable as a dimension
before you can assign/test it as a length.

As with most helpful things in LaTeX, we can wrap it up in a reusable
macro:

```latex
\newcommand{\oneline}[1]{{"{%" }}
  \newdimen{\namewidth}%
  \setlength{\namewidth}{\widthof{#1}}%
  \ifthenelse{\lengthtest{\namewidth < \textwidth}}%
  {#1}%
  {\resizebox{\textwidth}{!}{#1}}%
}
```

which allows

```latex
\oneline{\Huge{The no-wrap text to scale}}

\oneline{\Huge{The quick brown fox jumped over the lazy dog, over and over and over and over again.}}
```

On any reasonable-sized page width, these two lines will not wrap, but
the longer line will be stretched horizontally to fit in the available space.

You can find a fully-working (as of TeXLive 2012) gist [here][2].

[1]: https://tex.stackexchange.com/questions/33417/adjust-font-size-on-the-fly
[2]: https://gist.github.com/malloc47/5298181
