---
layout: post
title: Anatomy of a Chrome Extension
date: 2012-09-15 00:00:00
published: false
---

I launched [nonpartisan.me][1] a few weeks back, which exists
primarily in the form of a Google Chrome extension (there's a Firefox
add-on too).  Since I released it with all of the [source][2], this
makes it a great time to disect the (very simple) code.  As you will
notice from the site and the small bit of [press][5] it's picked up,
`nonpartisan.me` has a very simple premise: filter out political
keywords from the various newsfeeds (specifically Facebook, Twitter,
and Google+).

This was my first attempt at a Chrome extension, and it's surprisingly
straightforward.  All such extensions require a `manifest.json`, which
looks like this for `nonpartisan.me`:

{% highlight javascript %}
{
    "name"             : "nonpartisan.me",
    "version"          : "0.2.1",
    "manifest_version" : 2,
    "description"      : "Removes partisanship from your news feeds",
    "icons"            : { "16": "icon16.png",
                           "48": "icon48.png",
                          "128": "icon128.png" },
    "homepage_url"     : "http://nonpartisan.me",
    "page_action"      : {"default_icon" : "icon48.png",
                          "default_title": "nonpartisan'ed" },
    "permissions"      : ["tabs",
                          "http://www.facebook.com/",
                          "http://www.twitter.com/",
                          "http://plus.google.com/"],
    "options_page"     : "options.html",
    "content_scripts"  : [
    {
        "matches": ["*://*.facebook.com/*"],
        "js"     : ["jquery.js","common.js","fb.js","nonpartisan.js"],
        "run_at" : "document_end"
    },
    {
        "matches": ["*://twitter.com/*"],
        "js"     : ["jquery.js","common.js","tw.js","nonpartisan.js"],
        "run_at" : "document_end"
    },
    {
        "matches": ["*://plus.google.com/*"],
        "js"     : ["jquery.js","common.js","gp.js","nonpartisan.js"],
        "run_at" : "document_end"
    }],
    "background": {"scripts"   : ["common.js","background.js"],
                   "persistent": false }
}
{% endhighlight %}

The real meat here is `content_scripts`, which lists the javascript we
wish to trigger after a page is loaded, `greasemonkey`-style.  A
particularly nice feature of content scripts are that they work in an
isolated environment separate from any javascript that the page itself
may include.  Thus we can add `jquery` in the list of javascript that
is run in the content script without fear of clashing with a page's
execution context.  

You can think of every element in the `"js"` array as a separate
`<script>` tag in an `HTML` page, so the files are loaded in the given
order, all into a single namespace.  Rather clumsily, I chose to
simply put a callback module (which is called `plugin` here) in the
individual `fb.js`, `tw.js`, and `gp.js` files which is then used by
the core component, `nonpartisan.js` as a simple means of avoiding any
hard-coded per-site values in the actual filtering code.

With this, and the pseudo-regex `"matches"` field that specifies which
pages trigger the content script, we can run arbitrary code on
websites we specify For `nonpartisan.me`, the filtering code looks
like this:

{% highlight javascript %}
"use strict";
var nonpartisan = function(plugin) {

    function nonpartisan (watch,parent,keywords) {
        function kill (parent,removeList){
            $(parent).each(function () {
                var el = $(this);
                if(el.css('display') !== 'none') {
                    el.find('*').each(function () {
                        var toCheck = $(this).text().toLowerCase();
                        if(toCheck.length > 0 &&
                           (removeList.some(function (value) {
                               return (toCheck.search("\\b"+value.toLowerCase()+"\\b") >=0);
                           }))
                          ) {
                            el.css({'display':'none'});
                            return false;
                        }
                    });
                }
            });
        }

        if($(parent) && $(watch)) {
            var numChildren = $(parent).children().length;
            setInterval(function () {
                var newNumChildren = $(parent).children().length;
                if(numChildren !== newNumChildren) {
                    kill(parent,keywords);
                    numChildren = newNumChildren;
                }
            },
                        500);
            kill(parent,keywords);
        }
    }

    // get parameters from plugin and trigger nonpartisan() here...

}(plugin);
{% endhighlight %}

