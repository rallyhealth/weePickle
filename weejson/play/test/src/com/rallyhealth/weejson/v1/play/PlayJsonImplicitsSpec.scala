package com.rallyhealth.weejson.v1.play

import com.rallyhealth.weejson.v1.jackson.{FromJson, ToJson}
import com.rallyhealth.weepickle.v1.WeePickle.{FromScala, ToScala}
import org.scalactic.TypeCheckedTripleEquals
import org.scalatest.{FreeSpec, Inside, Matchers}
import play.api.libs.json._

class PlayJsonImplicitsSpec
  extends FreeSpec
  with Matchers
  with Inside
  with TypeCheckedTripleEquals {

  "valid" - {
    val string = """{"enveloped":{"ciphertext":"pony"}}"""

    "weePickle wraps play" - {
      val message = WeePickleEnvelope(PlayCiphertext("pony"))

      "weepickle" - {
        "weepickle.write" in assert(FromScala(message).transmit(ToJson.string) === string)
        "weepickle.read" in assert(FromJson(string).transmit(ToScala[WeePickleEnvelope]) === message)
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

        "weepickle.write" in assert(FromScala(message).transmit(ToJson.string) === string)
        "weepickle.read" in assert(FromJson(string).transmit(ToScala[PlayEnvelope]) === message)
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
          (the[Exception] thrownBy (FromJson(json).transmit(ToScala[Envelope]))).getCause shouldBe a[JsResultException]
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
          (the[Exception] thrownBy (FromJson(json)).transmit(ToScala[Envelope])).getCause shouldBe a[JsResultException]
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
}
