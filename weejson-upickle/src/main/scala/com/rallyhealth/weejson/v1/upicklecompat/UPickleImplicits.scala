package com.rallyhealth.weejson.v1.upicklecompat

import com.rallyhealth.weepickle.v1.WeePickle
import com.rallyhealth.weepickle.v1.core.Visitor
import upickle.core

object UPickleImplicits {

  implicit class UpickleReader[J](val reader: upickle.default.Reader[J]) extends AnyVal {

    def asTo: WeePickle.To[J] = new WeePickle.To.Delegate[Any, J](new UWVisitor[Any, J](reader))
  }

  implicit class UPickleWriter[T](val writer: upickle.default.Writer[T]) extends AnyVal {

    def asFrom: WeePickle.From[T] = new WeePickle.From[T] {
      override def transform0[Out](in: T, out: Visitor[_, Out]): Out = writer.transform(in, new WUVisitor[Nothing, Out](out))
    }
  }

  implicit class UPickleReadWriter[T](val rw: upickle.default.ReadWriter[T]) extends AnyVal {

    def asFromTo: WeePickle.FromTo[T] = WeePickle.FromTo.join(rw.asTo, rw.asFrom)
  }

  implicit class WeePickleFrom[T](val from: WeePickle.From[T]) extends AnyVal {

    def asWriter: upickle.default.Writer[T] = new upickle.default.Writer[T] {
      override def write0[V](out: core.Visitor[_, V], v: T): V = from.transform(v, new UWVisitor[Nothing, V](out))
    }
  }

  implicit class WeePickleTo[T](val to: WeePickle.To[T]) extends AnyVal {

    def asReader: upickle.default.Reader[T] = new upickle.default.Reader.Delegate[Any, T](new WUVisitor[Any, T](to))
  }

  implicit class WeePickleFromTo[T](val fromTo: WeePickle.FromTo[T]) extends AnyVal {

    def asReadWriter: upickle.default.ReadWriter[T] = upickle.default.ReadWriter.join(fromTo.asReader, fromTo.asWriter)
  }
}
