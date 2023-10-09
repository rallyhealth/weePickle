package com.rallyhealth.weejson.v1.jackson

import java.io.InputStream
import java.math.BigInteger

import com.fasterxml.jackson.core._
import com.rallyhealth.weepickle.v1.core._

/**
  * Implements most of the `JsonGenerator` interface.
  *
  * `CharSequence`s emitted to the visitor are typically thin wrappers around
  * jackson's internal mutable buffer. Values are stable until the visitor methods return.
  * It is the visitor's responsibility to call toString if immutability is needed.
  *
  * @param rootVisitor underlying sink of json events
  * @param objectCodec used for writeObject(), which will probably never be called.
  * @tparam J visitor return value
  */
class VisitorJsonGenerator[J](
  rootVisitor: Visitor[_, J],
  private var objectCodec: ObjectCodec
) extends JsonGenerator {

  def this(visitor: Visitor[_, J]) = this(visitor, DefaultJsonFactory.Instance.getCodec)

  private val cs = new TextBufferCharSequence(Array.emptyCharArray, 0, 0)

  /*
   * This stack used to be implemented as an ArrayStack. Scala 2.13 deprecated ArrayStack with the message "Use Stack
   * instead of ArrayStack; it now uses an array-based implementation". However, Scala 2.12 deprecated Stack with the
   * message "Stack is an inelegant and potentially poorly-performing wrapper around List. Use a List assigned to a var
   * instead." To avoid these dueling deprecations in the cross-version build, we are using a List now (i.e., a mutable
   * reference to an immutable data structure instead of an immutable reference to a mutable data structure), which may
   * slightly increase the memory allocation rate for all but the smallest messages. This is unlikely to cause any
   * noticeable performance regression, but at least it is private, so if such a regression is identified later, it can
   * be addressed without breaking binary compatibility.
   */
  private var stack: List[ObjArrVisitor[Any, _]] = Nil

  // Manually managing this reference is faster than calling stack.head every time.
  private var ctxt: ObjArrVisitor[Any, _] = {
    // Create a dummy root stack element that returns the rootVisitor.
    // This is simpler than special-casing the rootVisitor.
    new ArrVisitor[Any, J] {
      override def subVisitor: Visitor[Nothing, J] = rootVisitor

      override def visitValue(v: Any): Unit = ()

      override def visitEnd(): Nothing = {
        throw new IllegalStateException("programming error: illegal call to dummy ArrVisitor")
      }
    }
  }

  protected def facade: Visitor[_, _] = top.subVisitor

  protected def top: ObjArrVisitor[Any, _] = ctxt

  protected def push(e: ObjArrVisitor[Any, _]): Unit = {
    stack = top :: stack
    ctxt = e
  }
  protected def pop(): ObjArrVisitor[Any, _] = {
    val ret = top
    ctxt = stack.head
    stack = stack.tail
    ret
  }

  private def charSequence(buf: Array[Char], off: Int, len: Int): CharSequence = {
    cs.buf = buf
    cs.off = off
    cs.len = len
    cs
  }

  override def setCodec(oc: ObjectCodec): JsonGenerator = {
    objectCodec = oc
    this
  }

  override def getCodec: ObjectCodec = objectCodec

  override def version(): Version = Version.unknownVersion()

  override def enable(f: JsonGenerator.Feature): JsonGenerator = this

  override def disable(f: JsonGenerator.Feature): JsonGenerator = this

  override def isEnabled(f: JsonGenerator.Feature): Boolean = false

  override def getFeatureMask: Int = 0

  override def setFeatureMask(values: Int): JsonGenerator = this

  override def useDefaultPrettyPrinter(): JsonGenerator = this

  override def writeStartArray(): Unit = push(facade.visitArray(-1).narrow)

  override def writeStartObject(): Unit = push(facade.visitObject(-1).narrow)

  override def writeEndObject(): Unit = writeEndObjArr()

  override def writeEndArray(): Unit = writeEndObjArr()

  private def writeEndObjArr(): Unit = visitValue(pop().visitEnd())

  override def writeFieldName(name: String): Unit = {
    val objVisitor = top.asInstanceOf[ObjVisitor[Any, _]]
    objVisitor.visitKeyValue(objVisitor.visitKey().visitString(name))
  }

  override def writeFieldName(name: SerializableString): Unit = writeFieldName(name.getValue)

  override def writeString(text: String): Unit = {
    visitValue(facade.visitString(text))
  }

  override def writeString(buf: Array[Char], off: Int, len: Int): Unit = {
    visitValue(facade.visitString(charSequence(buf, off, len)))
  }

  override def writeString(text: SerializableString): Unit = {
    writeString(text.getValue)
  }

  override def writeRawUTF8String(text: Array[Byte], offset: Int, length: Int): Unit = throw notSupported

  override def writeUTF8String(text: Array[Byte], offset: Int, length: Int): Unit = throw notSupported

  override def writeRaw(text: String): Unit = throw notSupported

  override def writeRaw(text: String, offset: Int, len: Int): Unit = throw notSupported

  override def writeRaw(text: Array[Char], offset: Int, len: Int): Unit = throw notSupported

  override def writeRaw(c: Char): Unit = throw notSupported

  override def writeRawValue(text: String): Unit = throw notSupported

  override def writeRawValue(text: String, offset: Int, len: Int): Unit = throw notSupported

  override def writeRawValue(text: Array[Char], offset: Int, len: Int): Unit = throw notSupported

  override def writeBinary(bv: Base64Variant, data: Array[Byte], offset: Int, len: Int): Unit = {
    visitValue(facade.visitBinary(data, offset, len))
  }

  override def writeBinary(bv: Base64Variant, data: InputStream, dataLength: Int): Int = {
    // Size to dataLength + 1. If the estimate is correct, we can avoid doubling the buffer,
    // i.e. last read() will be made with 1 buf slot free, and will return -1.
    var buffer = Array.ofDim[Byte](if (dataLength > 0) dataLength + 1 else 32)
    var size = 0
    var r = data.read(buffer)
    while (r != -1) {
      size += r
      if (size == buffer.length) {
        val old = buffer
        buffer = Array.ofDim[Byte](buffer.length * 2)
        System.arraycopy(old, 0, buffer, 0, size)
      }
      r = data.read(buffer, size, buffer.length - size)
    }
    data.close()
    visitValue(facade.visitBinary(buffer, 0, size))
    size
  }

  override def writeNumber(v: Int): Unit = {
    visitValue(facade.visitInt32(v))
  }

  override def writeNumber(v: Long): Unit = {
    visitValue(facade.visitInt64(v))
  }

  override def writeNumber(v: BigInteger): Unit = {
    visitValue(facade.visitFloat64StringParts(v.toString, -1, -1))
  }

  override def writeNumber(v: Double): Unit = {
    visitValue(facade.visitFloat64(v))
  }

  override def writeNumber(v: Float): Unit = {
    visitValue(facade.visitFloat64(v))
  }

  override def writeNumber(v: java.math.BigDecimal): Unit = {
    visitValue(facade.visitFloat64String(v.toString))
  }

  override def writeNumber(encodedValue: String): Unit = {
    visitValue(facade.visitFloat64String(encodedValue))
  }

  override def writeBoolean(state: Boolean): Unit = {
    if (state) writeTrue()
    else writeFalse()
  }

  def writeTrue(): Unit = visitValue(facade.visitTrue())

  def writeFalse(): Unit = visitValue(facade.visitFalse())

  override def writeNull(): Unit = visitValue(facade.visitNull())

  override def writeObject(pojo: Any): Unit = objectCodec.writeValue(this, pojo)

  override def writeTree(rootNode: TreeNode): Unit = objectCodec.writeValue(this, rootNode)

  override def getOutputContext: JsonStreamContext = null

  override def flush(): Unit = ()

  override def isClosed: Boolean = top == null

  override def close(): Unit = {
    if (!isClosed) {
      stack = Nil
      ctxt = null
    }
  }

  private def visitValue(any: Any): Unit = {
    top.visitValue(any)
  }

  private def notSupported: UnsupportedOperationException = new UnsupportedOperationException()
}

/**
  * Reference to a chunk of text in a VERY MUTABLE BUFFER.
  *
  * Contents are guaranteed to be stable for as long as the receiving method is on the stack.
  * After the receiving method returns control, the contents may change.
  *
  * Cheaper than allocating a String when one is not needed.
  */
private final class TextBufferCharSequence(var buf: Array[Char], var off: Int, var len: Int) extends CharSequence {

  override def charAt(index: Int): Char = buf(off + index)

  override def subSequence(start: Int, end: Int): CharSequence =
    new TextBufferCharSequence(buf, off + start, end - start)

  override def toString: String = new String(buf, off, length())

  override def length(): Int = len
}
