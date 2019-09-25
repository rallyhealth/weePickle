## Differences
The https://github.com/lihaoyi/upickle macros serialize some things differently than other common libs like circe and play-json. Many of the differences have  well-reasoned motivations, but hinder adoption as a drop-in replacement. Many engineers will blindly change the macros without realizing their API response may change in breaking ways.

In https://github.com/rallyhealth/upickle, the macros have been changed to work more like circe and play-json, as described below.

## Default Values

```scala
case class Dflt(i: Int = 0)
```

|                    | play-json                  | lihaoyi/upickle | rallyhealth/upickle |
|--------------------|----------------------------|-----------------|---------------------|
| `write(Dflt(0))`   | `{"i":0}`                  | `{}`            | `{"i":0}`           |
| `read[Dflt]("{}")` | `JsFailure("missing: /i")` | `Dflt(0)`       | `Dflt(0)`           |

play-json always writes a `"key":"value"` pair regardless of whether it's the case class default value or not.

uPickle works [differently](http://www.lihaoyi.com/upickle/#Defaults). If a field is missing upon deserialization, uPickle uses the default value if one exists. If a field at serialization time has the same value as the default, uPickle leaves it out of the serialized blob.

OpenAPI 3 [has support](https://swagger.io/specification/#schemaObject) for uPickle's behavior:

> default - The default value represents what would be assumed by the consumer of the input as the value of the schema if one is not provided.

...but the play-json macros do not. play-json is not tolerant of omitted default values will throw if a field is missing, even if the case-class provdes a default!

If a service swapped play-json with upickle, it would be easy to stop sending fields, and break API compatibility. Any API change that requires your consumers to update is a breaking API change.
Potential for accidents is high here while there are many play-json consumers.

I've changed the macros to write all fields like play-json, but fill in missing defaults while reading like lihaoyi/upickle.

#### `@dropDefault`
I've added a field annotation, `@dropDefault` to make writes behave like lihaoyi/upickle.
New services (i.e. with no legacy play-json consumers) may wish to use `@dropDefault` on their OpenAPI 3 codegen'd models.

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

|                        | play-json     | lihaoyi/upickle | rallyhealth/upickle |
|------------------------|---------------|-----------------|---------------------|
| `write(Maybe(Some(1))` | `{"m":1}`     | `{"m":[1]}`     | `{"m":1}`           |
| `write(Maybe(None))`   | `{}`          | `{"m":[]}`      | `{"m":null}`        |
| `read({"maybe":null})` | `Maybe(None)` | `Maybe(null)`   | `Maybe(None)`       |


uPickle encodes `Option` as an array. Rationale is here: https://github.com/lihaoyi/upickle/issues/75
The approach is more robust than play-json over corner cases and simpler, too.

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
I implemented play-json's special-case Option handling, i.e. to remove `None` fields from parent `JsObject`, but ended up reverting it (https://github.com/rallyhealth/upickle/commit/a5599c9402dbfd0b99a98665e52351928ef446f7). Two drawbacks:
1. adds brittle complexity
2. makes it impossible to return to the lihaoyi/upickle array-based Option encoding since the special casing is hardcoded into the macro. Yuck.

Both play-json and rallyhealth/upickle can read `null` as `None`, so I'm leaving it at that. If you want to fully remove the `"field": null`, you can do it the uPickle way using the `@dropDefault` field annotation and a default value of `None`.

```scala
case class Maybe(@dropDefault m: Option[Int] = None)
```

|                        | play-json     | rallyhealth/upickle |
|------------------------|---------------|---------------------|
| `write(Maybe(Some(1))` | `{"m":1}`     | `{"m":1}`           |
| `write(Maybe(None))`   | `{}`          | `{}`                |
| `read({})`             | `Maybe(None)` | `Maybe(None)`       |
| `read({"maybe":null})` | `Maybe(None)` | `Maybe(None)`       |
