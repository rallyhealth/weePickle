package com.rallyhealth.weejson.v1.yaml

import java.io.{File, InputStream, Reader}
import java.nio.file.Path

import com.rallyhealth.weejson.v1.jackson.JsonParserOps
import com.rallyhealth.weepickle.v1.core.Transmittable

object FromYaml extends JsonParserOps(DefaultYamlFactory.Instance) {

  override def apply(string: String): Transmittable = super.apply(string)

  override def apply(bytes: Array[Byte]): Transmittable = super.apply(bytes)

  override def apply(in: InputStream): Transmittable = super.apply(in)

  override def apply(reader: Reader): Transmittable = super.apply(reader)

  override def apply(file: File): Transmittable = super.apply(file)

  override def apply(path: Path): Transmittable = super.apply(path)
}
