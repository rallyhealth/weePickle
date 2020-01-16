package com.rallyhealth.weejson.v0.jackson

import com.fasterxml.jackson.core.JsonGenerator
import com.rallyhealth.weepickle.v0.core.{ArrVisitor, JsVisitor, ObjVisitor, SimpleVisitor, Visitor}

/**
  * Adapter to Jackson's [[JsonGenerator]] classes, used to serialize jvm objects to bytes.
  *
  * Allows WeePickle to emit all of jackson's supported serialization formats:
  * - JsonGenerator
  * - YAMLGenerator
  * - SmileGenerator
  * - CBORGenerator
  * - more: https://github.com/FasterXML/jackson#data-format-modules
  */
class JsonGeneratorVisitor(
  generator: JsonGenerator
) extends JsVisitor[Any, JsonGenerator] {

  private val keyVisitor = new SimpleVisitor[Any, JsonGenerator] {
    override def expectedMsg: String = "expected string key"

    override def visitString(
      s: CharSequence,
      index: Int
    ): JsonGenerator = {
      generator.writeFieldName(s.toString)
      generator
    }
  }

  private val arrVisitor = new ArrVisitor[Any, JsonGenerator] {
    override def subVisitor: Visitor[_, _] = JsonGeneratorVisitor.this

    override def visitValue(
      v: Any,
      index: Int
    ): Unit = ()

    override def visitEnd(
      index: Int
    ): JsonGenerator = {
      generator.writeEndArray()
      generator
    }
  }

  override def visitArray(
    length: Int,
    index: Int
  ): ArrVisitor[Any, JsonGenerator] = {
    generator.writeStartArray()
    arrVisitor
  }

  val objVisitor = new ObjVisitor[Any, JsonGenerator] {
    override def visitKey(
      index: Int
    ): Visitor[_, _] = keyVisitor

    override def visitKeyValue(
      v: Any
    ): Unit = ()

    override def subVisitor: Visitor[_, _] = JsonGeneratorVisitor.this

    override def visitValue(
      v: Any,
      index: Int
    ): Unit = ()

    override def visitEnd(
      index: Int
    ): JsonGenerator = {
      generator.writeEndObject()
      generator
    }
  }

  override def visitObject(
    length: Int,
    index: Int
  ): ObjVisitor[Any, JsonGenerator] = {
    generator.writeStartObject()
    objVisitor
  }

  override def visitNull(
    index: Int
  ): JsonGenerator = {
    generator.writeNull()
    generator
  }

  override def visitFalse(
    index: Int
  ): JsonGenerator = {
    generator.writeBoolean(false)
    generator
  }

  override def visitTrue(
    index: Int
  ): JsonGenerator = {
    generator.writeBoolean(true)
    generator
  }

  override def visitInt64(
    l: Long,
    index: Int
  ): JsonGenerator = {
    generator.writeNumber(l)
    generator
  }

  override def visitInt32(
    i: Int,
    index: Int
  ): JsonGenerator = {
    generator.writeNumber(i)
    generator
  }

  override def visitFloat32(
    f: Float,
    index: Int
  ): JsonGenerator = {
    generator.writeNumber(f)
    generator
  }

  override def visitFloat64(
    d: Double,
    index: Int
  ): JsonGenerator = {
    generator.writeNumber(d)
    generator
  }

  override def visitFloat64StringParts(
    s: CharSequence,
    decIndex: Int,
    expIndex: Int,
    index: Int
  ): JsonGenerator = {
    visitFloat64String(s.toString, index)
  }

  override def visitFloat64String(s: String, index: Int): JsonGenerator = {
    if (generator.canWriteFormattedNumbers) {
      generator.writeNumber(s)
    } else {
      generator.writeNumber(new java.math.BigDecimal(s))
    }
    generator
  }

  override def visitString(
    s: CharSequence,
    index: Int
  ): JsonGenerator = {
    generator.writeString(s.toString)
    generator
  }

  override def visitBinary(bytes: Array[Byte], offset: Int, len: Int, index: Int): JsonGenerator = {
    generator.writeBinary(bytes, offset, len)
    generator
  }

  override def visitExt(tag: Byte, bytes: Array[Byte], offset: Int, len: Int, index: Int): JsonGenerator = {
    // Not a standard structure, but no idea what else to do with this.
    // At least reading it back is *possible*.
    generator.writeStartObject()
    generator.writeNumberField("ext", tag.toInt)
    generator.writeFieldName("bytes")
    generator.writeBinary(bytes, offset, len)
    generator.writeEndObject()
    generator
  }
}
