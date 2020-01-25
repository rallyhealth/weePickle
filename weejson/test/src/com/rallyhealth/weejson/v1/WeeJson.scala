package com.rallyhealth.weejson.v1

import com.rallyhealth.weepickle.v1.core.NoOpVisitor

object WeeJson {
  def transform[T](r: WeeJsonFromData, v: com.rallyhealth.weepickle.v1.core.Visitor[_, T]): T = r.transform(v)

  /**
    * Read the given JSON input as a JSON struct
    */
  def read(s: WeeJsonFromData): Value.Value = transform(s, Value)

  def copy(t: Value.Value): Value.Value = transform(t, Value)

  /**
    * Write the given JSON struct as a JSON String
    */
  def write(t: Value.Value, indent: Int = -1, escapeUnicode: Boolean = false): String = {
    transform(t, StringRenderer(indent, escapeUnicode)).toString
  }

  /**
    * Write the given JSON struct as a JSON String to the given From
    */
  def writeTo(t: Value.Value, out: java.io.Writer, indent: Int = -1, escapeUnicode: Boolean = false): Unit = {
    transform(t, Renderer(out, indent, escapeUnicode))
  }

  /**
    * Parse the given JSON input, failing if it is invalid
    */
  def validate(s: WeeJsonFromData): Unit = transform(s, NoOpVisitor)

  /**
    * Parse the given JSON input and write it to a string with
    * the configured formatting
    */
  def reformat(s: WeeJsonFromData, indent: Int = -1, escapeUnicode: Boolean = false): String = {
    transform(s, StringRenderer(indent, escapeUnicode)).toString
  }

  /**
    * Parse the given JSON input and write it to a string with
    * the configured formatting to the given From
    */
  def reformatTo(
    s: WeeJsonFromData,
    out: java.io.Writer,
    indent: Int = -1,
    escapeUnicode: Boolean = false
  ): Unit = {
    transform(s, Renderer(out, indent, escapeUnicode)).toString
  }
  // End com.rallyhealth.weejson.v1
}
