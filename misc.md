## [Shading](https://github.com/rallyhealth/sbt-shading) + [SemVer](https://semver.org/) + [MiMa](https://github.com/lightbend/mima)

Many of Rally Health's libraries need to work with JSON. However, we have
found (through long painful experience) that letting them use Play-Json
puts us into a particular form of Dependency Hell: the libraries wind up
dependent on a *specific version* of Play-Json, which means that upgrading
Play requires upgrading much of our ecosystem, which is a hassle. We want
to decouple our libraries from Play as much as possible, to reduce this
friction.

So we are encouraging libraries to make use of weePickle instead: it's
popular, well-supported and fast.

However, if we allowed libraries to simply pick random versions of
uPickle, we'd be right back where we were with Play-Json: if different
libraries used different versions, we could wind up with evictions and
runtime collisions, since uPickle isn't shaded.

So this is a shaded fork of uPickle. It is hard-shaded (instead of using
sbt-assembly or something like that) because uPickle includes macros with
hard-coded paths, so automatic shading isn't likely to work correctly.

## Differences
The upstream https://github.com/lihaoyi/upickle macros serialize some things differently than other common libs like circe and play-json. Many of the differences have well-reasoned motivations, but hinder adoption as a drop-in replacement.

In https://github.com/rallyhealth/weePickle, the macros have been changed to work
more like circe and play-json, as described [here](differences.md).



### Original Readme

uPickle: a simple Scala JSON and Binary (MessagePack) serialization library

- [Documentation](https://lihaoyi.github.io/uPickle)

If you use uPickle and like it, please support it by donating to lihaoyi's Patreon:

- [https://www.patreon.com/lihaoyi](https://www.patreon.com/lihaoyi)

[![Build Status](https://travis-ci.org/rallyhealth/weePickle.svg)](https://travis-ci.org/rallyhealth/weePickle)
