---
layout: post
title: Building a Personal Dashboard in ClojureScript Part 3
date: 2021-08-01 00:00:00
published: true
permalink: building-a-personal-dashboard-in-clojurescript-part-3
---

The raison d'etre for creating the wall-mounted dashboard discussed in
[previous][first installment] [posts][previous installment] was to
help with timing my transit connections when leaving my NYC
apartment. After living on my block for a few months, I had worked out
the perfect time to step out the door to make a subway, bus, or ferry
connection, but quickly grew tired of having to pull out my phone to
verify the transit time. The equivalent of an [MTA][] transit time
board customized with my local stations was exactly what I wanted:

<a href="/img/posts/cockpit/transit.png">
  <img src="/img/posts/cockpit/transit.png" alt="Sample transit card" width="400" />
</a>

This, however, is far simpler than it sounds. Unlike the [weather
card][previous installment], there isn't a single, free, purpose-built
API to serve such dashboards. To deal with transit, we have to solve
two main challenges:

1. Finding a source for the transit data we need to display on the
   dashboard, and

2. Consume transit data from the above source, stitching it together
   to populate the view.

For narrative reasons, we'll take these points in reverse order.

# Consuming Transit Data

The most common way to address (2) is by consuming the [GTFS] (General
Transit Feed Specification), the defacto standard for transit data
that is published by the majority of transit agencies. It has
complementary [static][GTFS static] and [realtime][GTFS realtime]
flavors, and we need both sources to get the most accurate data for
our dashboard.

To better understand the complexity of GTFS, this is a (very rough)
entity relationship diagram:

<a href="/img/posts/cockpit/gtfs-erd.svg">
  <img src="/img/posts/cockpit/gtfs-erd.svg" alt="GTFS ERD" width="600" />
</a>

Note that there are [other similar ERDs][GTFS reference] that are
likely better-researched but didn't fit quite as nealy into this post
so I took a stab at creating this diagram myself. There are a few
caveats in the image above: There are a lot of conditional
relationships in GTFS that are not captured in this image; similarly,
there are also a number of nested entities in the realtime spec that
are glossed over in this diagram for simplicity.

GTFS is quite normalized so there isn't an obvious self-contained
single entity we can read that will let us drive everything in our
dashboard.  Combing through the GTFS entities, there is noticeably a
sizable number that are not relevant to displaying transit times at
chosen stations. Removing entities related to fare calculation,
pathing, translation, station layout, and so forth, the resulting
trimmed-down ERD looks like:

<a href="/img/posts/cockpit/gtfs-erd-small.svg">
  <img src="/img/posts/cockpit/gtfs-erd-small.svg" alt="GTFS ERD Small" width="350" />
</a>

This subset of GTFS entities is a bit more manageable for walking
through what we need to consume for our dashboarding needs. The
particular subset that might be relevant to other agencies might be
different (e.g., some agencies might rely more on `frequency`-based
service or have `calendar`-based service changes).

Exploring this subset in more detail, the `agency` entity isn't
strictly necessary except in cases where the feed represents multiple
agencies. To populate the dashboard, we will identify one or more
`stops`, use these stops to filter the `stop times` list which allows
us to compute the arrival times at a given stop. This would be
sufficient for our dashboard if there's a single transit line going in
a single direction at a given stop. However, if there's multiple
routes or directions of travel at a particular stop then we need to
split the `stop times` into groups by direction and route to
differentiate them. To accomplish this, we additionally look up the
`trip` for each `stop time` which gives us information about the trip
direction, and then walk through `trips` to `routes` which allows us
bucket the `stop times` into groups by route.

So far, we've only touched the entities in the static GTFS, which is
sufficient if the agency consistency runs on time (🤣). To bring the
prescheduled `stop times` into alignment with reality, we read the
`trip updates` GTFS Realtime source and (hand-waving a lot here)
update the `stop times` with these realtime updates at a reasonable
interval.

At a high-level, this is our roadmap for reading the subset of the
GTFS that we need for this dashboard.

# Serving Transit Data

