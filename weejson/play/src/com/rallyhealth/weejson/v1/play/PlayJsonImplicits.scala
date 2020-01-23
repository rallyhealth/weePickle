package com.rallyhealth.weejson.v1.play

import com.rallyhealth.weepickle.v1.core.Visitor
import com.rallyhealth.weepickle.v1.WeePickle._
import play.api.libs.json._

import scala.language.implicitConversions
import scala.util.{Failure, Success, Try}
/**
  * Convenience shims back to inefficient play-json formats.
  *
  * weePickle [[Transceiver]] macros are more performant than using play-json Formats.
  * play-json always requires going through an intermediate heavyweight AST:
  * - [[JsValue]] boxes every type. Extra allocations.
  * - [[JsObject]] is full of hash maps which are less efficient than struct-like classes (both cpu and memory).
  */
object PlayJsonImplicits {

  implicit class ReceiverOps[T](val reader: Receiver[T]) extends AnyVal {

    def asReads: Reads[T] = Reads { jsValue =>
      Try(PlayJson.transform(jsValue, reader)) match {
        case Success(obj) => JsSuccess(obj)
        case Failure(t) => JsError(t.toString)
      }
    }
  }

  implicit class TransmitterOps[T](val writer: Transmitter[T]) extends AnyVal {

    def asWrites: Writes[T] = Writes[T] { obj =>
      writer.transmit(obj, PlayJson)
    }
  }

  implicit class ReadsOps[T](val reads: Reads[T]) extends AnyVal {

    def asReceiver: Receiver[T] = PlayJson.JsValueReceiver.map(_.as[T](reads))

    def asReceiverJsResult: Receiver[JsResult[T]] = PlayJson.JsValueReceiver.map(_.validate[T](reads))
  }

  implicit class WritesOps[T](val writes: Writes[T]) extends AnyVal {

    def asTransmitter: Transmitter[T] = new Transmitter[T] {
      override def transmit0[V](in: T, out: Visitor[_, V]): V = {
        val jsValue = writes.writes(in)
        PlayJson.transform(jsValue, out)
      }
    }
  }

  implicit class TransceiverFormat[T](val format: Format[T]) extends AnyVal {

    def asTransceiver: Transceiver[T] = Transceiver.join(format.asReceiver, format.asTransmitter)
  }

  implicit class FormatTransceiver[T](val rw: Transceiver[T]) extends AnyVal {

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

    implicit def playTransmitter[T: Writes]: Transmitter[T] = implicitly[Writes[T]].asTransmitter

    implicit def playReceiver[T: Reads]: Receiver[T] = implicitly[Reads[T]].asReceiver

    implicit def playReceiverJsResult[T: Reads]: Receiver[JsResult[T]] = implicitly[Reads[T]].asReceiverJsResult
  }

  /**
    * Adapts weePickle classes as play-json classes.
    *
    * ==CAREFUL WITH IMPORTS==
    * Do not import this with [[PlayJsonConversions]] or else you may get diverging implicit loops from scalac.
    * For safety, keep these imports scoped as tightly as possible
    */
  object WeePickleConversions {

    implicit def weepickleReads[T: Receiver]: Reads[T] = implicitly[Receiver[T]].asReads

    implicit def weepickleWrites[T: Transmitter]: Writes[T] = implicitly[Transmitter[T]].asWrites
  }

}
