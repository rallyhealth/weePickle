package com.rallyhealth.weejson.v1.yaml

import com.rallyhealth.weejson.v1.jackson.JsonGeneratorOps
import com.rallyhealth.weepickle.v1.core.Visitor

object ToYaml extends JsonGeneratorOps(DefaultYamlFactory.Instance) {

  override def string: Visitor[Any, String] = super.string

  override def bytes: Visitor[Any, Array[Byte]] = super.bytes

  override def outputStream[OutputStream <: java.io.OutputStream](out: OutputStream): Visitor[Any, OutputStream] =
    super.outputStream(out)

  override def writer[Writer <: java.io.Writer](writer: Writer): Visitor[Any, Writer] = super.writer(writer)
}
