package com.rallyhealth.weejson.v1.jackson

import java.io.{ByteArrayOutputStream, OutputStream, StringWriter, Writer}

import com.fasterxml.jackson.core.{JsonFactory, JsonGenerator}
import com.rallyhealth.weepickle.v1.core.Visitor

object ToJson extends JsonGeneratorOps {

  override def string: Visitor[Any, String] = super.string

  override def bytes: Visitor[Any, Array[Byte]] = super.bytes

  override def outputStream[OutputStream <: java.io.OutputStream](out: OutputStream): Visitor[Any, OutputStream] =
    super.outputStream(out)

  override def writer[Writer <: java.io.Writer](writer: Writer): Visitor[Any, Writer] = super.writer(writer)
}

object ToPrettyJson extends JsonGeneratorOps {

  override protected def wrapGenerator(g: JsonGenerator): JsonGenerator = g.setPrettyPrinter(CustomPrettyPrinter(2))

  override def string: Visitor[Any, String] = super.string

  override def bytes: Visitor[Any, Array[Byte]] = super.bytes

  override def outputStream[OutputStream <: java.io.OutputStream](out: OutputStream): Visitor[Any, OutputStream] =
    super.outputStream(out)

  override def writer[Writer <: java.io.Writer](writer: Writer): Visitor[Any, Writer] = super.writer(writer)
}

abstract class JsonGeneratorOps(
  factory: JsonFactory = DefaultJsonFactory.Instance
) {

  def string: Visitor[Any, String] = {
    val writer = new StringWriter()
    JsonRenderer(wrap(writer)).map(_ => writer.toString)
  }

  def bytes: Visitor[Any, Array[Byte]] = {
    outputStream(new ByteArrayOutputStream()).map(_.toByteArray)
  }

  def outputStream[OutputStream <: java.io.OutputStream](out: OutputStream): Visitor[Any, OutputStream] = {
    JsonRenderer(noClose(wrap(out))).map(_ => out)
  }

  def writer[Writer <: java.io.Writer](writer: Writer): Visitor[Any, Writer] = {
    JsonRenderer(noClose(wrap(writer))).map(_ => writer)
  }

  private def noClose(jsonGenerator: JsonGenerator): JsonGenerator = {
    // If we didn't create it, it's not ours to close().
    jsonGenerator.disable(JsonGenerator.Feature.AUTO_CLOSE_TARGET)
  }

  // Overridable hooks
  protected def wrapGenerator(g: JsonGenerator): JsonGenerator = g

  protected def wrap(w: Writer): JsonGenerator = wrapGenerator(factory.createGenerator(w))

  protected def wrap(o: OutputStream): JsonGenerator = wrapGenerator(factory.createGenerator(o))
}
