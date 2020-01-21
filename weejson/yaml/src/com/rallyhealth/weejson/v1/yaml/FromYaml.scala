package com.rallyhealth.weejson.v1.yaml

import java.io.{InputStream, Reader}

import com.rallyhealth.weejson.v1.jackson.JsonParserOps
import com.rallyhealth.weepickle.v1.core.Transformable

object FromYaml extends JsonParserOps(DefaultYamlFactory.Instance) {

  override def apply(string: String): Transformable = super.apply(string)

  override def apply(bytes: Array[Byte]): Transformable = super.apply(bytes)

  override def apply(in: InputStream): Transformable = super.apply(in)

  override def apply(yaml: Reader): Transformable = super.apply(yaml)
}
