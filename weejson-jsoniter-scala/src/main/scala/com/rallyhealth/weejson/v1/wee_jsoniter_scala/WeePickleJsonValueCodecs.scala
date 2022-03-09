package com.rallyhealth.weejson.v1.wee_jsoniter_scala

import com.github.plokhotnyuk.jsoniter_scala.core.{JsonReader, JsonReaderException, JsonValueCodec, JsonWriter}
import com.rallyhealth.weepickle.v1.core.JsonPointerVisitor.JsonPointerException
import com.rallyhealth.weepickle.v1.core.{FromInput, Visitor}

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
        parseNumberCounter(in, v)
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

    private def parseNumberCounter[J](
      in: JsonReader,
      v: Visitor[_, J]
    ): J = {
      in.setMark()
      var b = in.nextByte()
      var digits, index = 0
      var decIndex, expIndex = -1
      if (b == '-') {
        b = in.nextByte()
        index += 1
      }
      try {
        digits -= index
        while (b >= '0' && b <= '9') {
          b = in.nextByte()
          index += 1
        }
        digits += index
        if (b == '.') {
          decIndex = index
          b = in.nextByte()
          index += 1
        }
        digits -= index
        while (b >= '0' && b <= '9') {
          b = in.nextByte()
          index += 1
        }
        digits += index
        if ((b | 0x20) == 'e') {
          expIndex = index
          b = in.nextByte()
          index += 1
          if (b == '-' || b == '+') {
            b = in.nextByte()
            index += 1
          }
          while (b >= '0' && b <= '9') {
            b = in.nextByte()
            index += 1
          }
        }
      } catch {
        case _: JsonReaderException =>
          index += 1 // for length calcs, pretend that nextByte() didn't hit EOF
      } finally in.rollbackToMark()
      if ((decIndex & expIndex) == -1) {
        if (digits < 19) v.visitInt64(in.readLong())
        else {
          val x = in.readBigInt(null)
          if (x.bitLength < 64) v.visitInt64(x.longValue)
          else v.visitFloat64StringParts(x.toString, -1, -1)
        }
      } else {
        val cs = new String(in.readRawValAsBytes(), StandardCharsets.US_ASCII)
        require(cs.length == index, "invalid number")
        v.visitFloat64StringParts(cs, decIndex, expIndex)
      }
    }


    private def parseNumberRegex[J](
      in: JsonReader,
      v: Visitor[_, J]
    ): J = {
      in.setMark()
      var digits = 0
      var b = in.nextByte()
      if (b == '-') b = in.nextByte()
      try {
        while (b >= '0' && b <= '9') {
          b = in.nextByte()
          digits += 1
        }
      } catch {
        case _: JsonReaderException => // ignore the end of input error for now
      } finally in.rollbackToMark()

      if ((b | 0x20) != 'e' && b != '.') {
        if (digits < 19) {
          val l = in.readLong()
          v.visitInt64(l)
        } else {
          val x = in.readBigInt(null)
          if (x.bitLength < 64) v.visitInt64(x.longValue)
          else v.visitFloat64StringParts(x.toString, -1, -1)
        }
      } else {
        val cs = new String(in.readRawValAsBytes(), StandardCharsets.US_ASCII)

        /**
          * This regex performs rather badly, but gets the tests passing.
          *
          * We're looking for a value we can pass through the Visitor interface--
          * either a primitive Double or a CharSequence representing a *valid*
          * number conforming to https://datatracker.ietf.org/doc/html/rfc7159#page-6.
          *
          * `in.readRawValAsBytes()` does NOT do that validation. It will happily
          * return a String of "------".
          *
          * `in.readBigDecimal(null).toString` is tempting, but will not provide the raw input.
          * Instead, it transforms the input from "0.00000001" to "1.0E-8".
          * This fails roundtrip tests.
          *
          * I tried combining the two approaches, `in.readBigDecimal(null)` for validation,
          * then `in.rollbackToMark()` + `in.readRawValAsBytes()` to capture the raw input,
          * but for a value like "1.0-----", `in.readBigDecimal(null)` will read "1.0",
          * then `in.readRawValAsBytes()` will return the whole string, including the unwanted
          * trailing hyphens.
          *
          */
        require(ValidJsonNum.pattern.matcher(cs).matches(), "invalid number")
        v.visitFloat64String(cs)
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

  private val ValidJsonNum = """-?(0|[1-9]\d*)(\.\d+)?([eE][-+]?\d+)?""".r // based on https://datatracker.ietf.org/doc/html/rfc7159#page-6
}
