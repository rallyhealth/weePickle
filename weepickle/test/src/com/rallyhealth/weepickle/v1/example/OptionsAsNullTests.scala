package com.rallyhealth.weepickle.v1.example

import acyclic.file
import com.rallyhealth.weejson.v1.jackson.{FromJson, ToJson}
import utest._
import com.rallyhealth.weepickle.v1.example.Simple.Thing

case class Opt(a: Option[String], b: Option[Int])
object Opt{
  implicit def rw: OptionPickler.Transceiver[Opt] = OptionPickler.macroX
}
object OptionPickler extends com.rallyhealth.weepickle.v1.AttributeTagged {
  override implicit def OptionTransmitter[T: Transmitter]: Transmitter[Option[T]] =
    implicitly[Transmitter[T]].comap[Option[T]] {
      case None => null.asInstanceOf[T]
      case Some(x) => x
    }

  override implicit def OptionReceiver[T: Receiver]: Receiver[Option[T]] = {
    new Receiver.Delegate[Any, Option[T]](implicitly[Receiver[T]].map(Some(_))){
      override def visitNull(): Option[T] = None
    }
  }
}
// end_ex

object OptionsAsNullTests extends TestSuite {

  import OptionPickler._
  implicit def rw: OptionPickler.Transceiver[Thing] = OptionPickler.macroX
  val tests = TestSuite {
    test("nullAsNone"){

      // Quick check to ensure we didn't break anything
      test("primitive"){
        fromScala("A String").transmit(ToJson.string) ==> "\"A String\""
        FromJson("\"A String\"").transmit(toScala[String]) ==> "A String"
        fromScala(1).transmit(ToJson.string) ==> "1"
        FromJson("1").transmit(toScala[Int]) ==> 1
        fromScala(Thing(1, "gg")).transmit(ToJson.string) ==> """{"myFieldA":1,"myFieldB":"gg"}"""
        FromJson("""{"myFieldA":1,"myFieldB":"gg"}""").transmit(toScala[Thing]) ==> Thing(1, "gg")
      }

      test("none"){
        fromScala[None.type](None).transmit(ToJson.string) ==> "null"
        FromJson("null").transmit(toScala[None.type]) ==> None
      }

      test("some"){
        fromScala(Some("abc")).transmit(ToJson.string) ==> "\"abc\""
        FromJson("\"abc\"").transmit(toScala[Some[String]]) ==> Some("abc")
        fromScala(Some(1)).transmit(ToJson.string) ==> "1"
        FromJson("1").transmit(toScala[Some[Int]]) ==> Some(1)
        fromScala(Some(3.14159)).transmit(ToJson.string) ==> "3.14159"
        FromJson("3.14159").transmit(toScala[Some[Double]]) ==> Some(3.14159)
      }

      test("option"){
        fromScala(Option("abc")).transmit(ToJson.string) ==> "\"abc\""
        FromJson("\"abc\"").transmit(toScala[Option[String]]) ==> Some("abc")
        FromJson("null").transmit(toScala[Option[String]]) ==> None
      }

      test("caseClass"){
        fromScala(Opt(None, None)).transmit(ToJson.string) ==> """{"a":null,"b":null}"""
        FromJson("""{"a":null,"b":null}""").transmit(toScala[Opt]) ==> Opt(None, None)
        fromScala(Opt(Some("abc"), Some(1))).transmit(ToJson.string) ==> """{"a":"abc","b":1}"""
      }

      test("optionCaseClass"){
        implicit val thingReceiver = implicitly[Receiver[Thing]]
        implicit val thingTransmitter = implicitly[Transmitter[Thing]]

        fromScala(Opt(None, None)).transmit(ToJson.string) ==> """{"a":null,"b":null}"""
        FromJson("""{"a":null,"b":null}""").transmit(toScala[Opt]) ==> Opt(None, None)
        fromScala(Opt(Some("abc"), Some(1))).transmit(ToJson.string) ==> """{"a":"abc","b":1}"""

        fromScala(Option(Thing(1, "gg"))).transmit(ToJson.string) ==> """{"myFieldA":1,"myFieldB":"gg"}"""
        FromJson("""{"myFieldA":1,"myFieldB":"gg"}""").transmit(toScala[Option[Thing]]) ==> Option(Thing(1, "gg"))
      }

      // New tests.  Work as expected.
      'customPickler {
        // Custom pickler copied from the documentation
        class CustomThing2(val i: Int, val s: String)

        object CustomThing2 {
          implicit val rw = /*weepickle.default*/ OptionPickler.readerTransmitter[String].bimap[CustomThing2](
            x => x.i + " " + x.s,
            str => {
              val Array(i, s) = str.split(" ", 2)
              new CustomThing2(i.toInt, s)
            }
          )
        }

        'customClass {
          fromScala(new CustomThing2(10, "Custom")).transmit(ToJson.string) ==> "\"10 Custom\""
          val r = FromJson("\"10 Custom\"").transmit(toScala[CustomThing2])
          assert(r.i == 10, r.s == "Custom")
        }

        'optCustomClass_Some {
          fromScala(Some(new CustomThing2(10, "Custom"))).transmit(ToJson.string) ==> "\"10 Custom\""
          val r = FromJson("\"10 Custom\"").transmit(toScala[Option[CustomThing2]])
          assert(r.get.i == 10, r.get.s == "Custom")
        }

        'optCustomClass_None {
          FromJson("null").transmit(toScala[Option[CustomThing2]]) ==> None
        }

      }

      // Copied from ExampleTests
      'Js {
        import OptionPickler._   // changed from weepickle.WeePickle._
        case class Bar(i: Int, s: String)
        implicit val fooReadWrite: Transceiver[Bar] =
          readerTransmitter[com.rallyhealth.weejson.v1.Value].bimap[Bar](
            x => com.rallyhealth.weejson.v1.Arr(x.s, x.i),
            json => new Bar(json(1).num.toInt, json(0).str)
          )

        fromScala(Bar(123, "abc")).transmit(ToJson.string) ==> """["abc",123]"""
        FromJson("""["abc",123]""").transmit(toScala[Bar]) ==> Bar(123, "abc")

        // New tests.  Last one fails.  Why?
        'option {
          'write {fromScala(Some(Bar(123, "abc"))).transmit(ToJson.string) ==> """["abc",123]"""}
          'readSome {FromJson("""["abc",123]""").transmit(toScala[Option[Bar]]) ==> Some(Bar(123, "abc"))}
          'readNull {FromJson("""null""").transmit(toScala[Option[Bar]]) ==> None}
        }
      }

    }
  }
}
