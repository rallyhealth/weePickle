package com.rallyhealth.weejson.v1

import java.io.InputStream
import java.nio.file.{Files, Path}

import com.rallyhealth.weejson.v1.jackson.{FromJson, JsonTransformable}
import com.rallyhealth.weepickle.v1.core.{Transformable, Visitor}

trait WeeJsonTransformable extends Transformable {
  def transform[T](f: Visitor[_, T]): T
}

object WeeJsonTransformable {
  case class fromTransformer[T](t: T, w: Transformer[T]) extends WeeJsonTransformable{
    def transform[T](f: Visitor[_, T]): T = {
      w.transform(t, f)
    }
  }
  implicit def fromTransformable(t: Transformable): WeeJsonTransformable = new WeeJsonTransformable {
    override def transform[T](f: Visitor[_, T]): T = t.transform(f)
  }
  implicit def fromPath(path: Path): WeeJsonTransformable = {
    fromInputStream(Files.newInputStream(path))
  }
  implicit def fromFile(s: java.io.File): WeeJsonTransformable = fromPath(s.toPath)
  implicit def fromInputStream(in: InputStream): WeeJsonTransformable = new WeeJsonTransformable {
    override def transform[T](f: Visitor[_, T]): T = {
      FromJson(in).transform(f)
    }
  }
  implicit def fromString(s: String): WeeJsonTransformable = fromTransformer(s, StringParser)
  implicit def fromByteArray(s: Array[Byte]): WeeJsonTransformable = fromTransformer(s, ByteArrayParser)
}
