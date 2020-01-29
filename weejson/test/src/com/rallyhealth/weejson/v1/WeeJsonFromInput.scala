package com.rallyhealth.weejson.v1

import java.io.InputStream
import java.nio.file.{Files, Path}

import com.rallyhealth.weejson.v1.jackson.FromJson
import com.rallyhealth.weepickle.v1.core.{FromInput, Visitor}

trait WeeJsonFromInput extends FromInput {
  def transform[T](to: Visitor[_, T]): T
}

object WeeJsonFromInput {
  case class fromTransformer[T](t: T, w: Transformer[T]) extends WeeJsonFromInput {
    def transform[T](to: Visitor[_, T]): T = {
      w.transform(t, to)
    }
  }
  implicit def fromInput(t: FromInput): WeeJsonFromInput = new WeeJsonFromInput {
    override def transform[T](to: Visitor[_, T]): T = t.transform(to)
  }
  implicit def fromPath(path: Path): WeeJsonFromInput = {
    fromInputStream(Files.newInputStream(path))
  }
  implicit def fromFile(s: java.io.File): WeeJsonFromInput = fromPath(s.toPath)
  implicit def fromInputStream(in: InputStream): WeeJsonFromInput = fromInput(FromJson(in))
  implicit def fromString(s: String): WeeJsonFromInput = fromInput(FromJson(s))
  implicit def fromByteArray(s: Array[Byte]): WeeJsonFromInput = fromInput(FromJson(s))
}
