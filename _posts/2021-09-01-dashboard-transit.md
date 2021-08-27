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
get the timing just right. Having an MTA transit time board customized
with my local stations would be exactly what I needed:

<a href="/img/posts/cockpit/transit.png">
  <img src="/img/posts/cockpit/transit.png" alt="Sample transit card" width="400" />
</a>

This, however, is far simpler than it sounds. Unlike the [weather
card][previous installment], there isn't a single, free, purpose-built
API to serve such dashboards. To deal with transit, we have to solve
two main challenges: Finding a source for the transit data we need to
display on the dashboard, and consuming all the necessary information
from this source to stitch together for the complete view. To this
last point, the Google [GTFS] is the accepted standard for transit
data. It has both [static][GTFS static] and [realtime][GTFS realtime]
flavors, and we need both sources to get the best quality data for our
dashboard.

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