The first chunk--the `kill` function--works as advertised: given a
parent element and a set of keywords, the function iterates over every
child element and determines if any of the nested elements within
(i.e. `el.find('*')`) contains any one of the keywords.  Instead of
killing `DOM` nodes, which may break the page's own javascript (I
discovered this the hard way), it's easier to instead call
`el.css({'display':'none});` to simply hide unwanted elements.  For
efficiency, the `forEach` terminates as soon any any nested child
returns a match, potentialy saving a small amount of needless
searching. 

The second chunk starts a timer--if indeed the parent is even found on
the current page--that checks if the number of children of the parent
element has changed and, if so, re-triggers the filtering process to
determine if there are any new children to be hidden.  This helps
handle `AJAX`-driven sites, like the "infinite scrolling" facebook
newsfeed, which may mutate the `DOM` at any time.

These functions are wrapped up into another easy-to-call function
inside of the high-level `nonpartisan` module.  

The details of the
plumbing used to kick-off the process are omitted here, as they
involve transferring `localStorage` settings from another process via
Chrome's internal plumbing, which is educational, but not immediately
relevant just yet.  

And that really is all there is to a typical `greasemonkey`-like
Chrome extension, but that's certainly not the end of what a complete
and helpful extension can provide.  The trickier bit is persisting
configuration options.  The downside of sandboxing content scripts is
that they exist in a transient execution context, meaning there's no
`localStorage` to persist program options.

Chrome provides a nice solution for this, however, by providing a
`background` script which *does* have its own `localStorage`, which it
can transmit to a content script via the `chrome.extension.onMessage`
listener.  We can then fill in the omitted component of the above
script:

{% highlight javascript %}
chrome.extension.sendMessage({method: "config"}, function (response) {
    if(!response.sites[plugin.site]) return;
    var l = response.filter;
    if(l && l.length>0) {
        plugin.cb(l,nonpartisan);
    }
	// get default values from common.js
    else {
        l = [];
        for(var index in choices) {
            l = l.concat(choices[index]);
        }
        plugin.cb(l,nonpartisan);
    }
});
{% endhighlight %}

This sends a message, requesting `"config"` from the `background.js`
script, which returns, among other things, the list of keywords we
wish to filter, which were saved in the `localStorage` in
`background.js`'s execution context.  Recall that the `plugin` is the
module that specifies the particular settings for the page being
filtered.  Thus we specify the list of words to filter, and the
`nonpartisan()` callback function to the `plugin` module, and it then
executes `nonpartisan()` on the appropriate elements on the page.

Of course, there's only so much utility to be gained from
`localStorage` without supplying the user with the ability to
configure the various options that may be saved in `localStorage`.
This is done by a typical `html` page, specified by `"options_page"`.
Since there's not much magic there--it's just a plain html page with
enough javascript to persist the settings--I will omit the gory
details, which you can poke around ourself in [the][3]
[repository][4], if you're so inclined.

So that's an extension.  Writing the above was literally a matter of
minutes.  As is always the case (especially when I'm working outside
of my area of expertise, say with making the amateurish logo), the
real work is doing the little bits of spit-and-polish to handle the
various configuration options, throwing together the webpage, creating
the icons and promotional images for the [Chrome Web Store][6], etc.
But it's still good to know that the Chrome team has made the
extension-building process as simple and well [documented][7] as they
have.

[1]: http://nonpartisan.me
[2]: https://github.com/malloc47/nonpartisan.me
[3]: https://github.com/malloc47/nonpartisan.me/blob/master/chrome/options.js
[4]: https://github.com/malloc47/nonpartisan.me/blob/master/chrome/background.js
[5]: http://www.charlestoncitypaper.com/charleston/sick-of-politics-on-facebook-try-this-browser-tool/Content?oid=4153447
[6]: https://chrome.google.com/webstore/detail/ninebcppidndhampaggnjbijpacoadgg?
[7]: http://developer.chrome.com/extensions/docs.html
