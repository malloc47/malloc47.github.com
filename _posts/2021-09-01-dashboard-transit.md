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
flavors, so we need both sources to get the most accurate data for our
dashboard.

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
through what we need to consume for our dashboarding needs. `agency`
isn't strictly necessary except in cases where the feed represents
multiple agencies. To populate the dashboard, we will identify one or
more `stops`, use these stops to filter the `stop times` list which
allows us to compute the arrival times at a given stop. This would be
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

From the [Index API], the following endpoints look promising to meet
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
make the additional `trip` call to get the direction + route
ID. Depending on your particular agency, this optimization may not be
applicable or might be overkill if you're hosting your own OTP and
don't have any concerns about the number of API queries.

# Implementation

Now that we've settled on the physical API to use and know the
relationships among the entities we need for our dashboard, all we
have left is to code and style it.

| [Part 1][first installment] | [Part 2][previous installment] | Part 3 |

[first installment]: {% post_url 2020-12-27-dashboard-start %}
[previous installment]: {% post_url 2021-07-09-dashboard-weather %}
[Index API]: http://dev.opentripplanner.org/apidoc/1.0.0/resource_IndexAPI.html
[MTA]: http://mta.info/
[MTA realtime]: https://api.mta.info/
[GTFS]: https://developers.google.com/transit
[GTFS static]: https://developers.google.com/transit/gtfs
[GTFS realtime]: https://developers.google.com/transit/gtfs-realtime
[GTFS reference]: http://www.normes-donnees-tc.org/wp-content/uploads/2014/05/TransmodelForGoogle-09.pdf
[zip.js]: https://gildas-lormeau.github.io/zip.js/
[papaparse]: https://www.papaparse.com/
[Protocol Buffer]: https://developers.google.com/protocol-buffers
[pbf]: https://github.com/mapbox/pbf
[OTP]: https://www.opentripplanner.org/
[OpenStreetMap]: https://www.openstreetmap.org/
