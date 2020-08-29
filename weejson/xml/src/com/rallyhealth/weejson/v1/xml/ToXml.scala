package com.rallyhealth.weejson.v1.xml

import java.io.{OutputStream, Writer}

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.dataformat.xml.ser.ToXmlGenerator
import com.rallyhealth.weejson.v1.jackson.JsonGeneratorOps
import com.rallyhealth.weepickle.v1.core.Visitor
import javax.xml.namespace.QName

object ToXml extends JsonGeneratorOps(DefaultXmlFactory.Instance) {

  override def string: Visitor[Any, String] = super.string

  override def bytes: Visitor[Any, Array[Byte]] = super.bytes

  override def outputStream[OutputStream <: java.io.OutputStream](out: OutputStream): Visitor[Any, OutputStream] =
    super.outputStream(out)

  override def writer[Writer <: java.io.Writer](writer: Writer): Visitor[Any, Writer] = super.writer(writer)

  override def wrapGenerator(g: JsonGenerator): JsonGenerator = g match {
    case xmlGenerator: ToXmlGenerator =>
      xmlGenerator.setNextName(new QName(null, "root"))
      xmlGenerator
    case other => other
  }

}
