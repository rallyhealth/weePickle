package com.rallyhealth.weejson.v1.play

import com.rallyhealth.weepickle.v1.core.Visitor
import com.rallyhealth.weepickle.v1.WeePickle._
import play.api.libs.json._

import scala.language.implicitConversions
import scala.util.{Failure, Success, Try}
/**
  * Convenience shims back to inefficient play-json formats.
  *
  * weePickle [[ReaderWriter]] macros are more performant than using play-json Formats.
  * play-json always requires going through an intermediate heavyweight AST:
  * - [[JsValue]] boxes every type. Extra allocations.
  * - [[JsObject]] is full of hash maps which are less efficient than struct-like classes (both cpu and memory).
  */
object PlayJsonImplicits {

  implicit class ReaderOps[T](val reader: Reader[T]) extends AnyVal {

    def asReads: Reads[T] = Reads { jsValue =>
      Try(PlayJson.transform(jsValue, reader)) match {
        case Success(obj) => JsSuccess(obj)
        case Failure(t) => JsError(t.toString)
      }
    }
  }

  implicit class WriterOps[T](val writer: Writer[T]) extends AnyVal {

    def asWrites: Writes[T] = Writes[T] { obj =>
      writer.write(PlayJson, obj)
    }
  }

  implicit class ReadsOps[T](val reads: Reads[T]) extends AnyVal {

    def asReader: Reader[T] = PlayJson.JsValueReader.map(_.as[T](reads))

    def asReaderJsResult: Reader[JsResult[T]] = PlayJson.JsValueReader.map(_.validate[T](reads))
  }

  implicit class WritesOps[T](val writes: Writes[T]) extends AnyVal {

    def asWriter: Writer[T] = new Writer[T] {
      override def write0[V](out: Visitor[_, V], in: T): V = {
        val jsValue = writes.writes(in)
        PlayJson.transform(jsValue, out)
      }
    }
  }

  implicit class ReaderWriterFormat[T](val format: Format[T]) extends AnyVal {

    def asReaderWriter: ReaderWriter[T] = ReaderWriter.join(format.asReader, format.asWriter)
  }

  implicit class FormatReaderWriter[T](val rw: ReaderWriter[T]) extends AnyVal {

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

    implicit def playWriter[T: Writes]: Writer[T] = implicitly[Writes[T]].asWriter

    implicit def playReader[T: Reads]: Reader[T] = implicitly[Reads[T]].asReader

    implicit def playReaderJsResult[T: Reads]: Reader[JsResult[T]] = implicitly[Reads[T]].asReaderJsResult
  }

  /**
    * Adapts weePickle classes as play-json classes.
    *
    * ==CAREFUL WITH IMPORTS==
    * Do not import this with [[PlayJsonConversions]] or else you may get diverging implicit loops from scalac.
    * For safety, keep these imports scoped as tightly as possible
    */
  object WeePickleConversions {

    implicit def weepickleReads[T: Reader]: Reads[T] = implicitly[Reader[T]].asReads

    implicit def weepickleWrites[T: Writer]: Writes[T] = implicitly[Writer[T]].asWrites
  }

}
