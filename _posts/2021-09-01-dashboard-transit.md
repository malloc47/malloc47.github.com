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
verify the transit time. The equivalent of an MTA transit time board
customized with my local stations was exactly what I wanted:

<a href="/img/posts/cockpit/transit.png">
  <img src="/img/posts/cockpit/transit.png" alt="Sample transit card" width="400" />
</a>

This, however, is far simpler than it sounds. Unlike the [weather
card][previous installment], there isn't a single, free, purpose-built
API to serve such dashboards. To deal with transit, we have to solve
two main challenges:

1. Finding a source for the transit data we need to display on the
dashboard, and

2. Stitching together the information from this source to populate the
view.

Taking these points in reverse order, we can address (2) by consuming
[GTFS] feeds, the defacto standard for transit data. It has
complementary [static][GTFS static] and [realtime][GTFS realtime]
flavors, so we need both sources to get the most accurate data for our
dashboard.

To better understand the complexity of GTFS, this is a (very rough)
entity relation diagram:

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

GTFS is quite normalized so there isn't an obvious single entity we
can read that will let us drive everything in our dashboard.  Combing
through the GTFS entities, there is clearly a sizable footprint that
are not relevant to the dashboard we want to build. Removing entities
related to fare calculation, pathing, translation, station layout, and
so forth, a trimmed-down ERD

<a href="/img/posts/cockpit/gtfs-erd-small.svg">
  <img src="/img/posts/cockpit/gtfs-erd-small.svg" alt="GTFS ERD Small" width="400" />
</a>

This subset of GTFS is a bit more manageable for understanding what we
need to consume to satisfy (2) above.



GTFS can be ingested/served without an OTP instance but OTP's
ingestion tools are able to parse both realtime and static GTFS
sources on a schedule and the OTP [Index API][] provides a REST
interface to query this data. Most importantly, some agencies--in
addition to publishing their raw GTFS feeds--host a public OTP
instance for their own routing tools, removing any need to host the
OTP instance for this dashboard. For especially large agencies like
the [MTA][], having a configured OTP instance with feeds for
subways, buses, and ferries already available saved me significant
time and bandwidth.

| [Part 1][first installment] | [Part 2][previous installment] | Part 3 |

[first installment]: {% post_url 2020-12-27-dashboard-start %}
[previous installment]: {% post_url 2021-07-09-dashboard-weather %}
[Index API]: http://dev.opentripplanner.org/apidoc/1.0.0/resource_IndexAPI.html
[MTA]: http://mta.info/
[GTFS]: https://developers.google.com/transit
[GTFS static]: https://developers.google.com/transit/gtfs
[GTFS realtime]: https://developers.google.com/transit/gtfs-realtime
[GTFS reference]: http://www.normes-donnees-tc.org/wp-content/uploads/2014/05/TransmodelForGoogle-09.pdf
