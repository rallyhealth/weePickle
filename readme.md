# weePickle ![version](https://img.shields.io/github/v/release/rallyhealth/weepickle) ![Maven Central](https://img.shields.io/maven-central/v/com.rallyhealth/weepickle-v1_2.13)

A *stable* JSON, YAML, MsgPack, XML, etc. serialization framework based on [uPickle](https://com-lihaoyi.github.io/upickle/).

## We're tired of dependency hell!
weePickle exists to fulfill two promises:

1. We will not break compatibility in `v1.x.y`, as enforced by [MiMa](https://github.com/lightbend/mima).
2. When we release `v2.0.0`, you can use it immediately **without waiting for your other library dependencies to update**. We achieve this by [shading](https://github.com/rallyhealth/sbt-shading).

### Shading
Both `weepickle-v1.jar` and `weepickle-v2.jar` (in the future) will coexist on the classpath peacefully by applying [shading](https://github.com/rallyhealth/sbt-shading) at multiple levels.
1. All artifact names are suffixed with the major version number (e.g. `-v1`), which prevents evictions.
2. All packages are prefixed with the major version number (e.g. `com.rallyhealth.v1`), which prevents classpath conflicts.

Shading allows libraries to depend directly on [weePickle-v1](#sbt) without fear of causing incompatible evictions and runtime failures.

For more background, see [shading.md](shading.md).

## Features
weePickle combines some of the best parts of the serialization ecosystem.

- [Zero-overhead conversion of uPickle](http://www.lihaoyi.com/post/ZeroOverheadTreeProcessingwiththeVisitorPattern.html)
- [jackson-core](https://github.com/FasterXML/jackson#active-jackson-projects): async parsing and broad format support (JSON, YAML, XML, CBOR, SMILE, Ion, etc.)
- `case class` support (any number of fields) through customizable macros
- scala json AST interop (circe, json4s, play-json, argonaut)
- Fast serialization to/from [MessagePack](#messagepack)

## sbt
![Maven Central](https://img.shields.io/maven-central/v/com.rallyhealth/weepickle-v1_2.13)
```scala
libraryDependencies += "com.rallyhealth" %% "weepickle-v1" % "version"
```

## Getting Started
#### JSON to Scala
```scala
FromJson("[1,2,3]").transform(ToScala[List[Int]])    ==> List(1, 2, 3)
```

#### Scala to JSON
```scala
FromScala(List(1, 2, 3)).transform(ToJson.string)    ==> "[1,2,3]"
```

#### JSON to pretty JSON
```scala
FromJson("[1,2,3]").transform(ToPrettyJson.string)   ==>
[
    1,
    2,
    3
]
```

#### Case Classes
```scala
import com.rallyhealth.weepickle.v1.WeePickle.{macroFromTo, FromTo}
case class Foo(i: Int)

object Foo {
  implicit val rw: FromTo[Foo] = macroFromTo
}

FromScala(Foo(1)).transform(ToJson.string)           ==> """{"i":1}"""
FromJson("""{"i":1}""").transform(ToScala[Foo])      ==> Foo(1)
```

#### Files & YAML
![Maven Central](https://img.shields.io/maven-central/v/com.rallyhealth/weepickle-v1_2.13)

```scala
libraryDependencies ++= Seq(
  "com.rallyhealth" %% "weepickle-v1" % "version",
  "com.rallyhealth" %% "weeyaml-v1" % "version"
)
```

```scala
import com.rallyhealth.weejson.v1.yaml.{FromYaml, ToYaml}
import com.rallyhealth.weejson.v1.jackson.FromJson
import java.nio.file.Files
import java.nio.file.Paths

val jsonFile = Files.newInputStream(Paths.get("file.json"))
val yamlFile = Files.newOutputStream(Paths.get("file.yml"))

FromJson(jsonFile).transform(ToYaml.outputStream(yamlFile))
```

#### XML

XML and JSON feature sets don't translate one-to-one. The output is currently "whatever jackson does."

![Maven Central](https://img.shields.io/maven-central/v/com.rallyhealth/weepickle-v1_2.13)
```scala
libraryDependencies ++= Seq(
  "com.rallyhealth" %% "weepickle-v1" % "version",
  "com.rallyhealth" %% "weexml-v1" % "version"
)
```
```scala
import com.rallyhealth.weejson.v1.xml.{FromXml, ToXml}

FromScala(Foo(1)).transform(ToXml.string)                      ==> """<root><i>1</i></root>"""
FromXml("""<root><i>1</i></root>""").transform(ToJson.string)  ==> """{"i":"1"}"""
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
- `Date`, `Instant`, `LocalDate`, `LocalTime`, `LocalDateTime`, `OffsetDateTime`, `ZonedDateTime`
- Stand-alone `case class`es and `case object`s, and their generic equivalents,
- Non-generic `case class`es and `case object`s that are part of a `sealed trait` or `sealed class` hierarchy
- `sealed trait` and `sealed class`es themselves, assuming that all subclasses are picklable
- `UUID`s
- `null`

Readability/writability is recursive: a container such as a Tuple or case class is only readable if all its contents are readable, and only writable if all its contents are writable. That means that you cannot serialize a `List[Any]`, since weePickle doesn't provide a generic way of serializing `Any`.

Case classes are serialized using the `apply` and `unapply` methods on their companion objects. This means that you can make your own classes serializable by giving them companions `apply` and `unapply`. `sealed` hierarchies are serialized as tagged unions: whatever the serialization of the actual object, together with the fully-qualified name of its class, so the correct class in the sealed hierarchy can be reconstituted later.

Anything else is not supported by default, but you can add support using Custom Picklers.

## Defaults
If a field is missing upon **deserialization**, weePickle uses the default value if one exists:

```scala
case class Dflt(i: Int = 42)
object Dflt {
  implicit val rw: FromTo[Dflt] = macroFromTo
}

FromJson("""{}""").transform(ToScala[Dflt])            ==> Dflt(42)
FromJson("""{"i": null}""").transform(ToScala[Dflt])   ==> Dflt(42)
FromJson("""{"i": 999}""").transform(ToScala[Dflt])    ==> Dflt(999)
```

If a field at **serialization** time has the same value as the default, it will be written unless annotated with `@dropDefault`:

```scala
FromScala(Dflt(42)).transform(ToJson.string)    ==> """{"i": 42}"""
FromScala(Dflt(999)).transform(ToJson.string)   ==> """{"i": 999}"""
```

### using `@dropDefault`

When **serializing**, any field annotated `@dropDefault` and a value equal to their default will be dropped from the JSON:

```scala
case class DropDflt(@dropDefault i: Int = 42)
object DropDflt {
  implicit val rw: FromTo[DropDflt] = macroFromTo
}

FromScala(DropDflt(42)).transform(ToJson.string)    ==> """{}"""
FromScala(DropDflt(999)).transform(ToJson.string)   ==> """{"i": 999}"""
```

If a ***class*** is annotated with `@dropDefault`, all fields with the same value as their default will not be written:

```scala
@dropDefault case class DropMultiDflts(i: Int = 42, j: Int = 43, k: Int = 44)
object DropMultiDflts {
  implicit val rw: FromTo[DropMultiDflts] = macroFromTo
}

FromScala(DropMultiDflts(i = 42, j = 43, k = 999)).transform(ToJson.string)   ==> """{"k": 999}"""
```

If attempting to deserialize JSON to a field that has no defaults, and the JSON value is invalid (e.g. missing, or `null`), the operation will throw an Abort Exception.

### Option Defaults

`Option` works the same way, except there is always an assumed implicit `None` default, if an explicit default is not provided. See [Option Defaults & Nulls](#Option-Defaults-&-Nulls).

## Options

If your JSON input field could be valid, `null`, or missing from the JSON, then use an `Option[T]` to catch these cases. An `Option` field will be unwrapped when serialized to JSON (unless excluded from the JSON via `@dropDefaults`), and wrapped when deserialized. ([rationale](differences.md#options)):

```scala
case class DfltOpt(i: Option[Int]) // equivalent to 'i: Option[Int] = None` for weepickling
object DfltOpt {
  implicit val rw: FromTo[DfltOpt] = macroFromTo
}

FromScala(DfltOpt(Some(42))).transform(ToJson.string)    ==> """{"i":42}"""
FromScala(DfltOpt(None)).transform(ToJson.string)        ==> """{"i":null}"""

FromJson("""{"i":42}""").transform(ToScala[DfltOpt])     ==> DfltOpt(Some(42))
FromJson("""{"i":null}""").transform(ToScala[DfltOpt])   ==> DfltOpt(None)
FromJson("""{}""").transform(ToScala[DfltOpt])           ==> DfltOpt(None)
```

### Implicit Default None & Null Handling

In JSON, `null` ["represents the intentional absence of any object value"](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/null). This value is regularly used in JSON and must be supported. Scala also has a `null` value, but the usage is *strongly discouraged*, in part because it subverts the type system. Note JSON can represent an absent value by `null`, or excluding the field from the JSON entirely.

The more idiomatic value in Scala is `None`. To support parsing JSON's `null` or `{}`, set the field type to `Option`.

An `Option` starts with an implicit default of `None` if an explicit default is not provided. Setting an explicit default to `None` (e.g. `case class DfltOpt(i: Option[Int] = None)`) is effectively equivalent for weepickle formatting (you may want to declare `None` explicitly for other non-weepickle reasons):

```scala
case class DfltOpt(i: Option[Int]) // equivalent to 'i: Option[Int] = None` for weepickling
object DfltOpt {
  implicit val rw: FromTo[DfltOpt] = macroFromTo
}
```

When **deserializing** a JSON's `null` or `{}`, this implicit default will translate to `None`. ([rationale](differences.md#re-null)):

```scala
FromJson("""{}""").transform(ToScala[DfltOpt])          ==> DfltOpt(None)
FromJson("""{i: null}""").transform(ToScala[DfltOpt])   ==> DfltOpt(None)
FromJson("""{"i": 42}""").transform(ToScala[DfltOpt])   ==> DfltOpt(Some(42))
```

When **serializing** a `None`, it will translate to a JSON `null`:

```scala
FromScala(DfltOpt(None)).transform(ToJson.string)       ==> """{"i": null}"""
FromScala(DfltOpt(Some(42))).transform(ToJson.string)   ==> """{"i": 42}"""
```

### using `@dropDefault`

When **serializing** a `None` with `@dropDefault`, and the field's default is `None` (implicit or explicit), then the field is excluded from the JSON results:

```scala
case class DropDfltOpt(@dropDefault i: Option[Int]) // equivalent to 'i: Option[Int] = None` for weepickling
object DropDfltOpt {
  implicit val rw: FromTo[DropDfltOpt] = macroFromTo
}

FromScala(DropDfltOpt(None)).transform(ToJson.string)   ==> """{}"""
FromScala(DropDfltOpt(42)).transform(ToJson.string)     ==> """{"i": 42}"""
```

When **serializing** a ***class*** that's annotated with `@dropDefault`, all fields with default values will not be written to the JSON. Remember that any `Option` without an explicit default will use the implicit default to drop `None`:

```scala
@dropDefault case class DropMultiDfltOpt(i: Option[Int] = Some(42), j: Option[Int] = Some(42), k: Option[Int], l: Option[Int])
object DropMultiDfltOpt {
  implicit val rw: FromTo[DropMultiDfltOpt] = macroFromTo
}

FromScala(DropMultiDfltOpt(i = Some(42), j = None, k = Some(999), l = None)).transform(ToJson.string)   ==> """{"j": null, "k": 999}"""
// matching defaults and values are dropped: 
// i(42 == 42), j(42 != None), k(999 != None), l(None == None) 
```

### Explicit Defaults

An `Option` will have an [implicit default of `None`](#Nones-&-Nulls), unless you provide an explicit default ([Defaults](#Defaults)):

```scala
case class DfltOpt42(i: Option[Int] = Some(42)) // overrides implicit default `None`
object DfltOpt42 {
  implicit val rw: FromTo[DfltOpt42] = macroFromTo
}
```

When **deserializing**:

```scala
FromJson("""{}""").transform(ToScala[DfltOpt42])           ==> DfltOpt42(Some(42))
FromJson("""{i: null}""").transform(ToScala[DfltOpt42])    ==> DfltOpt42(Some(42))
FromJson("""{"i": 42}""").transform(ToScala[DfltOpt42])    ==> DfltOpt42(Some(42))
FromJson("""{"i": 999}""").transform(ToScala[DfltOpt42])   ==> DfltOpt42(Some(999))
```

When **serializing**:

```scala
FromScala(DfltOpt42(None)).transform(ToJson.string)        ==> """{"i": 42}"""
FromScala(DfltOpt42(Some(42))).transform(ToJson.string)    ==> """{"i": 42}"""
FromScala(DfltOpt42(Some(999))).transform(ToJson.string)   ==> """{"i": 999}"""
````

When **serializing** with `@dropDefaults`:

```scala
case class DropDfltOpt42(@dropDefault i: Int = 42)
object DropDfltOpt42 {
  implicit val rw: FromTo[DropDfltOpt42] = macroFromTo
}

FromScala(DropDfltOpt42(None)).transform(ToJson.string)        ==> """{"i": null}"""
FromScala(DropDfltOpt42(Some(42))).transform(ToJson.string)    ==> """{}"""
FromScala(DropDfltOpt42(Some(999))).transform(ToJson.string)   ==> """{"i": 999}"""
````

## Custom Keys
weePickle allows you to specify the key with which a field is serialized via a `@key` annotation:

```scala
case class KeyBar(@key("hehehe") kekeke: Int)
object KeyBar {
  implicit val rw: FromTo[KeyBar] = macroFromTo
}

FromScala(KeyBar(10)).transform(ToJson.string)              ==> """{"hehehe":10}"""
FromJson("""{"hehehe": 10}""").transform(ToScala[KeyBar])   ==> KeyBar(10)
```

## Sealed Hierarchies
Sealed hierarchies are serialized as tagged values, the serialized object tagged with the full name of the instance's class:

```scala
sealed trait Outcome
case class Success(value: Int) extends Outcome
case class DeferredVictory(excuses: Seq[String]) extends Outcome

object Success {
  implicit val rw: FromTo[Success] = macroFromTo
}
object DeferredVictory {
  implicit val rw: FromTo[DeferredVictory] = macroFromTo
}
// order matters: the trait's companion object must come at the end for implicit resolution to work
object Outcome {
  implicit val rw: FromTo[Outcome] = macroFromTo
}

FromScala(DeferredVictory(Seq("My json AST is too slow."))).transform(ToJson.string)  ==>
  """{"$type":"com.example.DeferredVictory","excuses":["My json AST is too slow."]}"""

// You can read tagged value without knowing its
// type in advance, just use type of the sealed trait
FromJson("""{"$type":"com.example.Success","value":42}""").transform(ToScala[Outcome]) ==> Success(42)
```

You can customize the `"$type"` key and values with annotations:
```scala
@discriminator("flavor")
sealed trait Outcome

@key("s")
case class Success(value: Int) extends Outcome

@key("dv")
case class DeferredVictory(excuses: Seq[String]) extends Outcome

FromScala(Success(42)).transform(ToJson.string)      ==> """{"flavor":"s",value:42}""" 
```

## Enumerations
```scala
object Suit extends Enumeration {
  val Spades = Value("Spades")
  val Hearts = Value("Hearts")
  val Diamonds = Value("Diamonds")
  val Clubs = Value("Clubs")

  implicit val pickler = WeePickle.fromToEnumerationName(this)
}

FromScala(Suit.Spades).transform(ToJson.string)         ==> """"Spades""""
FromJson(""""Spades"""").transform(ToScala[Suit.Value]) ==> Suit.Spades
```

## jackson-core
weePickle leans heavily on [jackson-core](https://github.com/FasterXML/jackson-core) for interop with JSON, YAML, and most other formats. [Jackson-databind](https://github.com/FasterXML/jackson-databind) is not used.

### Motivations
1. jackson-core's JSON support is mature, widely used, and heavily optimized.
2. The ecosystem of possible formats is huge: https://github.com/FasterXML/jackson#active-jackson-projects
3. jackson-core has a solid track record of backward compatibility.

### Buffer pooling
Internally, jackson-core uses buffer pooling to achieve some of its performance. Buffers return to the pool after calling `close()` on the underlying Parser/Generator. If this doesn't happen, new buffers get allocated for each message, and performance suffers slightly.

`FromJson` doesn't trust you and calls `close()` automatically after writing a single [json text](https://tools.ietf.org/html/rfc7159#section-2), which covers the vast majority of use cases. If you're working with multiple json texts separated by whitespace, jackson can handle it, but you have to drop down below the high level API and remember to close the parser/generator yourself.

## Value AST
WeePickle includes its own AST named `Value`, largely unchanged from the upstream [uJson](https://com-lihaoyi.github.io/upickle/#uJson):

```scala
val obj = Obj(
  "foo" -> Arr(
    42,
    "omg",
    true
  )
)

obj("foo")(0).num                  ==> 42

obj.toString                       ==> """{"foo":[42,"omg",true]}"""
obj.transform(ToPrettyJson.string) ==>
  """{
    "foo": [
      42,
      "omg",
      true
    ]
  }"""

FromJson("""{"foo":[42,"omg",true]}""").transform(Value) ==> obj
```

See:
- https://com-lihaoyi.github.io/upickle/#uJson
- http://www.lihaoyi.com/post/uJsonfastflexibleandintuitiveJSONforScala.html

## MessagePack
weePack is weePickle's [MessagePack](https://msgpack.org/index.html) implementation, largely unchanged from the upstream [uPack](https://com-lihaoyi.github.io/upickle/#uPack).

### sbt
![Maven Central](https://img.shields.io/maven-central/v/com.rallyhealth/weepack-v1_2.13)
```scala
libraryDependencies += "com.rallyhealth" %% "weepack-v1" % "version"
```

### Benchmarks
`FromMsgPack`/`ToMsgPack` perform exceptionally well under benchmarks, yielding higher throughput than JSON or the official [jackson-dataformat-msgpack](https://github.com/msgpack/msgpack-java/blob/develop/msgpack-jackson/README.md).

#### ParserBench
java 11:
```
Benchmark                    Mode  Cnt    Score    Error  Units
ParserBench.jsonBytes       thrpt   15  245.665 ±  3.202  ops/s
ParserBench.jsonString      thrpt   15  213.312 ±  5.250  ops/s
ParserBench.msgpackJackson  thrpt   15  205.738 ±  2.789  ops/s
ParserBench.msgpackScala    thrpt   15  422.313 ± 17.172  ops/s
ParserBench.smile           thrpt   15  271.947 ±  1.116  ops/s
```

#### GeneratorBench
java 11:
```
Benchmark                       Mode  Cnt    Score    Error  Units
GeneratorBench.jsonBytes       thrpt   15  238.335 ± 11.777  ops/s
GeneratorBench.jsonString      thrpt   15  240.125 ±  7.871  ops/s
GeneratorBench.msgpackJackson  thrpt   15  181.195 ±  5.774  ops/s
GeneratorBench.msgpackScala    thrpt   15  304.540 ±  2.225  ops/s
GeneratorBench.smile           thrpt   15  306.462 ±  3.134  ops/s
```

## Limitations
- ScalaJS is not supported (jackson-core is java-only)
- Same macro limitations as [uPickle](https://com-lihaoyi.github.io/upickle/#Limitations)
- XML support is still rudimentary and contributions are welcome.

## Developing
See [developing.md](developing.md) for building, testing, and IDE support.

## Upstream

uPickle: a simple Scala JSON and Binary (MessagePack) serialization library

- [Documentation](https://lihaoyi.github.io/upickle)
- [Project root](https://github.com/lihaoyi/upickle)

If you use uPickle/weePickle and like it, please support it by donating to lihaoyi's Patreon:

- [https://www.patreon.com/lihaoyi](https://www.patreon.com/lihaoyi)

Thanks to [JSONTestSuite](https://github.com/nst/JSONTestSuite) for the comprehensive collection of interesting JSON test files.
