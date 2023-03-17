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
      var b = in.nextByte()
      var intDigits, fracDigits, expDigits, punct = 0
      var decIndex, expIndex = -1
      if (b == '-') {
        punct += 1
        b = in.nextByte()
      }
      try {
        if (b == '0') {
          intDigits += 1
          b = in.nextByte()

          if (b >= '0' && b <= '9') {
            in.decodeError("invalid number")
          }
        }
        while (b >= '0' && b <= '9') {
          intDigits += 1
          b = in.nextByte()
        }
        if (b == '.') {
          decIndex = punct + intDigits
          punct += 1
          b = in.nextByte()
          while (b >= '0' && b <= '9') {
            fracDigits += 1
            b = in.nextByte()
          }
        }

        if ((b | 0x20) == 'e') {
          expIndex = punct + intDigits + fracDigits
          punct += 1
          b = in.nextByte()
          if (b == '-' || b == '+') {
            punct += 1
            b = in.nextByte()
          }
          while (b >= '0' && b <= '9') {
            expDigits += 1
            b = in.nextByte()
          }
        }
      } catch {
        case _: JsonReaderException =>
      } finally in.rollbackToMark()
      if ((decIndex & expIndex) == -1) {
        if (intDigits < 19) v.visitInt64(in.readLong())
        else {
          val x = in.readBigInt(null)
          if (x.bitLength < 64) v.visitInt64(x.longValue)
          else v.visitFloat64StringParts(x.toString, -1, -1)
        }
      } else {
        if (
          intDigits == 0 ||
          decIndex != -1 && fracDigits == 0 ||
          expIndex != -1 && expDigits == 0
        ) in.decodeError("invalid number")

        val cs = new String(in.readRawValAsBytes(), StandardCharsets.US_ASCII)
        val len = intDigits + fracDigits + expDigits + punct
        if (cs.length != len) in.decodeError("invalid number")
        v.visitFloat64StringParts(cs, decIndex, expIndex)
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
