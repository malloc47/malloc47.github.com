---
layout: post
title: Architecting a Data Organization
#date: 2021-10-20 00:00:00
published: true
permalink: architecting-a-data-organization
---

There are some great resources on [what Staff Engineers
do][what-staff-engs-do] and as someone who fits the [Architect
archetype][staff-archetypes] (at least by job title), I have given a
lot of thought to the broad categories of work--particularly the more
abstract--that happen under this umbrella. One lesser-discussed
category of work that I find valuable in this conversation is what I
term **architecture intersection**, the process of juxtaposing and
aligning different architectural domains so they are jointly better
realized. Similar to how engineers must juggle performance, security,
extensibility, scalability, and [all the other -ilities][system
quality attributes] when designing systems, software architects have
to trade-off among disparate domains, e.g.,

- Intersecting an application [reference architecture][] with a
  particular [application architecture][] instance, ideally
  bidirectionally rather than top-down so the reference architecture
  can be improved by the implementation and vice versa.

- Intersecting [data architecture][] of a large system with the
  [information security architecture][] of an organization with an eye
  towards optimizing both the security model and data model of the
  system.

- Intersecting the [organizational architecture][] of a company with
  the [systems architecture][], insuring that the way teams are
  organized supports the system decomposition.

This may sound like a lot of buzzword bingo--and, indeed, outside of
large organizations, a lot of these layers are far less
formalized--but there's an [architect elevator][] to ride whether or
not these terms are formally in use.

I supply this framing in the service of discussing a series of these
intersections, highlighting how anchoring decisions in one
architectural domain impacts another, starting with the last
bullet-point above: How [organizational architecture][] and [systems
architecture][] work together to define the modern **data
organization**. Consider this a guide to all the architecture
decisions I wish I had known to anticpate when building out a data
organization from scratch.

# The Modern Data Organization

As much as frontend application development practices have become the
butt of many jokes for inventing new client-side frameworks every few
years with little stability, it is arguable that modern data practices
are even more nacient and evolving. That's not to say that technical
people with "data" in their title is a new thing, but the modern data
organization is often a chimera of traditional practices like [BI][]
rolled together with newly-labeled practices like [MLOps][] resulting
in a [proliferation of technologies][data landscape 2021] that might
even prompt frontend folks to ask us to chill out.

With 2020 being the [year of automation][2020 automation] for the data
practices, it's not hard to argue that the maturity of the data
landscape is easily [10 years behind][] areas like web application
development. While other technical areas were leaning heavily into
deployment automation and infrastructure-as-code, a lot of the "big
data" systems were barely beyond supporting ad-hoc non-production
workflows. Because of this reality, there isn't a

# Organizational Structure

# Components

## Platform

# Operations

# Processes

## SDLC

[what-staff-engs-do]: https://staffeng.com/guides/what-do-staff-engineers-actually-do
[staff-archetypes]: https://staffeng.com/guides/staff-archetypes#Architect
[application architecture]: https://en.wikipedia.org/wiki/Applications_architecture
[enterprise architecture]: https://en.wikipedia.org/wiki/Enterprise_architecture
[architect elevator]: https://martinfowler.com/articles/architect-elevator.html
[reference architecture]: https://en.wikipedia.org/wiki/Reference_architecture
[data architecture]: https://en.wikipedia.org/wiki/Data_architecture
[information security architecture]: https://en.wikipedia.org/wiki/Enterprise_information_security_architecture
[organizational architecture]: https://en.wikipedia.org/wiki/Organizational_architecture
[systems architecture]: https://en.wikipedia.org/wiki/Systems_architecture
[system quality attributes]: https://en.wikipedia.org/wiki/List_of_system_quality_attributes
[BI]: https://en.wikipedia.org/wiki/Business_intelligence
[MLOps]: https://en.wikipedia.org/wiki/MLOps
[data landscape 2021]: https://mattturck.com/data2021/
[10 years behind]: https://www.ascend.io/blog/podcast-diving-into-data-engineering-with-sheel-choksi/
[2020 automation]: https://twitter.com/mattturck/status/1272287334470422528


[orgex1]: https://www.getdbt.com/data-teams/data-org-structure-examples/
[orgex2]: https://medium.com/snaptravel/how-should-our-company-structure-our-data-team-e71f6846024d
[orgex3]: https://www.getdbt.com/data-teams/centralized-vs-decentralized/
[orgex4]: https://www.dominodatalab.com/blog/3-companies-3-ways-to-structure-data-science
[orgex5]: https://www.altexsoft.com/blog/datascience/how-to-structure-data-science-team-key-models-and-roles/
[orgex6]: https://snowplowanalytics.com/blog/2020/03/10/a-guide-to-data-team-structures-with-examples/
[orgex7]: https://towardsdatascience.com/organizing-data-teams-where-to-make-the-cut-49969c5ec093
[sizing-teams]: https://lethain.com/sizing-engineering-teams/