Stepping backwards to address point (1) above, we need to talk about
how we physically consume GTFS in our web application. The static
portion of `GTFS` is a zip file containing `.txt` files (effectively
CSV formatted)--not impossible to handle in a webapp with the right
[decompression][zip.js] and [parsing][papaparse] libraries, but hardly
idiomatic for a web application. The GTFS Realtime format is even more
challenging as it is serialized as a [Protocol Buffer][]. It _might_
be theoretically possible to consume the realtime ProtoBuf stream by
providing the `.proto` file to the browser and using a [ProtoBuf
javascript decoder][pbf]; in practice, the real-time updates from the
[MTA][MTA realtime] are megabytes-to-gigabytes and are updated frequently enough
that I had concerns as to whether a cheap, wall-mounted tablet would
be able to handle parsing the feeds in-browser at a reasonable
frequency.

Thankfully, there are multiple server-side options available which
vary in quality, completeness, and implementation language. Choosing a
minimal GTFS server could absolutely work for this use case, but I
ultimately ended up gravitating towards the [Open Trip Planner][OTP]
project which specializes in route planning (including surface street
connections using [OpenStreetMap][] which is not part of the
GTFS). Not only does OTP consume GTFS (both static and realtime) for
use in its route planning, it caches the serialized results for faster
reloading, has a fetching mechanism to pull the feeds in at a regular
cadence, and--most importantly for our intended application--has an
[Index API][] which provides a REST interface to query GTFS
entities. Even better, it is becoming increasingly common for transit
agencies themselves to host an OTP instance for their route planning
or transit time needs--if such an instance is public-facing, using it
saves a lot of work configuring and hosting our own OTP instance.

Remember from the previous section that we have the following
pseudocode to get a viable payload for our dashboard:

```
1. Given a particular stop ID
2. Fetch the stop times for the given stop
3. Associate a route for each stop time by walking through the
   trip entity
4. Group stop times by route and direction
```

From the [Index API][], the following endpoints look promising to meet
these needs:

| stop       | `/index/stops/{stop-id}`           |
| stop times | `/index/stops/{stop-id}/stoptimes` |
| trip       | `/index/trips/{trip-id}`           |
| route      | `/index/routes/{route-id}`         |

As it turns out, the Index API is able to flatten the `trip` into the
`stop` entity (`/index/stops/{stop-id}`) for us in the scenario where
the stop services a single line + direction. This does not initially
sound like a very useful optimization for more complex transit systems
that routinely have multiple routes traveling multiple directions that
stop at the same station. However, in the case of the MTA's GTFS, the
agency choose to model stations hierarchically, where the main station
is the parent stop and different lines+directions are child stops
within the same overall station. The API also directly adds the
`route-id` to the individual `stop times` (i.e., it traverses the
`stop-time -> trip` entities for us). Thus by choosing these "child"
stops that represent a single line + direction, we can save having to
make the additional `trip` call to get the direction + route ID. Under
this optimization assumption we get an even trimmer effective ERD:

<a href="/img/posts/cockpit/gtfs-erd-smaller.svg"> <img
  src="/img/posts/cockpit/gtfs-erd-smaller.svg" alt="GTFS ERD Smaller"
  width="350" /> </a>

Depending on your particular agency, this optimization may not be
applicable or might be overkill if you're hosting your own OTP and
don't have any concerns about the number of API queries.

# Implementation

Now that we've settled on the physical API to use and know the
relationships among the entities we need for our dashboard, all we
have left is to code and style it. Rather than go line-by-line as in
previous installments, I'll only be going over the highlights of the
[source code][source] in this section.

As with other external APIs we need to hit, we use
[re-frame-http-fx][] for defining the "effect handlers". An example
where we fetch the `stop-times` (assumes that the `stop` has already
been fetched):

