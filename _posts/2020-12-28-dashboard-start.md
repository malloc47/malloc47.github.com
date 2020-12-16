---
layout: post
title: Building a Personal Dashboard in ClojureScript
date: 2020-12-13 00:00:00
published: true
permalink: building-a-personal-dashboard-in-clojurescript
---

After the 100th time checking the weather before heading out the door
in the morning, I came to the realization that having a tablet mounted
near my front door with relevant information would be useful. In
anticipation of venturing outdoors more regularly in a post-vaccine
world, I decided to build a personal dashboard as my pandemic project.

There is a lot of prior art in this space, from the [Magic Mirror][]
Raspberry-Pi-deployed dashboard intended to be embedded in a DIY
mirror, to lightweight customizable [iPad apps][] that have all the
common personal dashboard features out-of-the-box. For my part, I
wanted to balance the customizability associated with a DIY project
with the lightweight-ness of a solution that runs entirely on a
tablet. I specifically wanted to customize the particular APIs used,
including some less common sources like local transit times. Much as
I'm not a frontend developer, there's no more lightweight solution
than building everything in a [SPA][] and there no language I prefer
more when dealing with the frontend than [ClojureScript][].

This post will walk through the creation of [cockpit][], the
ClojureScript SPA I now have mounted on my wall. Before getting to
specifics, let's look at the final project:

<a href="/img/posts/cockpit/full.png">
  <img src="/img/posts/cockpit/full.png" alt="Full dashboard view not guaranteed to make sense" width="600" />
</a>

The highlights worth calling out:

- The dashboard is divided into "Cards" using a responsive grid layout
  and styled with [Material-UI][].

- Data for each card is polled at a regular interval with a timer in
  the bottom of each card showing the time elapsed since the data was
  last fetched and a "refresh" button to fetch the data right away.

- The weather card is sourced from the [Open Weather Map API][] with
  icon styling provided by the [Weather Icons][] font. It includes
  typical low/high, forecast, and precipitation information.

- The clock card is self-explanatory--the time is obtained from a
  plain Javascript `Date()` call which returns the system date/time in
  the local timezone.

- Stock data is pulled from the [IEX Cloud API][] and styled with
  [react-sparklines][]. <!-- There are a ton of choices to get stock data -->
  <!-- of varying levels of quality, granularity, and cost. The IEX Cloud -->
  <!-- has minute-level intraday stock prices and a free tier that covers -->
  <!-- enough queries to fetch a handful of stocks every few minutes -->
  <!-- for a month, so it ranks as "good enough" to drive a casual -->
  <!-- dashboard. -->

- The transit card contains rows with arrival times at a transit stop
  (arrows indicating cardinal direction at the stop). The data source
  here is an [Open Trip Planner][] instance loaded with the
  [GTFS][] feeds from the particular transit agency in
  question. <!-- GTFS can be ingested/served without an OTP instance but -->
  <!-- OTP's ingestion tools are able to parse both realtime and static -->
  <!-- GTFS sources on a schedule and the OTP [Index API][12] provides a -->
  <!-- REST interface to query this data. Most importantly, some -->
  <!-- agencies--in addition to publishing their raw GTFS feeds--host a -->
  <!-- public OTP instance for their own routing tools, removing any need -->
  <!-- to host the OTP instance for this dashboard. For especially large -->
  <!-- agencies like the [MTA][13], having a configured OTP instance with -->
  <!-- feeds for subways, buses, and ferries already available saved me -->
  <!-- significant time and bandwidth. -->

Each card has a lot more details worth diving into, but this post will
primarily cover the skeleton of the application; I'll aspirationally
follow-up with future posts to explore the different cards in the
dashboard.

In the ClojureScript SPA space, there are several top-level React
wrappers vying for dominance. For this project, I chose [re-frame][]
since the learning curve for a small-scale project was lighter than
[Fulcro][]. Rather than wire all the various libraries, build tools,
and debugging utilities together manually, the [re-frame-template][]
makes it easy to get started:

    lein new re-frame cockpit +10x +cider +kondo +test

is basically how I seeded the repo. The biggest opinion imposed in the
template aside from re-frame itself is [shadow-cljs][] as the build
tool.

With a skeleton project in hand, let's wire up the views. Dipping our
toes into Material UI requires adding it as a dependency to
`src/cljs/deps.edn`:

```clojure
{:npm-deps {"@material-ui/core"  "4.9.13"
            "@material-ui/icons" "4.9.1"}
 ...}
```

which will get it installed through the typical `npm` channels during
the build.

The `src/cljs/<project>/views.cljs` file is where the "Hello World"
`main-panel` lives. Thanks to the magic of shadow-cljs, we can require
the React components directly into the `cockpit.views` namespace as if
they were native ClojureScript code:

```clojure
(ns cockpit.views
  (:require
   [re-frame.core :as re-frame]
   ["@material-ui/core/Card" :default Card]
   ["@material-ui/core/CardContent" :default CardContent]
   ["@material-ui/core/Container" :default Container]
   ["@material-ui/core/Grid" :default Grid]
   ["@material-ui/core/CssBaseline" :default CssBaseline]
   ["@material-ui/core/Typography" :default Typography]))
```

This lets us modify the `main-panel` with our Material UI [Grid][]
components:

```clojure
(defn main-panel []
  (let [card-opts {:item true :xs 12 :sm 12 :md 6  :lg 4}]
    [:> CssBaseline
     [:> Container {:maxWidth false}
      [:> Grid {:container true :spacing 1}
       [:> Grid card-opts [weather]]
       [:> Grid card-opts [clock]]
       [:> Grid card-opts [transit]]
       [:> Grid card-opts [stocks]]]]]))
```

