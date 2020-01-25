package com.rallyhealth.weejson.v1.yaml

import java.io.{File, InputStream, Reader}
import java.nio.file.Path

import com.rallyhealth.weejson.v1.jackson.JsonParserOps
import com.rallyhealth.weepickle.v1.core.FromData

object FromYaml extends JsonParserOps(DefaultYamlFactory.Instance) {

  override def apply(string: String): FromData = super.apply(string)

  override def apply(bytes: Array[Byte]): FromData = super.apply(bytes)

  override def apply(in: InputStream): FromData = super.apply(in)

  override def apply(reader: Reader): FromData = super.apply(reader)

  override def apply(file: File): FromData = super.apply(file)

  override def apply(path: Path): FromData = super.apply(path)
}
