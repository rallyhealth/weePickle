package com.rallyhealth.weejson.v1

import java.io.InputStream
import java.nio.file.{Files, Path}

import com.rallyhealth.weejson.v1.jackson.{DefaultJsonFactory, WeeJackson}
import com.rallyhealth.weepickle.v1.core.{Transformable, Visitor}

trait JsonTransformable extends Transformable {
  def transform[T](f: Visitor[_, T]): T
}

object JsonTransformable {
  case class fromTransformer[T](t: T, w: Transformer[T]) extends JsonTransformable{
    def transform[T](f: Visitor[_, T]): T = {
      w.transform(t, f)
    }
  }
  implicit def fromTransformable(t: Transformable): JsonTransformable = new JsonTransformable {
    override def transform[T](f: Visitor[_, T]): T = t.transform(f)
  }
  implicit def fromPath(path: Path): JsonTransformable = {
    fromInputStream(Files.newInputStream(path))
  }
  implicit def fromFile(s: java.io.File): JsonTransformable = fromPath(s.toPath)
  implicit def fromInputStream(in: InputStream): JsonTransformable = new JsonTransformable {
    override def transform[T](f: Visitor[_, T]): T = {
      WeeJackson.parseSingle(DefaultJsonFactory.Instance.createParser(in), f)
    }
  }
  implicit def fromString(s: String): JsonTransformable = fromTransformer(s, StringParser)
  implicit def fromByteArray(s: Array[Byte]): JsonTransformable = fromTransformer(s, ByteArrayParser)
}
