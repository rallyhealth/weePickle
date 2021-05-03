package com.rallyhealth.weejson.v1

import java.io.StringWriter

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.json.JsonWriteFeature
import com.rallyhealth.weejson.v1.jackson.DefaultJsonFactory.Instance
import com.rallyhealth.weejson.v1.jackson.{CustomPrettyPrinter, JsonRenderer}
import com.rallyhealth.weepickle.v1.core.Visitor

object StringRenderer {

  def apply(
    indent: Int = -1,
    escapeUnicode: Boolean = false
  ): Visitor[Any, StringWriter] = BaseRenderer(new java.io.StringWriter(), indent, escapeUnicode)
}

object BaseRenderer {

  def apply[T <: java.io.Writer](
    out: T,
    indent: Int = -1,
    escapeUnicode: Boolean = false
  ): Visitor[Any, T] = {
    // We'll flush the java.io.Writer, but we won't close it, since we didn't create it.
    // The java.io.Writer is the return value, so the caller can do with it as they please.
    val generator = configurePrettyPrinting(
      Instance.createGenerator(out).disable(JsonGenerator.Feature.AUTO_CLOSE_TARGET),
      indent,
      escapeUnicode
    )
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