```clojure
(re-frame/reg-event-fx
 ::fetch-stop-times
 (fn [_ [_ {:keys [stop-id] :as stop}]]
   {:http-xhrio
    (merge
     otp-request
     {:uri        (str config/otp-uri
                       "/routers/default/index/stops/"
                       stop-id
                       "/stoptimes")
      :on-success [::persist-stop-times [:transit :stop-times stop]]
      :on-failure [::events/http-fail [:transit :stop-times stop]]})}))
```

The notable part of this effect handler is the `::persist-stop-times`
event which is dispatched when the effect handler is successful. The
`::persist-stop-times` event is, itself an effect handler that
persists the `stop-times` API payload into the `re-frame.db/app-db`
while also fanning out (`:dispatch-n`) to trigger `::fetch-route`
events for all the new `route-ids` that it finds:

```clojure
(re-frame/reg-event-fx
 ::persist-stop-times
 (fn [{:keys [db]} [_ key-path stop-times]]
   (let [existing-route-ids (-> db :transit :routes keys set)
         new-route-ids      (->> stop-times :route :id set)
         ;; diff what is in the DB with the newly-seen routes so we
         ;; only fetch them once
         route-ids          (->> (difference new-route-ids
                                             existing-route-ids)
                                 (remove nil?))
         stop-id (-> key-path last :stop-id)]
     {:db         (assoc-in db key-path stop-times)
      ;; fire requests for the routes listed in the payload
      :dispatch-n (map (fn [route-id]
                         [::fetch-route route-id])
                       route-ids)})))
```

Because routes do not change very often, `route-ids` that are already
present in the `app-db` are not fetched again to minimize API queries,
effectively treating the `app-db` as a cache.

