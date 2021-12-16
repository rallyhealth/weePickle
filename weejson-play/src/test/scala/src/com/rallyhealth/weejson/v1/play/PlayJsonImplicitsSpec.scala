package com.rallyhealth.weejson.v1.play

import com.rallyhealth.weejson.v1.Null
import com.rallyhealth.weejson.v1.jackson.{FromJson, ToJson}
import com.rallyhealth.weepickle.v1.WeePickle.{FromScala, ToScala}
import org.scalactic.TypeCheckedTripleEquals
import org.scalatest.{Assertion, Inside}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers
import play.api.libs.json._
import com.rallyhealth.weepickle.v1.WeePickle._

class PlayJsonImplicitsSpec extends AnyFreeSpec with Matchers with Inside with TypeCheckedTripleEquals {

  "valid" - {
    val string = """{"enveloped":{"ciphertext":"pony"}}"""

    "weePickle wraps play" - {
      val message = WeePickleEnvelope(PlayCiphertext("pony"))

      "weepickle" - {
        "weepickle.write" in assert(FromScala(message).transform(ToJson.string) === string)
        "weepickle.read" in assert(FromJson(string).transform(ToScala[WeePickleEnvelope]) === message)
      }

      "play-json" - {
        import com.rallyhealth.weejson.v1.play.PlayJsonImplicits.WeePickleConversions._

        "play-json.parse.as" in assert(Json.parse(string).as[WeePickleEnvelope] === message)
        "play-json.parse.validate" in assert(Json.parse(string).validate[WeePickleEnvelope].get === message)
        "play-json.toJson" in assert(Json.toJson(message).toString === string)
      }
    }

    "play wraps weePickle" - {
      val message = PlayEnvelope(WeePickleCiphertext("pony"))

      "weepickle" - {
        import com.rallyhealth.weejson.v1.play.PlayJsonImplicits.PlayJsonConversions._

        "weepickle.write" in assert(FromScala(message).transform(ToJson.string) === string)
        "weepickle.read" in assert(FromJson(string).transform(ToScala[PlayEnvelope]) === message)
      }

      "play-json" - {
        "play-json.parse.as" in assert(Json.parse(string).as[PlayEnvelope] === message)
        "play-json.parse.validate" in assert(Json.parse(string).validate[PlayEnvelope].get === message)
        "play-json.toJson" in assert(Json.toJson(message).toString === string)
      }
    }
  }

  "errors" - {
    val json = """{"enveloped":{"ciphertext":-666}}"""

    "weePickle wraps play" - {
      type Envelope = WeePickleEnvelope

      "weepickle" - {
        "weepickle.read" in {
          (the[Exception] thrownBy (FromJson(json).transform(ToScala[Envelope]))).getCause shouldBe a[JsResultException]
        }
      }

      "play-json" - {
        import com.rallyhealth.weejson.v1.play.PlayJsonImplicits.WeePickleConversions._

        "play-json.parse.as" in (a[JsResultException] shouldBe thrownBy(Json.parse(json).as[Envelope]))
        "play-json.parse.validate" in inside(Json.parse(json).validate[Envelope]) {
          case JsError(errors) =>
            // JsError(List((,List(ValidationError(List(play.api.libs.json.JsResultException: JsResultException(errors:List((/ciphertext,List(ValidationError(List(weepickle.core.Abort: expected string got float64 string),WrappedArray())))))),WrappedArray())))))
            errors should not be empty
        }
      }

    }

    "play wraps weePickle" - {
      type Envelope = PlayEnvelope

      "weepickle" - {
        import com.rallyhealth.weejson.v1.play.PlayJsonImplicits.PlayJsonConversions._

        "weepickle.read" in {
          (the[Exception] thrownBy (FromJson(json)).transform(ToScala[Envelope])).getCause shouldBe a[JsResultException]
        }
      }

      "play-json" - {
        "play-json.parse.as" in (a[JsResultException] shouldBe thrownBy(Json.parse(json).as[Envelope]))
        "play-json.parse.validate" in inside(Json.parse(json).validate[Envelope]) {
          case JsError(errors) =>
            // JsError(List((,List(ValidationError(List(play.api.libs.json.JsResultException: JsResultException(errors:List((/enveloped,List(ValidationError(List(weepickle.core.Abort: expected string got float64 string),WrappedArray())))))),WrappedArray())))))
            errors should not be empty
        }
      }
    }
  }

