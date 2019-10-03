package com.rallyhealth.upack.v1

import com.rallyhealth.weepickle.v1.core.{JsonPointerVisitor, Visitor}

trait Readable {
  def transform[T](v: Visitor[_, T]): T
}

object Readable {
  implicit def fromByteArray(s: Array[Byte]) = new Readable{
    def transform[T](v: Visitor[_, T]): T = new MsgPackReader(0, s).parse(JsonPointerVisitor(v))
  }
}
