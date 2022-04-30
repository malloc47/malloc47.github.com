{:layout :post
 :title "Import Links into Google Plus as +1s"
 :permalink "/blog/import-links-into-google-plus-as-1s/"
 :date "2012-02-18 00:00:00"}

I've been accumulating helpful and interesting articles for a number
of years now. At first, they existed solely as starred articles in
Google Reader. Eventually, I migrated to
[Delicious](https://delicious.com/), and finally to
[diigo](https://www.diigo.com). While diigo has served me well, I
finally decided to consolidate services and begin using Google's +1
feature, as it is fairly ubiquitous and is associated with an account
I am already logged into (and appears nicely on my Google+ profile).

While Google's Takeout is very useful on the exporting front (a factor
I consciously consider before migrating to any system), I've never run
across a good way to import a list of links as +1s on my Google+
profile. So here's what I came up with last night:

```bash
#!/bin/sh
echo "<html><head>" > page.html
echo "<script type=\"text/javascript\" src=\"https://apis.google.com/js/plusone.js\"></script>" >> page.html
echo "</head><body>" >> page.html
grep -o http[^\"\)\']* $1 | xargs -I{} echo "<g:plusone href=\"{}\"></g:plusone>" >> page.html
echo "</body></html>" >> page.html
```

Since diigo will happily let you export in a variety of formats, I
chose the csv file. My goal was simply to create a webpage with the
links and the +1 button next to each. The process of clicking the
buttons themselves could be automated, but I decided to manually click
on the buttons, since I wanted to vet the links I transferred. The
very simple (read: not iron-clad) regex (`http[^\"\)\']*`) is used to
pull out the links. It looks for an instance of a string "http" and
then continues grabbing characters until it hits a quote. It should
work in a number of contexts, aside from just CSV files. `xargs`
kindly loops over all of these addresses and outputs each in HTML
form. The rest of the script just adds the usual HTML boilerplate, as
well as the +1 script necessary for the buttons to work.

One more wrinkle: the latest crop of browsers sandbox what javascript
is allowed to do to the local filesystem (and rightfully so), so you
will need to upload the generated page to a non-local path (or simply
copy and paste it into a website that will let you edit html
live). Once you do, just click on all the +1 buttons everywhere, and
your links will be +1'd accordingly.

Have a [look](https://plus.google.com/u/0/113712188424853568731/plusones).
