{:layout :post
 :title "Flirting with the Front End"
 :date "2012-03-20"}

Every few years, when I'm not teaching introductory web programming, I
revisit front end development, oftentimes in the form of retooling my
site.  Last time, it was a Wordpress-driven theme with
cobbled-together JavaScript snippets for random bits of functionality:

[<img src="/img/posts/flirting-with-the-front-end/thumb/old-site.jpg" alt="Old Site" width="280" height="183" />](/img/posts/flirting-with-the-front-end/old-site.png)

Serviceable, at least.

Before this, I used a generic Wordpress theme, the specifics of which
I don't recall.  Rolling back all the way to the mid-90s, I had a
[fortunecity][1] site, which was--as typical of sites in the
90s--equal parts bland and garish:

[<img src="/img/posts/flirting-with-the-front-end/thumb/older-site.jpg" alt="Old Site" width="280" height="210" />](/img/posts/flirting-with-the-front-end/older-site.png)

Yes, it had a Christmas theme for the title page. And yes, the header,
navigation bar, and footer (on individual pages) are all java applets.
Not exactly, a usability panacea.

And now, I've transitioned to [Jekyll][2], for a few reasons:

- It's hard to get faster than static pages for serving content.
- [Github][3] can handle more traffic than the shared hosting I was
  using previously.
- A Jekyll deploy on github can't use external plugins.  Which is, by
  most accounts, a downside, but it forces me to find front-end
  solutions for what I want rather than turning to the back end for
  everything.
- I wanted to build everything from scratch.  The limited [Liquid][4]
  DSL used by Jekyll is leaner than full-blown `PHP`, and more
  satisfying for building from the ground-up (all my Wordpress themes
  started from--at minimum--a skeleton theme, just to cover the
  essentials needed by Wordpress).
- Having everything in a git repo is both satisfying for my current
  work flow and avoids the pain of database backups.

So [here][5] it is.  I avoided `jQuery` (convenient as it is) to keep
things lean and loading quickly, and rampantly bludgeoned the site with
`HTML5/CSS3` without much regard for backwards compatibility.  To
further optimize queries, I used Liquid `include`s to aggregate all
the `js` and `css` into single files.  For `JavaScript`:

```javascript
---
---
(function () {
    "use strict";
{{ "{% include cookies.js " }}%}
{{ "{% include mini-clock.js " }}%}
{{ "{% include check-time.js " }}%}
{{ "{% include event-handler.js " }}%}
}());
```

you can wrap everything with `"use strict"` to get some extra
exception goodness.  Doing this may cause [JSLint][6] to complain
about indentation issues, and if you don't add event handlers with
JavaScript (e.g. you use the `onclick` or `onload` events in your
`HTML` tags), you may run into scope issues as well.  All of this
together provided a nearly 20-point speed bump on
[Google page speed][7].

I opted for a dual-themed site, determined by the time of day.  The
clock drawn in the HTML5 Canvas element in the upper-left shows when
the transition will occur, or you can override it with the icon next
to the clock.

All in all, a good transition so far.

[1]: http://www.fortunecity.com
[2]: https://github.com/mojombo/jekyll
[3]: https://github.com/
[4]: https://shopify.github.io/liquid/
[5]: https://github.com/malloc47/malloc47.github.com
[6]: https://www.jslint.com/
[7]: https://developers.google.com/pagespeed/
