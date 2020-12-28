---
layout: post
title: Building a Personal Dashboard in ClojureScript Part 4
date: 2020-12-27 00:00:00
published: false
permalink: building-a-personal-dashboard-in-clojurescript-part-4
---

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

[Index API]: http://dev.opentripplanner.org/apidoc/1.0.0/resource_IndexAPI.html
[MTA]: http://mta.info/
