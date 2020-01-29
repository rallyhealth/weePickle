package com.rallyhealth.weejson.v1

object WeeJson {

  def transform[T](r: WeeJsonFromInput, v: com.rallyhealth.weepickle.v1.core.Visitor[_, T]): T = r.transform(v)

  /**
    * Read the given JSON input as a JSON struct
    */
  def read(s: WeeJsonFromInput): Value.Value = transform(s, Value)

  def copy(t: Value.Value): Value.Value = transform(t, Value)

  /**
    * Write the given JSON struct as a JSON String
    */
  def write(t: Value.Value, indent: Int = -1, escapeUnicode: Boolean = false): String = {
    transform(t, StringRenderer(indent, escapeUnicode)).toString
  }

  // End com.rallyhealth.weejson.v1
}
