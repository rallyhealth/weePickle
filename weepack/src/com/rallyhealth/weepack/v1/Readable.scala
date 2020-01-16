package com.rallyhealth.weepack.v1

import com.rallyhealth.weepickle.v1.core.{BufferingInputStreamParser, Visitor}
import com.rallyhealth.weepickle.v1.geny.ReadableAsBytes

trait Readable {
  def transform[T](f: Visitor[_, T]): T
}

object Readable {
  implicit def fromByteArray(s: Array[Byte]): Readable = new Readable{
    def transform[T](f: Visitor[_, T]): T = new MsgPackReader(0, s).parse(f)
  }
  implicit def fromReadable(s: ReadableAsBytes): Readable = new Readable{
    def transform[T](f: Visitor[_, T]): T = {
      s.readBytesThrough(
        new InputStreamMsgPackReader(
          _,
          BufferingInputStreamParser.defaultMinBufferStartSize,
          BufferingInputStreamParser.defaultMaxBufferStartSize
        ).parse(f)
      )
    }
  }
}
