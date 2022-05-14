{:layout :post
 :title "Man Page Readability"
 :uri "/man-page-readability/"
 :redirects ["/blog/man-page-readability/"]
 :date #inst "2012-02-11T00:00:00.00Z"}

Man pages are one of the staples of a healthy \*nix diet, but having
grown up with them, it didn't occur to me until recently to wonder how
readable they really are. The de facto standard for readability
has--for better or worse--converged to the [Flesch–Kincaid][1] test,
which (a particular variant) ranks readability as a "grade level,"
roughly corresponding to an American school grade. Getting a
readability score for a manpage is as simple as piping our man page to
the GNU *style* program (not installed on many distros, I discovered,
and typically available in the "diction" package).

    > man /usr/share/man/man1/git.1.gz | style
    readability grades:
            Kincaid: 7.0
            ARI: 4.4
            Coleman-Liau: 4.0
            ...

Notice that man can read in the (tarzipped) man source file (typically
located in `/usr/share/man/man?` folders) rather than having to type
the executable name.

Since we're only concerned with the Kincaid score, we can apply a
smattering of grep+awk to extract it.

    man /usr/share/man/man1/git.1.gz | style | grep Kincaid | awk '{print $2'}

And finally, looping over all the installed man pages (+sed to trim
out short sentences, headers, etc.) gives us one big file, from which
we can get readability statistics.

    for i in `ls -d -1 /usr/share/man/man?/*` ; do echo -n "$i " ; man $i | tr '\n' ' ' | sed 's/\./\.\n/g' | sed -e 's/^[ \t]*//' | sed '/.\{3\}/!d' | grep Kincaid | awk '{print $2}' ; done > ~/flesch-kincaid

More awk magic will give us an average and standard deviation.

    > awk '{avg+=$2} END {print avg/NR}' ~/flesch-kincaid
    9.08134
    > awk '{sum+=$2; sumsq+=$2*$2} END {print sqrt(sumsq/NR - (sum/NR)**2)}' ~/flesch-kincaid
    10.3857

Perhaps surprisingly, the average readability of the man pages on my
machine is below the college level. More unexpectedly, the standard
deviation is very high, indicating that there's a wide range of
readability from one man page to another. The page with the most
absurdly large readability score

    grep `awk '$2>m{m=$2}END{print m}' ~/flesch-kincaid` ~/flesch-kincaid

consist almost entirely of code and API documentation.

Of course, this does not take into account a myriad of confounding
factors: some non-English language pages crept into the list, which
kick out bogus scores by the Flesch–Kincaid metric, man pages have
non-standard formatting (e.g. command switches) which aren't
considered in the metric, etc. But knowing the general (or at least
average) education level required to comprehend man pages is worth
considering as more mainstream distributions bring with them an influx
of younger and less experienced users.

[1]: https://en.wikipedia.org/wiki/Flesch%E2%80%93Kincaid_readability_test
