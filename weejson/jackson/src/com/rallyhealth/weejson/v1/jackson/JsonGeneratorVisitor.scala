package com.rallyhealth.weejson.v1.jackson

import java.time.Instant
import java.util.Base64

import com.fasterxml.jackson.core.JsonGenerator
import com.rallyhealth.weepickle.v1.core._

/**
  * See [[JsonRenderer]] for high level use.
  *
  * Adapter to Jackson's [[JsonGenerator]] classes, used to serialize jvm objects to bytes.
  *
  * Allows WeePickle to emit all of jackson's supported serialization formats:
  * - JsonGenerator
  * - YAMLGenerator
  * - SmileGenerator
  * - CBORGenerator
  * - more: https://github.com/FasterXML/jackson#data-format-modules
  *
  * @note Important for perf to call close().
  *       Otherwise jackson will not return its buffer to the pool for reuse.
  */
class JsonGeneratorVisitor(
  generator: JsonGenerator
) extends JsVisitor[Any, JsonGenerator] {

  private val keyVisitor = new SimpleVisitor[Any, JsonGenerator] {
    override def expectedMsg: String = "expected string key"

    override def visitString(cs: CharSequence): JsonGenerator = {
      generator.writeFieldName(cs.toString)
      generator
    }

    override def visitInt32(i: Int): JsonGenerator = {
      generator.writeFieldId(i)
      generator
    }

    override def visitInt64(l: Long): JsonGenerator = {
      generator.writeFieldId(l)
      generator
    }

    override def visitFloat64StringParts(
      cs: CharSequence,
      decIndex: Int,
      expIndex: Int
    ): JsonGenerator = {
      generator.writeFieldName(cs.toString)
      generator
    }

    override def visitTrue(): JsonGenerator = {
      generator.writeFieldName("true")
      generator
    }

    override def visitFalse(): JsonGenerator = {
      generator.writeFieldName("false")
      generator
    }

    override def visitFloat64(d: Double): JsonGenerator = {
      generator.writeFieldName(d.toString)
      generator
    }

    override def visitFloat32(d: Float): JsonGenerator = {
      generator.writeFieldName(d.toString)
      generator
    }

    override def visitUInt64(ul: Long): JsonGenerator = {
      if (ul < 0) {
        generator.writeFieldName(java.lang.Long.toUnsignedString(ul))
      } else {
        generator.writeFieldId(ul)
      }
      generator
    }

    override def visitFloat64String(s: String): JsonGenerator = {
      generator.writeFieldName(s)
      generator
    }

    override def visitTimestamp(instant: Instant): JsonGenerator = {
      generator.writeFieldName(instant.toString)
      generator
    }

    override def visitChar(c: Char): JsonGenerator = {
      generator.writeFieldName(c.toString)
      generator
    }

    override def visitBinary(
      bytes: Array[Byte],
      offset: Int,
      len: Int
    ): JsonGenerator = {
      val arr = if (offset > 0 || len < bytes.length) {
        bytes.slice(offset, offset + len)
      } else {
        bytes
      }
      val base64 = Base64.getEncoder.encodeToString(arr)
      generator.writeFieldName(base64)
      generator
    }
  }

  private val arrVisitor = new ArrVisitor[Any, JsonGenerator] {
    override def subVisitor: Visitor[_, _] = JsonGeneratorVisitor.this

    override def visitValue(v: Any): Unit = ()

    override def visitEnd(): JsonGenerator = {
      generator.writeEndArray()
      generator
    }
  }

  override def visitArray(length: Int): ArrVisitor[Any, JsonGenerator] = {
    generator.writeStartArray()
    arrVisitor
  }

  val objVisitor = new ObjVisitor[Any, JsonGenerator] {
    override def visitKey(): Visitor[_, _] = keyVisitor

    override def visitKeyValue(
      v: Any
    ): Unit = ()

    override def subVisitor: Visitor[_, _] = JsonGeneratorVisitor.this

    override def visitValue(v: Any): Unit = ()

    override def visitEnd(): JsonGenerator = {
      generator.writeEndObject()
      generator
    }
  }

  override def visitObject(length: Int): ObjVisitor[Any, JsonGenerator] = {
    generator.writeStartObject()
    objVisitor
  }

  override def visitNull(): JsonGenerator = {
    generator.writeNull()
    generator
  }

  override def visitFalse(): JsonGenerator = {
    generator.writeBoolean(false)
    generator
  }

  override def visitTrue(): JsonGenerator = {
    generator.writeBoolean(true)
    generator
  }

  override def visitInt64(l: Long): JsonGenerator = {
    generator.writeNumber(l)
    generator
  }

  override def visitInt32(i: Int): JsonGenerator = {
    generator.writeNumber(i)
    generator
  }

  override def visitFloat32(f: Float): JsonGenerator = {
    generator.writeNumber(f)
    generator
  }

  override def visitFloat64(d: Double): JsonGenerator = {
    generator.writeNumber(d)
    generator
  }

  override def visitFloat64StringParts(cs: CharSequence, decIndex: Int, expIndex: Int): JsonGenerator = {
    visitFloat64String(cs.toString)
  }

  override def visitFloat64String(s: String): JsonGenerator = {
    if (generator.canWriteFormattedNumbers) {
      generator.writeNumber(s)
    } else {
      generator.writeNumber(new java.math.BigDecimal(s))
    }
    generator
  }

  override def visitString(cs: CharSequence): JsonGenerator = {
    generator.writeString(cs.toString)
    generator
  }

  override def visitBinary(bytes: Array[Byte], offset: Int, len: Int): JsonGenerator = {
    generator.writeBinary(bytes, offset, len)
    generator
  }

  override def visitExt(tag: Byte, bytes: Array[Byte], offset: Int, len: Int): JsonGenerator = {
    // Not a standard structure, but no idea what else to do with this.
    // At least reading it back is *possible*.
    generator.writeStartObject()
    generator.writeNumberField("ext", tag.toInt)
    generator.writeFieldName("bytes")
    generator.writeBinary(bytes, offset, len)
    generator.writeEndObject()
    generator
  }

  override def close(): Unit = {
    if (!generator.isClosed) {
      generator.close()
    }
  }
}