The `:>` shorthand lets us adapt React components into Reagent
components. `weather`, `clock`, `transit`, and `stocks` are functions
that define the contents of each card. This gives us a blank slate to
fill in our cards with content.

# The Clock Card

<a href="/img/posts/cockpit/clock.png">
  <img src="/img/posts/cockpit/clock.png" alt="Clock card" width="400" />
</a>

The clock card consists of a header with the date, the current time, a
grid with the Central and Pacific times displayed, and the
sunrise/sunset times.

The `clock` view implementing this looks something like:

```clojure
(defn clock []
  [:> Card
   [:> CardContent
    [:> Typography {:align "center" :variant "h4"}
     @(re-frame/subscribe [::clock/day])]
    [:> Typography {:align "center" :variant "h1"}
     @(re-frame/subscribe [::clock/time])]
    [:> Grid {:container true :spacing 0 :direction "row"
              :justify "center" :alignItems "center"}
     [:> Grid {:item true :xs 6}
      [:> Typography {:align "center" :variant "h6"}
       @(re-frame/subscribe [::clock/time-pt])]
      [:> Typography {:align "center" :variant "body2"}
       "San Francisco"]]
     [:> Grid {:item true :xs 6}
      [:> Typography {:align "center" :variant "h6"}
       @(re-frame/subscribe [::clock/time-ct])]
      [:> Typography {:align "center" :variant "body2"}
       "Chicago"]]]
    (let [{:keys [sunrise sunset]}
          @(re-frame/subscribe [::weather/sun])]
      [:> Typography {:align "center"
                      :variant "h6"}
       [:i {:class (str "wi wi-sunrise " (:accent-color classes))}]
       sunrise
       [:i {:class (str "wi wi-sunset " (:accent-color classes))}]
       sunset])]])
```

Nested within the various React components styling the view are
`re-frame/subscribe` functions which bind the view to re-frame
_subscriptions_ which are, effectively, listeners for re-frame
_events_. Subscriptions and events are commonly defined in
`src/cljs/<project>/events.cljs`. The clock events and subscriptions
for the main time display are comparatively simple:

```clojure
(re-frame/reg-event-db
 ::timer
 (fn [db _]
   (assoc db :clock (js/Date.))))

(re-frame/reg-sub
 ::clock
 (fn [db _]
   (:clock db)))

(re-frame/reg-sub
 ::time
 :<- [::clock]
 (fn [clock _]
   (.toLocaleTimeString
    clock
    []
    (clj->js {:hour "numeric" :minute "numeric" :hour12 true}))))
```

Subscriptions and events in re-frame are a [complex][subscriptions]
and nuanced topic, so this breakdown will only begin to scratch the
surface. In short, the `::timer` event--when triggered (details on how
we do this forthcoming)--will update the `:clock` key in the
application's `db` state. The `::clock` subscription is a "Layer 2"
extractor subscription that does nothing but pluck the `:clock` key
back out of the application `db`. The `::time` subscription is a
"Layer 3" materialized view of this extracted value, converting it to
the time string that is ready to be inserted into the rendered
view. Internally, re-frame chains these subscriptions into a graph,
updating all the Layer 2 subscriptions when the db changes, and then
updates only the changed Layer 3 subscriptions and their subscribed
views, leaving everything else untouched.

To tie things together, we need to continually trigger the `::timer`
event for our clock to receive updates and be re-rendered in the
view. For this, we turn to [re-pollsive][], a library that lets us
trigger events based on a fixed interval. After adding the library
dependency to the `project.clj` file, we wire the library to poll in
the `src/cljs/<project>/core.cljs` file:

```clojure
(defn init []
  ...
  (re-frame/dispatch [::poll/set-rules
                      [{:interval                 1
                        :event                    [::events/timer]
                        :dispatch-event-on-start? true}]])
  ...)
```

An `:interval` of 1 will update our clock every second.

<!-- The rest of this post is going to go over the skeleton of the -->
<!-- dashboard. Later posts will deep-dive into the more complicated -->
<!-- aspects of each individual card. -->



[Magic Mirror]: https://magicmirror.builders/
[iPad apps]: https://apps.apple.com/us/app/id659672658
[SPA]: https://en.wikipedia.org/wiki/Single-page_application
[ClojureScript]: https://clojurescript.org/
[cockpit]: https://github.com/malloc47/cockpit
[Material-UI]: https://material-ui.com/
[Open Weather Map API]: https://openweathermap.org/api
[Weather Icons]: https://erikflowers.github.io/weather-icons/
[IEX Cloud API]: https://iexcloud.io/docs/api/
[react-sparklines]: https://github.com/borisyankov/react-sparklines
[Open Trip Planner]: https://www.opentripplanner.org/
[GTFS]: https://en.wikipedia.org/wiki/General_Transit_Feed_Specification
[re-frame]: https://github.com/day8/re-frame
[Fulcro]: https://fulcro.fulcrologic.com/
[re-frame-template]: https://github.com/day8/re-frame-template
[shadow-cljs]: https://github.com/thheller/shadow-cljs
[Grid]: https://material-ui.com/components/grid/
[12]: http://dev.opentripplanner.org/apidoc/1.0.0/resource_IndexAPI.html
[13]: http://mta.info/

[subscriptions]: https://day8.github.io/re-frame/subscriptions/

[re-pollsive]: https://github.com/gadfly361/re-pollsive
