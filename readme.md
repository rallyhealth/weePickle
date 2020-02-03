## WeePickle
A JSON, YAML, MsgPack, XML, etc. serialization framework with MiMa and shading.
Safe for use by libraries without causing dependency hell.

## Features
- [SemVer](https://semver.org/) ([mima](https://github.com/lightbend/mima)) + [Shading](https://github.com/rallyhealth/sbt-shading) => strong backwards compatibility + forwards interop, even across major versions
- [Zero-overhead conversion](http://www.lihaoyi.com/post/ZeroOverheadTreeProcessingwiththeVisitorPattern.html) between:
    - [jackson-core formats](https://github.com/FasterXML/jackson#active-jackson-projects) (JSON, YAML, XML, CBOR, SMILE, Ion, etc.)
    - scala json ASTs (circe, json4s, play-json, argonaut)
    - case classes (flexible macros)

## sbt

```sbt
resolvers += "Rally Health" at "https://dl.bintray.com/rallyhealth/maven"
```
<table>
<tr>
<td><pre lang="sbt">
"com.rallyhealth" %% "weepickle-v1" % "version"
</pre></td>
<td><img src="https://api.bintray.com/packages/rallyhealth/maven/weePickle/images/download.svg" title="Download" /></td>
</tr>
</table>


## Getting Started
Json to scala:
```scala
FromJson("[1,2,3]").transform(ToScala[List[Int]]) // List(1, 2, 3)
```

Scala to json:
```scala
FromScala(List(1, 2, 3)).transform(ToJson.string) // "[1,2,3]"
```

Json to pretty json:
```scala
FromJson("[1,2,3]").transform(ToPrettyJson.string)
// [
//     1,
//     2,
//     3
// ]
```

Files & yaml:
```scala
val jsonFile = Files.newInputStream(Paths.get("file.json"))
val yamlFile = Files.newOutputStream(Paths.get("file.yml"))

FromJson(jsonFile).transform(ToYaml.outputStream(yamlFile))
```

case classes:
```scala
import com.rallyhealth.weepickle.v1.WeePickle
case class Foo(i: Int)

object Foo {
  implicit val rw = WeePickle.macroFromTo[Foo]
}

FromScala(Foo(1)).transform(ToJson.string)        // """{"i":1}"""
FromJson("""{"i":1}""").transform(ToScala[Foo])   // Foo(1)
```

## Pick Any Two
You can convert directly between any `From`/`To` types. See [Zero-Overhead Tree Processing with the Visitor Pattern](http://www.lihaoyi.com/post/ZeroOverheadTreeProcessingwiththeVisitorPattern.html) for how this works.

The following is a non-exhaustive map of type support:
![From and To convertable types](readme/map.svg)

## Supported Types
- `Boolean`, `Byte`, `Char`, `Short`, `Int`, `Long`, `Float`, `Double`
- `Tuple`s from 1 to 22
- Immutable `Seq`, `List`, `Vector`, `Set`, `SortedSet`, `Option`, `Array`, `Maps`, and all other collections with a reasonable `CanBuildFrom` implementation
- `Duration`, `Either`,
- `Date`, `Instant`, `LocalDate`, `LocalTime`, `LocalDateTime`, `OffsetDateTime`
- Stand-alone `case class`es and `case object`s, and their generic equivalents,
- Non-generic `case class`es and `case object`s that are part of a `sealed trait` or `sealed class` hierarchy
- `sealed trait` and `sealed class`es themselves, assuming that all subclasses are picklable
- `UUID`s
- `null`

Readability/writability is recursive: a container such as a Tuple or case class is only readable if all its contents are readable, and only writable if all its contents are writable. That means that you cannot serialize a `List[Any]`, since weePickle doesn't provide a generic way of serializing `Any`. Case classes are only serializable up to 64 fields.

Case classes are serialized using the `apply` and `unapply` methods on their companion objects. This means that you can make your own classes serializable by giving them companions `apply` and `unapply`. `sealed` hierarchies are serialized as tagged unions: whatever the serialization of the actual object, together with the fully-qualified name of its class, so the correct class in the sealed hierarchy can be reconstituted later.

Anything else is not supported by default, but you can add support using Custom Picklers.

## Defaults
If a field is missing upon deserialization, weePickle uses the default value if one exists.

```scala
case class Dflt(i: Int = 42)

FromJson("""{}""").transform(ToScala[Dflt])          // Dflt(42)
FromJson("""{"i": 999}""").transform(ToScala[Dflt])  // Dflt(999)
```

If a field at serialization time has the same value as the default, it will be written unless annotated with `@dropDefault`.

```scala
FromScala(Dflt(42)).transform(ToJson.string)         // """{"i": 42}"""
```

```scala
case class Dflt2(@dropDefault i: Int = 42)
FromScala(Dflt2(42)).transform(ToJson.string)        // """{}"""
```

## Options
`Option[T]` are unwrapped when the option is `Some` ([rationale](differences.md#options)):

```scala
case class Maybe1(i: Option[Int])
object Maybe1 {
  implicit val rw = WeePickle.macroFromTo[Maybe1]
}

FromScala(Maybe1(Some(42))).transform(ToJson.string) ==> """{"i":42}"""
FromJson("""{"i":42}""").transform(ToScala[Maybe1]) ==> Maybe1(Some(42))
```

`None` is translated as `null` ([rationale](differences.md#re-null)):

```scala
FromScala(Maybe1(None)).transform(ToJson.string) ==> """{"i":null}"""
FromJson("""{"i":null}""").transform(ToScala[Maybe1]) ==> Maybe1(None)
FromJson("""{}""").transform(ToScala[Maybe1]) ==> Maybe1(None)
```

If you want to suppress the field entirely on `None`, you can use [Defaults](#Defaults).
```scala
case class Maybe2(@dropDefault i: Option[Int] = None)

FromScala(Maybe2(None)).transform(ToJson.string) ==> """{}"""
```

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
The upstream https://github.com/lihaoyi/upickle macros serialize some things differently
than other common libs like circe and play-json. Many of the differences have 
well-reasoned motivations, but hinder adoption as a drop-in replacement.

In https://github.com/rallyhealth/weePickle, the macros have been changed to work
more like circe and play-json, as described [here](differences.md).

### Building weePickle

weePickle is based on Mill, not sbt. (There is an sbt file, but it's just
for the documentation.) In order to build this, you will need to install
Mill:
```
brew install mill
```

#### IntelliJ
You can generate an IntelliJ project structure with:
```
./genIdea.sh
```

#### Compile

To build the entire system, say:
```
mill __.compile
```
In Mill, a single underscore `_` is the wildcard, and a double underscore
`__` is a recursive wildcard that drills into sub-modules. So the above
formula means "find all modules and submodules, and compile them".

#### Testing

Similarly, to run all unit tests:
```
mill __.test
```

#### Packaging

To create the JAR files:
```
mill __.jar
```

(There's also a `__.assembly` task, which I believe creates fatjars,
but I can't see why we would care in this case.)

#### When Things Silently Fail

Mill has one iffy characteristic: when something is broken in the
`build.sc` file, it will often fail silently. No errors or anything;
just nothing happens.

When this occurs, the `resolve` command tends to be a lifesaver. This
steps back and just shows what tasks *would* be run by the given
command, and it does generally show the errors.

So for example, when compile seems to be dead in the water, say:
```
mill resolve __.compile
```
and it will generally show you what's broken.

### Original Readme

uPickle: a simple Scala JSON and Binary (MessagePack) serialization library

- [Documentation](https://lihaoyi.github.io/uPickle)

If you use uPickle and like it, please support it by donating to lihaoyi's Patreon:

- [https://www.patreon.com/lihaoyi](https://www.patreon.com/lihaoyi)

[![Build Status](https://travis-ci.org/rallyhealth/weePickle.svg)](https://travis-ci.org/rallyhealth/weePickle)
