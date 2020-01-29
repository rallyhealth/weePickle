package com.rallyhealth.weejson.v1.yaml

import java.io.{File, InputStream, Reader}
import java.nio.file.Path

import com.rallyhealth.weejson.v1.jackson.JsonParserOps
import com.rallyhealth.weepickle.v1.core.FromInput

object FromYaml extends JsonParserOps(DefaultYamlFactory.Instance) {

  override def apply(string: String): FromInput = super.apply(string)

  override def apply(bytes: Array[Byte]): FromInput = super.apply(bytes)

  override def apply(in: InputStream): FromInput = super.apply(in)

  override def apply(reader: Reader): FromInput = super.apply(reader)

  override def apply(file: File): FromInput = super.apply(file)

  override def apply(path: Path): FromInput = super.apply(path)
}
