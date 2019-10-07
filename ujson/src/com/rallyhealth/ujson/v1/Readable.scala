package com.rallyhealth.ujson.v1

import java.nio.ByteBuffer
import java.nio.channels.ReadableByteChannel

import com.rallyhealth.weepickle.v1.core.{JsonPointerVisitor, Visitor}

trait Readable {
  def transform[T](f: Visitor[_, T]): T
}

object Readable {
  case class fromTransformer[T](t: T, w: Transformer[T]) extends Readable{
    def transform[T](f: Visitor[_, T]): T = {
      w.transform(t, JsonPointerVisitor(f))
    }
  }
  implicit def fromString(s: String) = fromTransformer(s, StringParser)
  implicit def fromCharSequence(s: CharSequence) = fromTransformer(s, CharSequenceParser)
  implicit def fromChannel(s: ReadableByteChannel) = fromTransformer(s, ChannelParser)
  implicit def fromPath(s: java.nio.file.Path) = fromTransformer(s, PathParser)
  implicit def fromFile(s: java.io.File) = fromTransformer(s, FileParser)
  implicit def fromByteBuffer(s: ByteBuffer) = fromTransformer(s, ByteBufferParser)
  implicit def fromByteArray(s: Array[Byte]) = fromTransformer(s, ByteArrayParser)
}
