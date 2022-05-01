{:layout :post
 :title "Google Completion in Emacs"
 :date "2012-05-19"}

While many an `emacs` include `dabbrev-expand` for within-buffer
completion, I've always wanted (purely for reasons of amusement) to
take it further: completion via Google's search suggestions.  I was
going to do this as a weekend project, but an ugly version was
surprisingly simple.

Conveniently, `curl` is all we need to fetch the completions for a
query string as `JSON`:

```bash
> echo -en $(curl -H "Accept: application/json" \
  "https://suggestqueries.google.com/complete/search?client=firefox&q=query")

["query",["query","query xiv","query letter","query_posts","query shark","query access","query tracker","query string","query letter sample","queryperformancecounter"]]
```

using a (very platform dependent) `echo` trick to convert the escaped
unicode sequences to their proper characters.

With this, a quick hack in `elisp` is all that's necessary to parse
the results into `emacs` and insert it into the current buffer:

```cl
(defun google-request (query)
 (shell-command-to-string
  (format
   "echo -en $(curl -H \"Accept: application/json\" \"https://suggestqueries.google.com/complete/search?client=firefox&q=%s\" 2>/dev/null)"
   query)))

(defun google-preprocess (query)
 (let ((l (split-string
	   (apply 'string
		  (removel
		   '(?\" ?\[ ?\])
		   (string-to-list query)))
	   ",")))
  (if (> (length (car (cdr l))) 0)
    (remove (car l) (cdr l))
   nil)))

(defun google-complete ()
 (interactive)
 (end-of-thing 'word)
 (let ((s (thing-at-point 'word)))
  (when s
   (let ((q (google-preprocess (google-request s))))
    (when q
     (insert (substring
	       (car q)
	       (length s))))))))

(defun removel (el l)
 (cond (el (removel (cdr el) (remove (car el) l)))
       (t l)))
```

Since it went more swiftly than anticipated, I generalized the code to
parsing any delimited shell output and wrapped it in a minor mode with
some key bindings and `customize` variables.  Right now, I'm
uncreatively calling it [`shell-parse.el`][3].

After activating `shell-parse-mode`, it has support for scrolling
through the list of completions forwards (`shell-parse-complete`) and
backwards (`shell-parse-complete-backwards`) with the `C-Tab` and
`C-Shift-Tab` keys, respectively.  Using `M-x customize-mode <Enter>
shell-parse-mode`, you can swap out the `curl` command with any shell
snippet that will kick back completions, and change the delimiter as
well.

You can grab `shell-parse.el` on [github][1].  Simply `load-file` the
`shell-parse.el` script in `.emacs` and it should be ready to go.

It has a few todos scattered through it yet, and is not very idiomatic
`emacs` or portable, but that's what github's [issue tracker][2] is
for, after all.

[1]: https://github.com/malloc47/shell-parse.el
[2]: https://github.com/malloc47/shell-parse.el/issues
[3]: https://github.com/malloc47/shell-parse.el/blob/master/shell-parse.el
