package com.rallyhealth.upack.v1

import com.rallyhealth.upickle.v1.core.Visitor

trait Readable {
  def transform[T](f: Visitor[_, T]): T
}

object Readable {
  implicit def fromByteArray(s: Array[Byte]) = new Readable{
    def transform[T](f: Visitor[_, T]): T = new MsgPackReader(0, s).parse(f)
  }
}
