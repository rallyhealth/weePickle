package com.rallyhealth.weepack.v0

import com.rallyhealth.weepickle.v0.core.{JsonPointerVisitor, Visitor}

trait Readable {
  def transform[T](v: Visitor[_, T]): T
}

object Readable {
  implicit def fromByteArray(s: Array[Byte]): Readable = new Readable{
    def transform[T](v: Visitor[_, T]): T = new MsgPackReader(0, s).parse(JsonPointerVisitor(v))
  }
}
