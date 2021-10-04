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
abstract--that happen under this umbrella. The type of work varies
greatly by the particular flavor of architecture in question; whether
dealing with traditional [application architecture][], large-scale
[enterprise architecture][], or otherwise traversing the floors of the
[architect elevator][], the artifacts I produced day-to-day vary
widely.

One lesser-discussed category of work that I find valuable in this
conversation is what I term **architecture intersection**, the process
of juxtaposing and aligning different architectural domains so they
are jointly better realized. I recognize this is a very abstract
definition, so here are some examples work that fall into this bucket:

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
large organizations, a lot of these layers are by lack of necessity
far less formalized--but intuitively this is something most engineers
already do at various scales without having a term attached: We juggle
performance, security, extensibility, scalability, and [all the other
ilities][system quality attributes] at a functional- and code-level,
so architecture intersection is effectively the same activity at a
higher level of abstraction.

So why attach a term to this activity? I'm hoping to discuss a series
of these intersections, highlighting how anchoring decisions in one
architectural domain impacts another, starting with the last
bullet-point above: How [organizational architecture][] impacts the
[systems architecture][] of a data platform.

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

[orgex1]: https://www.getdbt.com/data-teams/data-org-structure-examples/
[orgex2]: https://medium.com/snaptravel/how-should-our-company-structure-our-data-team-e71f6846024d
[orgex3]: https://www.getdbt.com/data-teams/centralized-vs-decentralized/
[orgex4]: https://www.dominodatalab.com/blog/3-companies-3-ways-to-structure-data-science
[orgex5]: https://www.altexsoft.com/blog/datascience/how-to-structure-data-science-team-key-models-and-roles/
[orgex6]: https://snowplowanalytics.com/blog/2020/03/10/a-guide-to-data-team-structures-with-examples/
[orgex7]: https://towardsdatascience.com/organizing-data-teams-where-to-make-the-cut-49969c5ec093
[sizing-teams]: https://lethain.com/sizing-engineering-teams/
