{:layout :post
 :title "QR Codes En Masse"
 :date "2012-03-24T23:00:00"}

For the upcoming [POSSCON][1] here in Columbia, we had need of QR
codes for the brochure.  Lots of them.  And while there are
[some](https://qrcode.kaywa.com/)
[great](https://goqr.me/)
[online](https://www.patrick-wied.at/static/qrgen/)
resources,
I wanted to create QR codes in batch.

Of course, the online services could be batch processed with a dose
of `curl` magic, but there is a more UNIX way: `libqrencode`.

Creating a discrete QR code image is straightforward with the
`qrencode` command:

    qrencode -o output.png -s 50 "https://www.malloc47.com"

The `-s` parameter controls the size of the QR "dots" and therefore
the output resolution.

This is great if you don't need a vectorized format, but for
print-quality work where you may not know the eventual `DPI`, having
vectorized output (e.g. `eps`, and `svg`) is preferable.

Again, the vast repositories of libre software come to the rescue
here: `potrace` is designed for exactly this.  Annoyingly, it only
handles bitmap (or the easy-to-generate [pnm][2]) format, so a little
`imagemagick` will take care of this:

    convert output.png output.bmp

Now we can convert to a vector format easily:

    potrace -e -o output.eps output.bmp # -e is for EPS
    potrace -s -o output.svg output.bmp # -s is for SVG

We can wrap it all up into a nice (`bash`) script:

```bash
#!/bin/bash
set -e
qrencode -o $1.png -s 50 "$2"
convert $1.png $1.bmp
potrace -e -o $1.eps $1.bmp
rm $1.png $1.bmp
```

which takes a file name prefix and a string to be encoded. To generate
a large set of QR codes with this script, simply create a file with
file prefix-`URL`(or whatever data is to be encoded) pairs, each on a
separate line,

    google https://www.google.com
	amazon https://www.amazon.com
	reddit https://www.reddit.com
	....

and then loop over this file, line-by-line:

```bash
while read line ; do ./create-qr-code.sh $line ; done < list.text
```

which conveniently gives us `google.eps`, `amazon.eps`, and
`reddit.eps` files for their respective `URL`s.

If there is uncertainty that your `URL`s are good (i.e. don't kick back
`404`s), then you can augment the above script with this nice `curl`
snippet (courtesy of [this][3] post on SO):

```bash
#!/bin/bash
set -e
curl -s --head $2 | head -n 1 | grep "HTTP/1.[01] [23].." > /dev/null
if [ $? -eq 0 ] ; then
    qrencode -o $1.png -s 50 "$2"
    convert $1.png $1.bmp
    potrace -e -o $1.eps $1.bmp
    rm $1.png $1.bmp
else
    echo "URL error: $2" 1>&2
fi
```

This will let you know which `URL`s don't come back with clean headers
so you can give them further attention.  It won't capture everything
that might go wrong, but it does give you a programmatic way to verify
that all is well.

Incidentally, all the tools used here can be installed on Arch with

    pacman -S qrencode potrace imagemagick curl

Not exactly the prettiest shell glue, but it certainly beats slowly
copy &amp; pasting in and out of a browser.

[1]: https://www.posscon.org/
[2]: https://en.wikipedia.org/wiki/Netpbm_format
[3]: https://stackoverflow.com/questions/2924422/how-do-i-determine-if-a-web-page-exists-with-shell-scripting
