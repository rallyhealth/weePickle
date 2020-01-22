package com.rallyhealth.weejson.v1

import java.io.{ByteArrayOutputStream, OutputStream, StringWriter}

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.json.JsonWriteFeature
import com.rallyhealth.weejson.v1.BaseRenderer.configurePrettyPrinting
import com.rallyhealth.weejson.v1.jackson.DefaultJsonFactory._
import com.rallyhealth.weejson.v1.jackson.{CustomPrettyPrinter, JsonRenderer}
import com.rallyhealth.weepickle.v1.core.Visitor

object BytesRenderer {

  def apply(): Visitor[Any, ExposedByteArrayOutputStream] = {
    apply(new ExposedByteArrayOutputStream)
  }

  def apply[Out <: OutputStream](
    out: Out,
    indent: Int = -1,
    escapeUnicode: Boolean = false
  ): Visitor[Any, Out] = {
    // We'll flush the java.io.Writer, but we won't close it, since we didn't create it.
    // The java.io.Writer is the return value, so the caller can do with it as they please.
    val generator = configurePrettyPrinting(Instance.createGenerator(out).disable(JsonGenerator.Feature.AUTO_CLOSE_TARGET), indent, escapeUnicode)

    JsonRenderer(BaseRenderer.configurePrettyPrinting(generator, indent, escapeUnicode))
      .map(_ => out)
  }

  class ExposedByteArrayOutputStream extends ByteArrayOutputStream {

    /**
      * Returns the internal buffer for performance sensitive cases.
      * Only the first [[size()]] bytes actually contain data.
      */
    def internalBuffer: Array[Byte] = buf
  }

}

object StringRenderer {

  def apply(
    indent: Int = -1,
    escapeUnicode: Boolean = false
  ): Visitor[Any, StringWriter] = BaseRenderer(new java.io.StringWriter(), indent, escapeUnicode)
}

object Renderer {

  def apply[W <: java.io.Writer](
    out: W,
    indent: Int = -1,
    escapeUnicode: Boolean = false
  ): Visitor[Any, W] = BaseRenderer(out, indent, escapeUnicode)
}

object BaseRenderer {

  def apply[T <: java.io.Writer](
    out: T,
    indent: Int = -1,
    escapeUnicode: Boolean = false
  ): Visitor[Any, T] = {
    // We'll flush the java.io.Writer, but we won't close it, since we didn't create it.
    // The java.io.Writer is the return value, so the caller can do with it as they please.
    val generator = configurePrettyPrinting(Instance.createGenerator(out).disable(JsonGenerator.Feature.AUTO_CLOSE_TARGET), indent, escapeUnicode)
    JsonRenderer(generator).map(_ => out)
  }

  def configurePrettyPrinting(
    generator: JsonGenerator,
    indent: Int,
    escapeUnicode: Boolean
  ): JsonGenerator = {
    if (indent != -1) {
      generator.setPrettyPrinter(CustomPrettyPrinter(indent))
    }

    if (escapeUnicode) {
      generator.enable(JsonWriteFeature.ESCAPE_NON_ASCII.mappedFeature())
    }
    generator
  }

}
