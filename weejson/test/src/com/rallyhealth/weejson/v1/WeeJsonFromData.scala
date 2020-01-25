package com.rallyhealth.weejson.v1

import java.io.InputStream
import java.nio.file.{Files, Path}

import com.rallyhealth.weejson.v1.jackson.{FromJson, JsonFromData}
import com.rallyhealth.weepickle.v1.core.{FromData, Visitor}

trait WeeJsonFromData extends FromData {
  def transform[T](to: Visitor[_, T]): T
}

object WeeJsonFromData {
  case class fromTransformer[T](t: T, w: Transformer[T]) extends WeeJsonFromData {
    def transform[T](to: Visitor[_, T]): T = {
      w.transform(t, to)
    }
  }
  implicit def fromTransmittable(t: FromData): WeeJsonFromData = new WeeJsonFromData {
    override def transform[T](to: Visitor[_, T]): T = t.transform(to)
  }
  implicit def fromPath(path: Path): WeeJsonFromData = {
    fromInputStream(Files.newInputStream(path))
  }
  implicit def fromFile(s: java.io.File): WeeJsonFromData = fromPath(s.toPath)
  implicit def fromInputStream(in: InputStream): WeeJsonFromData = new WeeJsonFromData {
    override def transform[T](to: Visitor[_, T]): T = {
      FromJson(in).transform(to)
    }
  }
  implicit def fromString(s: String): WeeJsonFromData = fromTransformer(s, StringParser)
  implicit def fromByteArray(s: Array[Byte]): WeeJsonFromData = fromTransformer(s, ByteArrayParser)
}
