package com.rallyhealth.weejson.v1.jackson

import com.fasterxml.jackson.core.JsonParser.NumberType
import com.fasterxml.jackson.core.JsonToken._
import com.fasterxml.jackson.core.JsonTokenId._
import com.fasterxml.jackson.core.{JsonFactory, JsonParser, JsonToken, JsonTokenId}
import com.rallyhealth.weepickle.v1.core.{Abort, CallbackVisitor, FromInput, TransformException, Visitor}

import java.io.{File, InputStream, Reader}
import java.nio.file.Path
import scala.annotation.switch
import scala.util.Try
import scala.util.control.NonFatal

object FromJson extends JsonParserOps {

  override def apply(string: String): FromInput = super.apply(string)

  override def apply(bytes: Array[Byte]): FromInput = super.apply(bytes)

  override def apply(in: InputStream): FromInput = super.apply(in)

  override def apply(reader: Reader): FromInput = super.apply(reader)

  override def apply(file: File): FromInput = super.apply(file)

  override def apply(path: Path): FromInput = super.apply(path)
}

abstract class JsonParserOps(factory: JsonFactory = DefaultJsonFactory.Instance) {

  def apply(string: String): FromInput = fromReplayableParser(() => factory.createParser(string))

  def apply(bytes: Array[Byte]): FromInput = fromReplayableParser(() => factory.createParser(bytes))

  def apply(in: InputStream): FromInput = fromParser(factory.createParser(in))

  def apply(reader: Reader): FromInput = fromParser(factory.createParser(reader))

  def apply(file: File): FromInput = fromReplayableParser(() => factory.createParser(file))

  def apply(path: Path): FromInput = fromReplayableParser(() => factory.createParser(path.toFile))

  protected def fromParser(parser: JsonParser): FromInput = new JsonFromInput(parser)

  protected def fromReplayableParser(parser: () => JsonParser): FromInput = new JsonFromInput(parser)
}

/**
  * Adapter to the Jackson parsers and generators.
  *
  * @see https://github.com/FasterXML/jackson
  */
class JsonFromInput(createParser: () => JsonParser) extends FromInput {

  def this(parser: JsonParser) = this(() => parser)

  override def transform[J](
    to: Visitor[_, J]
  ): J = {
    val parser = createParser()
    try {
      if (parser.isClosed) throw new Abort("Parser is closed.")
      parser.nextToken()
      val result = parseRec(to, 64)(parser)
      if (parser.nextToken() != null) {
        // Multiple values is unsupported at this level of abstraction.
        // https://tools.ietf.org/html/rfc8259#section-2 says:
        // > JSON-text = ws value ws
        // If you want multiple whitespace separated values, you'll have to use lower level APIs.
        throw JsonParserException("Unexpected data after end of single json value", parser)
      }
      result
    } catch {
      case t: TransformException =>
        throw t
      case NonFatal(t) =>
        throw JsonParserException("Parser or Visitor failure", parser, t)
    } finally {
      Try(parser.close()) // completely consumed.
    }
  }

