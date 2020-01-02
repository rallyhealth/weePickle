package com.rallyhealth.weejson.v0

import java.nio.ByteBuffer

import com.rallyhealth.weepickle.v0.core.{JsonPointerVisitor, Visitor}
import com.rallyhealth.weepickle.v0.geny.ReadableAsBytes
import ujson.InputStreamParser

trait Readable {
  def transform[T](f: Visitor[_, T]): T
}

object Readable extends ReadableLowPri{
  case class fromTransformer[T](t: T, w: Transformer[T]) extends Readable{
    def transform[T](f: Visitor[_, T]): T = {
      w.transform(t, JsonPointerVisitor(f))
    }
  }
  implicit def fromString(s: String): Readable = fromTransformer(s, StringParser)
  implicit def fromCharSequence(s: CharSequence): Readable = fromTransformer(s, CharSequenceParser)
  implicit def fromPath(s: java.nio.file.Path): Readable = fromTransformer(
    java.nio.file.Files.newInputStream(s),
    InputStreamParser
  )
  implicit def fromFile(s: java.io.File): Readable = fromTransformer(
    java.nio.file.Files.newInputStream(s.toPath),
    InputStreamParser
  )
  implicit def fromByteBuffer(s: ByteBuffer): Readable = fromTransformer(s, ByteBufferParser)
  implicit def fromByteArray(s: Array[Byte]): Readable = fromTransformer(s, ByteArrayParser)
}

trait ReadableLowPri{
  implicit def fromReadable[T](s: T)(implicit conv: T => ReadableAsBytes): Readable = new Readable{
    def transform[T](f: Visitor[_, T]): T = conv(s).readBytesThrough(InputStreamParser.transform(_, f))
  }
}
