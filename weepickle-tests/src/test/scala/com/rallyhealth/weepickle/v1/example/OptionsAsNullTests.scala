package com.rallyhealth.weepickle.v1.example

import acyclic.file
import com.rallyhealth.weejson.v1.jackson.{FromJson, ToJson}
import utest._
import com.rallyhealth.weepickle.v1.example.Simple.Thing

case class Opt(a: Option[String], b: Option[Int])
object Opt {
  implicit def rw: OptionPickler.FromTo[Opt] = OptionPickler.macroFromTo
}
object OptionPickler extends com.rallyhealth.weepickle.v1.AttributeTagged {
  override implicit def OptionFrom[T: From]: From[Option[T]] =
    implicitly[From[T]].comap[Option[T]] {
      case None    => null.asInstanceOf[T]
      case Some(x) => x
    }

  override implicit def ToOption[T: To]: To[Option[T]] = {
    new To.Delegate[Any, Option[T]](implicitly[To[T]].map(Some(_))) {
      override def visitNull(): Option[T] = None
    }
  }
}
// end_ex

object OptionsAsNullTests extends TestSuite {

  import OptionPickler._
  implicit def rw: OptionPickler.FromTo[Thing] = OptionPickler.macroFromTo
  val tests = TestSuite {
    test("nullAsNone") {

      // Quick check to ensure we didn't break anything
      test("primitive") {
        fromScala("A String").transform(ToJson.string) ==> "\"A String\""
        FromJson("\"A String\"").transform(toScala[String]) ==> "A String"
        fromScala(1).transform(ToJson.string) ==> "1"
        FromJson("1").transform(toScala[Int]) ==> 1
        fromScala(Thing(1, "gg")).transform(ToJson.string) ==> """{"myFieldA":1,"myFieldB":"gg"}"""
        FromJson("""{"myFieldA":1,"myFieldB":"gg"}""").transform(toScala[Thing]) ==> Thing(1, "gg")
      }

      test("none") {
        fromScala[None.type](None).transform(ToJson.string) ==> "null"
        FromJson("null").transform(toScala[None.type]) ==> None
      }

      test("some") {
        fromScala(Some("abc")).transform(ToJson.string) ==> "\"abc\""
        FromJson("\"abc\"").transform(toScala[Some[String]]) ==> Some("abc")
        fromScala(Some(1)).transform(ToJson.string) ==> "1"
        FromJson("1").transform(toScala[Some[Int]]) ==> Some(1)
        fromScala(Some(3.14159)).transform(ToJson.string) ==> "3.14159"
        FromJson("3.14159").transform(toScala[Some[Double]]) ==> Some(3.14159)
      }

      test("option") {
        fromScala(Option("abc")).transform(ToJson.string) ==> "\"abc\""
        FromJson("\"abc\"").transform(toScala[Option[String]]) ==> Some("abc")
        FromJson("null").transform(toScala[Option[String]]) ==> None
      }

      test("caseClass") {
        fromScala(Opt(None, None)).transform(ToJson.string) ==> """{"a":null,"b":null}"""
        FromJson("""{"a":null,"b":null}""").transform(toScala[Opt]) ==> Opt(None, None)
        fromScala(Opt(Some("abc"), Some(1))).transform(ToJson.string) ==> """{"a":"abc","b":1}"""
      }

      test("optionCaseClass") {
        implicit val thingTo = implicitly[To[Thing]]
        implicit val thingFrom = implicitly[From[Thing]]

        fromScala(Opt(None, None)).transform(ToJson.string) ==> """{"a":null,"b":null}"""
        FromJson("""{"a":null,"b":null}""").transform(toScala[Opt]) ==> Opt(None, None)
        fromScala(Opt(Some("abc"), Some(1))).transform(ToJson.string) ==> """{"a":"abc","b":1}"""

        fromScala(Option(Thing(1, "gg"))).transform(ToJson.string) ==> """{"myFieldA":1,"myFieldB":"gg"}"""
        FromJson("""{"myFieldA":1,"myFieldB":"gg"}""").transform(toScala[Option[Thing]]) ==> Option(Thing(1, "gg"))
      }

      // New tests.  Work as expected.
      'customPickler {
        // Custom pickler copied from the documentation
        class CustomThing2(val i: Int, val s: String)

        object CustomThing2 {
          implicit val rw = /*weepickle.default*/ OptionPickler
            .fromTo[String]
            .bimap[CustomThing2](
              x => x.i + " " + x.s,
              str => {
                val Array(i, s) = str.split(" ", 2)
                new CustomThing2(i.toInt, s)
              }
            )
        }

        'customClass {
          fromScala(new CustomThing2(10, "Custom")).transform(ToJson.string) ==> "\"10 Custom\""
          val r = FromJson("\"10 Custom\"").transform(toScala[CustomThing2])
          assert(r.i == 10, r.s == "Custom")
        }

        'optCustomClass_Some {
          fromScala(Some(new CustomThing2(10, "Custom"))).transform(ToJson.string) ==> "\"10 Custom\""
          val r = FromJson("\"10 Custom\"").transform(toScala[Option[CustomThing2]])
          assert(r.get.i == 10, r.get.s == "Custom")
        }

        'optCustomClass_None {
          FromJson("null").transform(toScala[Option[CustomThing2]]) ==> None
        }

      }

      // Copied from ExampleTests
      'Js {
        import OptionPickler._ // changed from weepickle.WeePickle._
        case class Bar(i: Int, s: String)
        implicit val fooReadWrite: FromTo[Bar] =
          fromTo[com.rallyhealth.weejson.v1.Value].bimap[Bar](
            x => com.rallyhealth.weejson.v1.Arr(x.s, x.i),
            json => new Bar(json(1).num.toInt, json(0).str)
          )

        fromScala(Bar(123, "abc")).transform(ToJson.string) ==> """["abc",123]"""
        FromJson("""["abc",123]""").transform(toScala[Bar]) ==> Bar(123, "abc")

        // New tests.  Last one fails.  Why?
        'option {
          'write { fromScala(Some(Bar(123, "abc"))).transform(ToJson.string) ==> """["abc",123]""" }
          'readSome { FromJson("""["abc",123]""").transform(toScala[Option[Bar]]) ==> Some(Bar(123, "abc")) }
          'readNull { FromJson("""null""").transform(toScala[Option[Bar]]) ==> None }
        }
      }

    }
  }
}