  /**
    * Mimics [[com.fasterxml.jackson.core.JsonGenerator#copyCurrentStructure]],
    * but does not lose precision over JSON BigDecimal numbers.
    *
    * Uses on-stack Obj/ArrVisitor tracking up to some fixed depth, then falls
    * back to a stack-safe approach.
    *
    * Precondition: parser points to the token to be parsed, e.g. STRING, START_ARRAY, etc.
    * Post-condition: parser points to the last token parsed, e.g. STRING, END_ARRAY, etc.
    */
  private def parseRec[T, J](
    v: Visitor[T, J],
    remainingDepth: Int
  )(implicit
    p: JsonParser
  ): J = {
    val token = p.currentToken()
    if (token == null) throw JsonParserException("Premature EOF", p)
    (token.id(): @switch) match {
      case ID_NOT_AVAILABLE | ID_NO_TOKEN =>
        throw JsonParserException("No current event to copy", p)
      case ID_EMBEDDED_OBJECT =>
        copyStackSafe(v)
      case ID_STRING =>
        val cs: CharSequence =
          if (p.getTextLength == 0) ""
          else {
            val start = p.getTextOffset
            val end = start + p.getTextLength
            new runtime.ArrayCharSequence(p.getTextCharacters, start, end)
          }
        v.visitString(cs)
      case ID_FIELD_NAME =>
        v.visitString(p.getCurrentName)
      case ID_NUMBER_INT =>
        p.getNumberType match {
          case NumberType.INT => v.visitInt32(p.getIntValue)
          case NumberType.LONG => v.visitInt64(p.getLongValue)
          case _ => v.visitFloat64String(p.getValueAsString)
        }
      case ID_NUMBER_FLOAT =>
        // p.getNumberType cannot be trusted!
        // For JSON, it always returns DOUBLE, even if the number would lose precision.
        // getNumberValueExact() always returns a BigDecimal (-50% perf)
        // So we toss the "float" in a string and let the Visitor figure it out.
        // Being lazy can be a boon. Visitor might not even use the value.
        // For jackson-dataformats-binary, this results in unfortunate conversions from
        // base2 => base10 => base2, but there's currently no way to know when
        // JsonParser.getNumberType() will answer accurately without add'l cost.
        // Wishlist: `JsonParser.isCurrentNumberTypeGuaranteedExact(): Boolean`
        v.visitFloat64String(p.getValueAsString())
      case ID_NULL =>
        v.visitNull()
      case ID_TRUE =>
        v.visitTrue()
      case ID_FALSE =>
        v.visitFalse()
      case ID_START_ARRAY =>
        val depthM1 = remainingDepth - 1
        if (depthM1 < 0) copyStackSafe(v)
        else {
          if (p.nextToken() == END_ARRAY) {
            v.visitArray(0).visitEnd()
          } else {
            val arr = v.visitArray(-1).narrow
            while ( {
              arr.visitValue(parseRec(arr.subVisitor, depthM1))
              p.nextToken() != END_ARRAY
            }) ()
            arr.visitEnd()
          }
        }
      case ID_START_OBJECT =>
        val depthM1 = remainingDepth - 1
        if (depthM1 < 0) copyStackSafe(v)
        else {
          if (p.nextToken() == END_OBJECT) {
            v.visitObject(0).visitEnd()
          } else {
            val obj = v.visitObject(-1).narrow
            while ( {
              obj.visitKeyValue(parseRec(obj.visitKey(), depthM1))
              p.nextToken()
              obj.visitValue(parseRec(obj.subVisitor, depthM1))
              p.nextToken() != JsonToken.END_OBJECT
            }) ()
            obj.visitEnd()
          }
        }
      case _ =>
        throw JsonParserException(s"unknown current token $token", p)
    }
  }

  /**
    * Precondition: parser points to the token to be parsed, e.g. STRING, START_ARRAY, etc.
    * Post-condition: parser points to the last token parsed, e.g. STRING, END_ARRAY, etc.
    */
  private def copyStackSafe[J](
    visitor: Visitor[_, J]
  )(implicit
    parser: JsonParser
  ): J = {
    // this stuff is slower, but only used rarely at high depth.
    val builder = List.newBuilder[J]
    val generator =
      new VisitorJsonGenerator(
        new CallbackVisitor(visitor)(builder += _)
      )

    var depth = 0
    while ( {
      import JsonTokenId._
      val token = parser.currentToken()
      token.id match {
        case ID_NUMBER_FLOAT =>
          // Special case float: https://github.com/FasterXML/jackson-core/issues/730
          generator.writeNumber(parser.getValueAsString)
        case ID_START_OBJECT | ID_START_ARRAY =>
          depth += 1
          generator.copyCurrentEvent(parser)
        case ID_END_OBJECT | ID_END_ARRAY =>
          depth -= 1
          generator.copyCurrentEvent(parser)
        case _ =>
          generator.copyCurrentEvent(parser)
      }
      // advance to next token except for the last one
      depth > 0 && parser.nextToken() != null
    }) ()

    builder.result() match {
      case Nil         => throw JsonParserException("Reached end of input, but Visitor produced no result.", parser)
      case head :: Nil => head
      case many        => throw JsonParserException("Expected 1 result. Visitor produced many.", parser)
    }
  }
}
