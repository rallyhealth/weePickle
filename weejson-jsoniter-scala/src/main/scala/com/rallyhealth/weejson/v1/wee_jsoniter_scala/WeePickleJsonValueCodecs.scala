package com.rallyhealth.weejson.v1.wee_jsoniter_scala

import com.github.plokhotnyuk.jsoniter_scala.core.{JsonReader, JsonReaderException, JsonValueCodec, JsonWriter}
import com.rallyhealth.weepickle.v1.core.JsonPointerVisitor.JsonPointerException
import com.rallyhealth.weepickle.v1.core.{FromInput, Types, Visitor}

import java.nio.charset.StandardCharsets
import scala.util.control.{NoStackTrace, NonFatal}

object WeePickleJsonValueCodecs {

  implicit object FromInputJsonValueEncoder extends JsonValueCodec[FromInput] {

    override def decodeValue(
      in: JsonReader,
      default: FromInput
    ): FromInput = throw new UnsupportedOperationException(
      "only supports encoding"
    )

    override def encodeValue(
      fromInput: FromInput,
      out: JsonWriter
    ): Unit = {
      fromInput.transform(new JsonWriterVisitor(out))
    }

    override def nullValue: FromInput = null
  }

  private case class JsonPathParts(
    pointer: List[String],
    cause: Throwable
  ) extends RuntimeException(cause)
      with NoStackTrace {

    override def getMessage: String = pointer.mkString("/")
  }

