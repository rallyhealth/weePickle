## Differences
The https://github.com/lihaoyi/upickle macros serialize some things differently than other common libs like [circe](https://github.com/circe/circe) and [play-json](https://github.com/playframework/play-json). Many of the differences have  well-reasoned motivations, but hinder adoption as a drop-in replacement. Many engineers will blindly change the macros without realizing their API response may change in breaking ways.

In https://github.com/rallyhealth/weepickle, the macros have been changed to work more like circe and play-json, as described below.

## Default Values

```scala
case class Dflt(i: Int = 0)
```

|                    | play-json                  | lihaoyi/upickle | rallyhealth/weepickle |
|--------------------|----------------------------|-----------------|---------------------|
| `write(Dflt(0))`   | `{"i":0}`                  | `{}`            | `{"i":0}`           |
| `read[Dflt]("{}")` | `JsFailure("missing: /i")` | `Dflt(0)`       | `Dflt(0)`           |

play-json always writes a `"key":"value"` pair regardless of whether it's the case class default value or not.

weepickle works [differently](http://www.lihaoyi.com/upickle/#Defaults). If a field is missing upon deserialization, weepickle uses the default value if one exists. If a field at serialization time has the same value as the default, weepickle leaves it out of the serialized blob.

OpenAPI 3 [has support](https://swagger.io/specification/#schemaObject) for dropping defaults:

> default - The default value represents what would be assumed by the consumer of the input as the value of the schema if one is not provided.

...but the play-json macros do not. play-json is not tolerant of omitted default values: it will throw if a field is missing, even if the case class provides a default!

If a service swapped play-json with weepickle, it would be easy to stop sending fields, and break API compatibility. Any API change that requires your consumers to update is a breaking API change.
Potential for accidents is high here while there are many play-json consumers.

I've changed the macros to write all fields like play-json, but fill in missing defaults while reading like lihaoyi/upickle.

#### `@dropDefault`
I've added a field annotation, `@dropDefault` to make writes behave like [lihaoyi/upickle](https://github.com/lihaoyi/upickle).
New services (i.e., with no legacy play-json consumers) may wish to use `@dropDefault` on their OpenAPI 3 codegen'd models.

```scala
case class Foo(
  @dropDefault i: Int = 10,
  @dropDefault s: String = "lol"
)
```

|                                 | no field annotations | `@dropDefault`   |
|---------------------------------|----------------------|------------------|
| `read[Foo]("{}")`               | `Foo(10, "lol")`     | `Foo(10, "lol")` |
| `write(Foo())`                  | `{"i":10,"s":"lol"}` | `{}`             |
| `write(Foo(i = 99, s = "lol"))` | `{"i":99,"s":"lol"}` | `{"i":99}`       |


## Options
```scala
case class Maybe(m: Option[Int])
```

|                        | play-json     | lihaoyi/upickle | rallyhealth/weepickle |
|------------------------|---------------|-----------------|---------------------|
| `write(Maybe(Some(1))` | `{"m":1}`     | `{"m":[1]}`     | `{"m":1}`           |
| `write(Maybe(None))`   | `{}`          | `{"m":[]}`      | `{"m":null}`        |
| `read({"maybe":null})` | `Maybe(None)` | `Maybe(null)`   | `Maybe(None)`       |


weepickle encodes `Option` as an array. Rationale is here: https://github.com/lihaoyi/upickle/issues/75
The approach is more robust than play-json over corner cases, and simpler, too.

For example, it is not possible to write a `play.json.Writes[Option[String]]` that when encoding the value of `None`, removes the field from its parent `JsObject`. This scenario is handled by special-case logic for `Option[_]` in the `play-json` macros. Outside of those macros, the behavior of `Writes[Option[T]]` can only return the field and `null` instead:

```scala
Json.toJson(Map("some" -> Some(1), "none" -> None)) ==> "{"some":1,"none":null}"
```

#### Our needs
While `Some(None)` and `None` *could* be encoded unambiguously as `[[]]` and `[]` to support faithful round-tripping, I can't think of a realistic scenario where we'd ever want to encode anything as `Option[Option[_]]`.

OTOH, we are likely to want to encode `Option[String]` as an optional field on an OpenAPI 3 schema.
We also need to interop with play-json consumers which won't understand this encoding.
I suspect it's probably not idiomatic in most other serializers/languages as well.

### Re: null
I implemented play-json's special-case Option handling, i.e. to remove `None` fields from parent `JsObject`, but ended up reverting it (https://github.com/rallyhealth/weepickle/commit/a5599c9402dbfd0b99a98665e52351928ef446f7). Two drawbacks:
1. adds brittle complexity
2. makes it impossible to return to the lihaoyi/upickle array-based Option encoding since the special casing is hardcoded into the macro. Yuck.

Both play-json and rallyhealth/weepickle can read `null` as `None`, so I'm leaving it at that. If you want to fully remove the `"field": null`, you can do it the weepickle way using the `@dropDefault` field annotation and a default value of `None`.

```scala
case class Maybe(@dropDefault m: Option[Int] = None)
```

|                        | play-json     | rallyhealth/weepickle |
|------------------------|---------------|---------------------|
| `write(Maybe(Some(1))` | `{"m":1}`     | `{"m":1}`           |
| `write(Maybe(None))`   | `{}`          | `{}`                |
| `read({})`             | `Maybe(None)` | `Maybe(None)`       |
| `read({"maybe":null})` | `Maybe(None)` | `Maybe(None)`       |

## `discriminator`
The `@discriminator` annotation that can be used to override the default `"$type"` discriminator tag at a per-schema level.

Instead of:
```json
{
  "$type": "place",
  "locations": [
    {
      "$type": "lab",
      "labStuff": "..."
    },
    {
      "$type": "hospital",
      "hospitalStuff": "..."
    }
  ]
}
```

... you can have:
```json
{
  "providerType": "place",
  "locations": [
    {
      "locationType": "lab",
      "labStuff": "..."
    },
    {
      "locationType": "hospital",
      "hospitalStuff": "..."
    }
  ]
}
```

with `@discriminator` annotations on the sealed parent type:
```scala
@discriminator("locationType") sealed trait ProviderLocation
@key("hospital") case class Hospital() extends ProviderLocation
@key("lab") case class Lab() extends ProviderLocation

@discriminator("providerType") sealed trait Provider
@key("place") case class Place(locations: Seq[ProviderLocation]) extends Provider
```

## FromJson/ToJson, FromMsgPack/ToMsgPack
`FromJson`/`ToJson` and `FromMsgPack`/`ToMsgPack` are accessible via named objects instead of package objects. For example: `FromJson("{}")` instead of `ujson.read("{}")`. This makes them more easily importable and avoids naming collisions.

## ReadWriter => FromTo
`Reader` => `From`

`Writer` => `To`

`From` + `To` = `FromTo`

## weejson.Num(Double) => weejson.Num(BigDecimal) 
ujson's `case class Num(value: Double)` has been replaced with `case class Num(value: BigDecimal)`. This makes it capable of representing 64-bit whole numbers (particularly from external APIs) without precision loss.

The primary consequence is how JavaScript consumers are treated. JavaScript has only one number type, 64-bit floating point numbers. Seriously, put this in your chrome console:

```
Math.pow(2, 53) === (Math.pow(2, 53) + 1)
> true
```

In particular, this can become an issue when handling 64-bit `scala.Long` numbers. Nobody likes silent data loss, but encoding fields as numbers sometimes, and then strings other times, isn't great either.

Varying the encoding of a single field based on concrete number types, e.g., `{"foo": 1}` but `{"foo", "18014398509481984"}` is too surprising to do at the library level by default, since it requires special handling by the browser. Varying the encoding based on concrete values increases the likelihood of programming errors. For example, the js code `body.foo + 666` verified against the first case, would incorrectly return "18014398509481984666" for the second case. It is more straightforward to *always* encode values that can contain large values as strings consistently and declare it as such in your JSON schema.
