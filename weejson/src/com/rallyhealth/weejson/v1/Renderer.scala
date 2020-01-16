package com.rallyhealth.weejson.v0

import java.io.{ByteArrayOutputStream, StringWriter}

import com.fasterxml.jackson.core.json.JsonWriteFeature
import com.fasterxml.jackson.core.util.{DefaultIndenter, DefaultPrettyPrinter}
import com.fasterxml.jackson.core.{JsonFactory, JsonGenerator, PrettyPrinter}
import com.rallyhealth.weejson.v0.jackson.{DefaultJsonFactory, WeeJackson}
import com.rallyhealth.weepickle.v0.core.Visitor

object BytesRenderer {

  def apply(): Visitor[_, BytesWriter] = Renderer(new BytesWriter)

  class BytesWriter(out: java.io.ByteArrayOutputStream = new ByteArrayOutputStream())
    extends java.io.OutputStreamWriter(out) {

    def toBytes = {
      this.flush()
      out.toByteArray
    }
  }

}

object StringRenderer {

  def apply(
    indent: Int = -1,
    escapeUnicode: Boolean = false
  ): Visitor[_, StringWriter] = BaseRenderer(new java.io.StringWriter(), indent, escapeUnicode)
}

object Renderer {

  def apply[W <: java.io.Writer](
    out: W,
    indent: Int = -1,
    escapeUnicode: Boolean = false
  ): Visitor[_, W] = BaseRenderer(out, indent, escapeUnicode)
}

object BaseRenderer {

  def apply[T <: java.io.Writer](
    out: T,
    indent: Int = -1,
    escapeUnicode: Boolean = false
  ): Visitor[_, T] = {
    val generator = DefaultJsonFactory.Instance.createGenerator(out)

    if (indent != -1) {
      generator.setPrettyPrinter(CustomPrettyPrinter(indent))
    }

    if (escapeUnicode) {
      generator.enable(JsonWriteFeature.ESCAPE_NON_ASCII.mappedFeature())
    }

    WeeJackson.toGenerator(generator)
      .map { gen =>
        gen.flush()
        out
      }
  }

  object CustomPrettyPrinter {

    class FieldSepPrettyPrinter(base: DefaultPrettyPrinter) extends DefaultPrettyPrinter(base) {

      override def writeObjectFieldValueSeparator(g: JsonGenerator): Unit = {
        // https://stackoverflow.com/a/56159005
        g.writeRaw(": ")
      }
    }

    def apply(indent: Int): PrettyPrinter = {
      val indenter = new DefaultIndenter(" " * indent, DefaultIndenter.SYS_LF)

      new FieldSepPrettyPrinter(
        new DefaultPrettyPrinter()
          .withObjectIndenter(indenter)
          .withArrayIndenter(indenter)
      )
    }
  }

}