  class VisitorJsonValueEncoder[J](
    v: Visitor[_, J],
    maxDepth: Int = 64
  ) extends JsonValueCodec[J] {

    override def decodeValue(
      in: JsonReader,
      default: J
    ): J = {
      try {
        decodeValue(in, v, maxDepth)
      } catch {
        case JsonPathParts(pointer, cause) =>
          val jsonPointer = pointer.view
            .map(_.replace("~", "~0").replace("/", "~1"))
            .mkString("/", "/", "")
          throw new JsonPointerException(jsonPointer, cause)
        case NonFatal(t) =>
          throw new JsonPointerException("", t)
      }
    }

    private def decodeValue[TT, JJ](
      in: JsonReader,
      v: Visitor[_, JJ],
      depth: Int
    ): JJ = {
      val b = in.nextToken()

      if (b == '"') {
        in.rollbackToken()
        v.visitString(in.readString(null))
      } else if (b == 't' || b == 'f') {
        in.rollbackToken()
        if (in.readBoolean()) v.visitTrue() else v.visitFalse()
      } else if ((b >= '0' && b <= '9') || b == '-') {
        in.rollbackToken()
        parseNumber(in, v)
      } else if (b == '[') {
        val depthM1 = depth - 1
        if (depthM1 < 0) in.decodeError("depth limit exceeded")
        val isEmpty = in.isNextToken(']')
        var i = 0
        val arr = v.visitArray(if (isEmpty) 0 else -1).narrow
        try {
          if (!isEmpty) {
            in.rollbackToken()
            while ({
              arr.visitValue(decodeValue(in, arr.subVisitor, depthM1))
              i += 1
              in.isNextToken(',')
            }) ()
            if (!in.isCurrentToken(']')) in.arrayEndOrCommaError()
          }
        } catch {
          case NonFatal(t) => prependFailurePathInfo(t, String.valueOf(i))
        }
        arr.visitEnd()
      } else if (b == '{') {
        val depthM1 = depth - 1
        if (depthM1 < 0) in.decodeError("depth limit exceeded")
        val isEmpty = in.isNextToken('}')
        val obj = v.visitObject(if (isEmpty) 0 else -1).narrow
        if (!isEmpty) {
          in.rollbackToken()
          var key: String = "?"
          try {
            while ({
              key = in.readKeyAsString()
              obj.visitKeyValue(obj.visitKey().visitString(key))
              obj.visitValue(decodeValue(in, obj.subVisitor, depthM1))
              in.isNextToken(',')
            }) ()
            if (!in.isCurrentToken('}')) in.objectEndOrCommaError()
          } catch {
            case NonFatal(t) => prependFailurePathInfo(t, key)
          }
        }
        obj.visitEnd()
      } else {
        in.readNullOrError(v.visitNull(), "expected `null` value")
      }
    }

    private def parseNumber[J](
      in: JsonReader,
      v: Visitor[_, J]
    ): J = {
      in.setMark()
      // Identify all important indexes before choosing parsing strategy.
      var hasLeadingZero = false
      var hasMoreInput = false
      var pos = 0
      var decIndex = -1
      var expIndex = -1
      var digits = 0
      var decDigits = 0
      var expDigits = 0
      var b = in.nextByte()
      if (b == '-') {
        b = in.nextByte()
        pos += 1
      }
      try {
        hasLeadingZero = b == '0'
        while (b >= '0' && b <= '9') {
          digits += 1
          b = in.nextByte()
        }
        pos += digits

        if (b == '.') {
          decIndex = pos
          b = in.nextByte()
          pos += 1

          while (b >= '0' && b <= '9') {
            decDigits += 1
            b = in.nextByte()
          }
          pos += decDigits
        }

        if ((b | 0x20) == 'e') {
          expIndex = pos // don't need pos anymore.
          b = in.nextByte()
          if (b == '+' || b == '-') {
            b = in.nextByte()
          }
          while (b >= '0' && b <= '9') {
            expDigits += 1
            b = in.nextByte()
          }
        }
        hasMoreInput = true
      } catch {
        case _: JsonReaderException => // ignore the end of input error for now
      } finally in.rollbackToMark()

      // reject cases like 1e1e1 which in.readRawValAsBytes()/skipNumber() would treat as valid

      // If we can _trivially_ parse to a primitive, then do so.
      // Otherwise, CharSequence and the visitor can deal with it.
      if (decIndex == -1 && digits < 19) {
        if (expIndex == -1) {
          v.visitInt64(in.readLong())
        } else if (expDigits <= 3) {
          // 3-char base10 exponents all fit within the bounds of −1022 to 1023 (11 bits)
          v.visitFloat64(in.readDouble())
        } else {
          if (hasMoreInput && ((b >= '0' && b <= '9') || b == '.' || (b | 0x20) == 'e' || b == '-' || b == '+'))
            in.decodeError("invalid number")

          if (digits == 0) in.decodeError("invalid number")
          if (hasLeadingZero && digits != 1) in.decodeError("invalid number")
          if (decIndex != -1 && decDigits == 0) in.decodeError("invalid number")
          if (expIndex != -1 && expDigits == 0) in.decodeError("invalid number")
          v.visitFloat64StringParts(asAsciiCharSequence(in.readRawValAsBytes), decIndex, expIndex)
        }
      } else {
        if (hasMoreInput && ((b >= '0' && b <= '9') || b == '.' || (b | 0x20) == 'e' || b == '-' || b == '+'))
          in.decodeError("invalid number")
        if (digits == 0) in.decodeError("invalid number")
        if (hasLeadingZero && digits != 1) in.decodeError("invalid number")
        if (decIndex != -1 && decDigits == 0) in.decodeError("invalid number")
        if (expIndex != -1 && expDigits == 0) in.decodeError("invalid number")
        v.visitFloat64StringParts(asAsciiCharSequence(in.readRawValAsBytes), decIndex, expIndex)
      }
    }

    private def asAsciiCharSequence(asciiBytes: Array[Byte]): CharSequence = {
      new CharSequence {
        override def length(): Int = asciiBytes.length

        override def charAt(index: Int): Char = asciiBytes(index).toChar

        override def subSequence(start: Int, end: Int): CharSequence = toString.subSequence(start, end)

        override def toString: String = new String(asciiBytes, StandardCharsets.US_ASCII)
      }
    }

    private def prependFailurePathInfo(
      t: Throwable,
      pathSegment: String
    ): Nothing = t match {
      case JsonPathParts(pointer, cause) =>
        throw JsonPathParts(pathSegment :: pointer, cause)
      case other: Throwable => throw JsonPathParts(pathSegment :: Nil, other)
    }

    override def nullValue: J = null.asInstanceOf[J] // unused

    override def encodeValue(
      x: J,
      out: JsonWriter
    ): Unit = {
      throw new UnsupportedOperationException("only supports decoding")
    }
  }
}
