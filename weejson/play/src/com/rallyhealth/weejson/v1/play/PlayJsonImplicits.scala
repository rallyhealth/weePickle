package com.rallyhealth.weejson.v1.play

import com.rallyhealth.weepickle.v1.core.Visitor
import com.rallyhealth.weepickle.v1.WeePickle._
import play.api.libs.json._

import scala.language.implicitConversions
import scala.util.{Failure, Success, Try}

/**
  * Convenience shims back to inefficient play-json formats.
  *
  * weePickle [[FromTo]] macros are more performant than using play-json Formats.
  * play-json always requires going through an intermediate heavyweight AST:
  * - [[JsValue]] boxes every type. Extra allocations.
  * - [[JsObject]] is full of hash maps which are less efficient than struct-like classes (both cpu and memory).
  */
object PlayJsonImplicits {

  implicit class ToOps[T](val to: To[T]) extends AnyVal {

    def asReads: Reads[T] = Reads { jsValue =>
      Try(PlayJson.transform(jsValue, to)) match {
        case Success(obj) => JsSuccess(obj)
        case Failure(t)   => JsError(t.toString)
      }
    }
  }

  implicit class FromOps[T](val from: From[T]) extends AnyVal {

    def asWrites: Writes[T] = Writes[T] { obj =>
      from.transform(obj, PlayJson)
    }
  }

  implicit class ReadsOps[T](val reads: Reads[T]) extends AnyVal {

    def asTo: To[T] = PlayJson.ToJsValue.map(_.as[T](reads))

    def asToJsResult: To[JsResult[T]] = PlayJson.ToJsValue.map(_.validate[T](reads))
  }

  implicit class WritesOps[T](val writes: Writes[T]) extends AnyVal {

    def asFrom: From[T] = new From[T] {
      override def transform0[V](in: T, out: Visitor[_, V]): V = {
        val jsValue = writes.writes(in)
        PlayJson.transform(jsValue, out)
      }
    }
  }

  implicit class FromToFormat[T](val format: Format[T]) extends AnyVal {

    def asFromTo: FromTo[T] = FromTo.join(format.asTo, format.asFrom)
  }

  implicit class FormatFromTo[T](val rw: FromTo[T]) extends AnyVal {

    def asFormat: Format[T] = Format(rw.asReads, rw.asWrites)
  }

  /**
    * Adapts play-json classes as weePickle classes.
    *
    * ==CAREFUL WITH IMPORTS==
    * Do not import this with [[WeePickleConversions]] or else you may get diverging implicit loops from scalac.
    * For safety, keep these imports scoped as tightly as possible
    */
  object PlayJsonConversions {

    implicit def playFrom[T: Writes]: From[T] = implicitly[Writes[T]].asFrom

    implicit def playTo[T: Reads]: To[T] = implicitly[Reads[T]].asTo

    implicit def playToJsResult[T: Reads]: To[JsResult[T]] = implicitly[Reads[T]].asToJsResult
  }

  /**
    * Adapts weePickle classes as play-json classes.
    *
    * ==CAREFUL WITH IMPORTS==
    * Do not import this with [[PlayJsonConversions]] or else you may get diverging implicit loops from scalac.
    * For safety, keep these imports scoped as tightly as possible
    */
  object WeePickleConversions {

    implicit def weepickleReads[T: To]: Reads[T] = implicitly[To[T]].asReads

    implicit def weepickleWrites[T: From]: Writes[T] = implicitly[From[T]].asWrites
  }

}
