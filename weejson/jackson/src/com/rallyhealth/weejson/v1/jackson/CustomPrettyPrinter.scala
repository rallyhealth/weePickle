package com.rallyhealth.weejson.v1.jackson

import com.fasterxml.jackson.core.{JsonGenerator, PrettyPrinter}
import com.fasterxml.jackson.core.util.{DefaultIndenter, DefaultPrettyPrinter}

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