How the `stop` and `route` entities are persisted is less interesting
so I'm omitting examples of them here. Just like the [weather
API][previous installment] prior, we now need to [poll the transit
API](https://github.com/malloc47/cockpit/blob/d4badb7e652014693574063806a8ccda27d9fa36/src/cljs/cockpit/polling.cljs#L27-L37)
at regular intervals to make sure our `app-db` always has fresh
information ready for display.

Like other re-frame applications, now that we have our events defined
we need to create subscriptions on the resulting `app-db` changes to
turn these raw OTP Index API payloads into a processed form ready to
be used by our view. Our "Level 1" subscriptions are fairly simple:

```clojure
(re-frame/reg-sub
 ::stop-times-raw
 (fn [db _]
   (-> db :transit :stop-times)))
;;; Repeat for stops and routes...
```

which fetches the raw API payload, which looks something like this:

```clojure
[{:route {:id "MTASBWY:1"}
  :times
  [{:departureDelay 0
    :stopName "South Ferry"
    :scheduledDeparture 89130
    :stopId "MTASBWY:142N"
    :directionId "0"
    :serviceDay 1592539200
    :tripId "MTASBWY:5953"
    :realtimeDeparture 89130
    :stopHeadsign "Uptown & The Bronx"
    :tripHeadsign "Van Cortlandt Park - 242 St"}]
    ...}
 ...]
```

This payload goes through cleanup subscription that grabs specific
keys from the payload, places them into a flattened data structure,
and converts the fixed departure timestamp into a "minutes from now"
format that we'll want in our view:

```clojure
(re-frame/reg-sub
 ::stop-times
 :<- [::stop-times-raw]
 :<- [::clock/clock]
 (fn [[stop-times clock] _]
   (let [now (time-coerce/from-date clock)]
     (->> stop-times
          vals
          (apply concat)
          (mapcat
           (fn [{:keys [times route]}]
             (->> times
                  (map #(assoc % :route route))
                  (map
                   (fn [{time           :realtimeDeparture
                         day            :serviceDay
                         stop-id        :stopId
                         {route-id :id} :route
                         direction-id   :directionId}]
                     {:minutes        (-> time (+ day) (* 1e3)
                                          time-coerce/from-long
                                          (->> (safe-interval now))
                                          time/in-seconds
                                          (/ 60)
                                          js/Math.ceil)
                      :stop-id        stop-id
                      :route-id       route-id
                      :direction-id   direction-id})))))))))
```

This subscription code is detailed and has some assorted helpers
(`safe-interval`, the `cljs-time` namespaces) that are significant but
not worth a tangent right now. As before, I'm also omitting similar
cleanup subscriptions for the `stop` and `route` payloads.

Finally, we join all three of `stops`, `stop-times`, and `routes`
together with a 3rd-level subscription:

```clojure
(re-frame/reg-sub
 ::stop-times-processed
 :<- [::stop-times]
 :<- [::routes]
 :<- [::stops]
 (fn [[stop-times routes stops] _]
   (->> stop-times
        (filter (-> (every-pred nat-int? (partial > 60))
                    (comp :minutes)))
        (map (fn [{:keys [stop-id route-id] :as stop-time}]
               (-> stop-time
                   (assoc :stop (get stops stop-id))
                   (assoc :route (get routes route-id)))))
        ;; Make this an inner join
        (filter (every-pred :stop :route))
        ;; Group by stop only
        (group-by #(select-keys % [:stop]))
        ;; Add route to key after grouping to keep routes together
        (map (fn [[k v]]
               [(assoc k :route (roll-up-route v))
                v]))
        (into {})
        (map-vals #(->> %
                        (filter
                         (fn [{:keys [direction-id]
                               {stop-direction-id :direction-id} :stop}]
                           (or
                            (= direction-id stop-direction-id)
                            (nil? stop-direction-id))))
                        (sort-by :minutes)
                        (take 4)))
        (sort-by (juxt (comp :sort-override :stop first)
                       (comp :sort-order :route first)
                       (comp :stop-id :stop first))))))
```

This is a _lot_ to unpack (which probably means it needs some
refactoring into more subscriptions); out of laziness, I'll summarize
the highlights of this subscription:

- Only `stop-times` that are less than 60 minutes out are kept
- The `stop` and `route` are attached to the `stop-time` with an inner
  join
- Group all `stop-times` by the different `stops` they represent
- Do a "roll up" of all the routes inside of each stop group with the
  [`roll-up-route`
  function](https://github.com/malloc47/cockpit/blob/d4badb7e652014693574063806a8ccda27d9fa36/src/cljs/cockpit/transit.cljs#L233-L256),
  which lets us show a stop, say
  [Lex/63rd](https://en.wikipedia.org/wiki/Lexington_Avenue%E2%80%9363rd_Street_station),
  as a single row labeled "F/Q" rather than having separate rows for
  the F and Q lines. This is typically more useful for express lines
  or other situations where you care only about the latest departure
  but don't care about the particular line.
- Sort the stop times in each group in ascending order
- Take the first four stop-times in each group to show in the view
- Sort the groups themselves

We now have a stable, easy-to-consume set of groups corresponding to
all the stops we care about, each with up to four upcoming departure
times.

| [Part 1][first installment] | [Part 2][previous installment] | Part 3 |

[first installment]: {% post_url 2020-12-27-dashboard-start %}
[previous installment]: {% post_url 2021-07-09-dashboard-weather %}
[Index API]: http://dev.opentripplanner.org/apidoc/1.0.0/resource_IndexAPI.html
[MTA]: http://mta.info/
[MTA realtime]: https://api.mta.info/
[GTFS]: https://developers.google.com/transit
[GTFS static]: https://developers.google.com/transit/gtfs
[GTFS realtime]: https://developers.google.com/transit/gtfs-realtime
[GTFS reference]: http://www.normes-donnees-tc.org/wp-content/uploads/2014/05/TranpsmodelForGoogle-09.pdf
[zip.js]: https://gildas-lormeau.github.io/zip.js/
[papaparse]: https://www.papaparse.com/
[Protocol Buffer]: https://developers.google.com/protocol-buffers
[pbf]: https://github.com/mapbox/pbf
[OTP]: https://www.opentripplanner.org/
[OpenStreetMap]: https://www.openstreetmap.org/
[source]: https://github.com/malloc47/cockpit/blob/d4badb7e652014693574063806a8ccda27d9fa36/src/cljs/cockpit/transit.cljs
[re-frame-http-fx]: https://github.com/day8/re-frame-http-fx