  "implicit scope" - {
    case class NoCompanion(msg: String)
    implicit val reads: Reads[NoCompanion] = Reads(v => JsSuccess(NoCompanion("")))
    val jsValue = Json.obj("msg" -> "")
    jsValue.as[NoCompanion]
  }

  "primitive translations" in {
    implicit class CheckJsValue(jsValue: JsValue) {
      def -->[T: To](toValue: T): Assertion =
        assert(PlayJson.FromJsValue.transform(jsValue, to[T]) === toValue)

      def <--[T: From](fromValue: T): Assertion =
        assert(FromScala(fromValue).transform(PlayJson.ToJsValue) === jsValue)

      def <-->[T: To: From](expected: T): Assertion = {
        -->(expected)
        <--(expected)
      }
    }

    val jsPony = JsString("pony")

    JsNull <--> Null
    JsBoolean(true) <--> true // JsTrue in Play > 2.5
    JsBoolean(false) <--> false // JsFalse in Play > 2.5
    jsPony <--> "pony"
    JsObject(Seq("ciphertext" -> jsPony)) <--> WeePickleCiphertext("pony")
    JsArray(Seq(jsPony, jsPony, jsPony)) <--> Seq("pony", "pony", "pony")

    // Cover various common numeric representations (not comprehensive)
    // and proper truncation when represented as an int

    JsArray(
      Seq(JsNumber(-1.1e11),
          JsNumber(-1.1),
          JsNumber(-1),
          JsNumber(0),
          JsNumber(1.toDouble),
          JsNumber(1.1),
          JsNumber(1.1e11))) <-->
      Seq(-1.1e11, -1.1, -1, 0, 1, 1.1, 1.1e11)
    JsNumber(0) <--> 0 // number from int (no frac) to/from int
    JsNumber(1.toDouble) <--> 1 // number from double (no frac) to/from int
    JsNumber(1.toDouble) <--> 1.0 // number from double (no frac) to/from double
    JsNumber(1.toDouble) <--> 1L // number from double (no frac) to/from long
    JsNumber(1.1) <--> 1.1 // number from double (w/frac) to/from double
    JsNumber(1.1) --> 1 // number from double (w/frac) to int (truncated)
    JsNumber(1.1) --> 1L // number from double (w/frac) to long (truncated)
    JsNumber(1.99999) --> 1 // number from double (w/frac) to int (truncated)
    JsNumber(1.99999) --> 1L // number from double (w/frac) to int (truncated)
    JsNumber(1.1e+11) <--> 1.1e+11 // number from double (w/frac+exp) to/from double
    JsNumber(1.1e+11.toLong) <--> 1.1e+11.toLong // number from long (w/frac+exp) to/from long
    JsNumber(1.1e+11) <--> 1.1e+11.toLong // number from double (w/frac+exp) to/from long
    JsNumber(1.1e30) <--> 1.1e30 // number from double (w/frac+exp) to/from double
    JsNumber(1.1e-11) <--> 1.1e-11
    JsNumber(1.1e-11) --> 0 // Truncates
    JsNumber(-1.1e-11) --> 0 // Truncates
    JsNumber(-1) <--> -1
    JsNumber(-1.1) <--> -1.1
    JsNumber(-1.1) --> -1 // Truncates
    JsNumber(-1.99999) --> -1 // Truncates
    JsNumber(-1.1e11) <--> -1.1e11.toLong
    JsNumber(-1.1e30) <--> -1.1e30
  }
}
