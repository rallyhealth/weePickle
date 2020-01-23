package com.rallyhealth.weejson.v1

import java.io.InputStream
import java.nio.file.{Files, Path}

import com.rallyhealth.weejson.v1.jackson.{FromJson, JsonTransmittable}
import com.rallyhealth.weepickle.v1.core.{Transmittable, Visitor}

trait WeeJsonTransmittable extends Transmittable {
  def transmit[T](f: Visitor[_, T]): T
}

object WeeJsonTransmittable {
  case class fromTransformer[T](t: T, w: Transformer[T]) extends WeeJsonTransmittable{
    def transmit[T](f: Visitor[_, T]): T = {
      w.transform(t, f)
    }
  }
  implicit def fromTransmittable(t: Transmittable): WeeJsonTransmittable = new WeeJsonTransmittable {
    override def transmit[T](f: Visitor[_, T]): T = t.transmit(f)
  }
  implicit def fromPath(path: Path): WeeJsonTransmittable = {
    fromInputStream(Files.newInputStream(path))
  }
  implicit def fromFile(s: java.io.File): WeeJsonTransmittable = fromPath(s.toPath)
  implicit def fromInputStream(in: InputStream): WeeJsonTransmittable = new WeeJsonTransmittable {
    override def transmit[T](f: Visitor[_, T]): T = {
      FromJson(in).transmit(f)
    }
  }
  implicit def fromString(s: String): WeeJsonTransmittable = fromTransformer(s, StringParser)
  implicit def fromByteArray(s: Array[Byte]): WeeJsonTransmittable = fromTransformer(s, ByteArrayParser)
}
