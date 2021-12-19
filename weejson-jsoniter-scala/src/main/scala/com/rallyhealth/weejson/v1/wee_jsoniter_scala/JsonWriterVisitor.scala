package com.rallyhealth.weejson.v1.wee_jsoniter_scala

import com.github.plokhotnyuk.jsoniter_scala.core.JsonWriter
import com.rallyhealth.weepickle.v1.core._

import java.time.Instant

class JsonWriterVisitor(
  writer: JsonWriter
) extends JsVisitor[Any, JsonWriter] {

  private[this] val arrVisitor = new ArrVisitor[Any, JsonWriter] {
    override def subVisitor: Visitor[_, _] = JsonWriterVisitor.this

    override def visitValue(
      v: Any
    ): Unit = ()

    override def visitEnd(): JsonWriter = {
      writer.writeArrayEnd()
      writer
    }
  }

  private[this] val objVisitor = new ObjVisitor[Any, JsonWriter] {
    writer.writeObjectStart()
    override def visitKey(): Visitor[_, _] = StringVisitor

    override def visitKeyValue(
      v: Any
    ): Unit = writer.writeKey(v.toString)

    override def subVisitor: Visitor[_, _] = JsonWriterVisitor.this

    override def visitValue(
      v: Any
    ): Unit = ()

    override def visitEnd(): JsonWriter = {
      writer.writeObjectEnd()
      writer
    }
  }

  override def visitArray(
    length: Int
  ): ArrVisitor[Any, JsonWriter] = arrVisitor

  override def visitObject(
    length: Int
  ): ObjVisitor[Any, JsonWriter] = objVisitor

  override def visitNull(): JsonWriter = {
    writer.writeNull()
    writer
  }

  override def visitFalse(): JsonWriter = {
    writer.writeVal(false)
    writer
  }

  override def visitTrue(): JsonWriter = {
    writer.writeVal(true)
    writer
  }

  override def visitFloat64StringParts(
    cs: CharSequence,
    decIndex: Int,
    expIndex: Int
  ): JsonWriter = {
    writer.writeNonEscapedAsciiVal(cs.toString)
    writer
  }

  override def visitFloat64(
    d: Double
  ): JsonWriter = {
    writer.writeVal(d)
    writer
  }

  override def visitFloat32(
    d: Float
  ): JsonWriter = {
    writer.writeVal(d)
    writer
  }

  override def visitInt32(
    i: Int
  ): JsonWriter = {
    writer.writeVal(i)
    writer
  }

  override def visitInt64(
    l: Long
  ): JsonWriter = {
    writer.writeVal(l)
    writer
  }

  override def visitFloat64String(
    s: String
  ): JsonWriter = {
    writer.writeNonEscapedAsciiVal(s)
    writer
  }

  override def visitString(
    cs: CharSequence
  ): JsonWriter = {
    writer.writeVal(cs.toString)
    writer
  }

  override def visitChar(
    c: Char
  ): JsonWriter = {
    writer.writeVal(c)
    writer
  }

  override def visitTimestamp(
    instant: Instant
  ): JsonWriter = {
    writer.writeVal(instant)
    writer
  }

  override def visitBinary(
    bytes: Array[Byte],
    offset: Int,
    len: Int
  ): JsonWriter = {
    val trimmed = if (bytes.length != len) bytes.take(len) else bytes
    writer.writeBase64Val(trimmed, true)
    writer
  }
}
