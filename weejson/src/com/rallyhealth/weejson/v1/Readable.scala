package com.rallyhealth.weejson.v0

import java.io.InputStream
import java.nio.file.{Files, Path}

import com.rallyhealth.weejson.v0.jackson.{DefaultJsonFactory, WeeJackson}
import com.rallyhealth.weepickle.v0.core.Visitor
import com.rallyhealth.weepickle.v0.geny.ReadableAsBytes

trait Readable {
  def transform[T](f: Visitor[_, T]): T
}

object Readable extends ReadableLowPri{
  case class fromTransformer[T](t: T, w: Transformer[T]) extends Readable{
    def transform[T](f: Visitor[_, T]): T = {
      w.transform(t, f)
    }
  }
  implicit def fromPath(path: Path): Readable = {
    fromInputStream(Files.newInputStream(path))
  }
  implicit def fromFile(s: java.io.File): Readable = fromPath(s.toPath)
  implicit def fromInputStream(in: InputStream): Readable = new Readable {
    override def transform[T](f: Visitor[_, T]): T = {
      WeeJackson.parseSingle(DefaultJsonFactory.Instance.createParser(in), f)
    }
  }
  implicit def fromString(s: String): Readable = fromTransformer(s, StringParser)
  implicit def fromByteArray(s: Array[Byte]): Readable = fromTransformer(s, ByteArrayParser)
}

trait ReadableLowPri{
  implicit def fromReadable[T](s: T)(implicit conv: T => ReadableAsBytes): Readable = new Readable{
    def transform[T](f: Visitor[_, T]): T = conv(s).readBytesThrough { in =>
      WeeJackson.parseSingle(DefaultJsonFactory.Instance.createParser(in), f)
    }
  }
}
