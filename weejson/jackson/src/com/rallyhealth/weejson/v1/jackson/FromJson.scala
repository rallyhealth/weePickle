package com.rallyhealth.weejson.v1.jackson

import java.io.{File, InputStream, Reader}
import java.nio.file.Path

import com.fasterxml.jackson.core.{JsonFactory, JsonParser}
import com.rallyhealth.weepickle.v1.core.{CallbackVisitor, Transformable, Visitor, TransformException}

import scala.util.Try
import scala.util.control.NonFatal

object FromJson extends JsonParserOps {

  override def apply(string: String): Transformable = super.apply(string)

  override def apply(bytes: Array[Byte]): Transformable = super.apply(bytes)

  override def apply(in: InputStream): Transformable = super.apply(in)

  override def apply(reader: Reader): Transformable = super.apply(reader)

  override def apply(file: File): Transformable = super.apply(file)

  override def apply(path: Path): Transformable = super.apply(path)
}

abstract class JsonParserOps(factory: JsonFactory = DefaultJsonFactory.Instance) {

  def apply(string: String): Transformable = fromParser(factory.createParser(string))

  def apply(bytes: Array[Byte]): Transformable = fromParser(factory.createParser(bytes))

  def apply(in: InputStream): Transformable = fromParser(factory.createParser(in))

  def apply(reader: Reader): Transformable = fromParser(factory.createParser(reader))

  def apply(file: File): Transformable = fromParser(factory.createParser(file))

  def apply(path: Path): Transformable = fromParser(factory.createParser(path.toFile))

  protected def fromParser(parser: JsonParser): Transformable = new JsonTransformable(parser)
}

/**
  * Adapter to the Jackson parsers and generators.
  *
  * @see https://github.com/FasterXML/jackson
  */
class JsonTransformable(parser: JsonParser) extends Transformable {

  override def transform[T](visitor: Visitor[_, T]): T = {
    if (parser.nextToken() == null) {
      return visitor.visitNull()
    }

    val builder = List.newBuilder[T]
    val generator = new VisitorJsonGenerator(
      new CallbackVisitor(visitor)(builder += _)
    )

    try {
      generator.copyCurrentStructure(parser)

      if (parser.nextToken() != null) {
        // Multiple values is unsupported at this level of abstraction.
        // https://tools.ietf.org/html/rfc8259#section-2 says:
        // > JSON-text = ws value ws
        // If you want multiple whitespace separated values, you'll have to use lower level APIs.
        throw JsonParserException("Unexpected data after end of single json value", parser)
      }

      builder.result() match {
        case Nil => throw JsonParserException("Reached end of input, but Visitor produced no result.", parser)
        case head :: Nil => head
        case many => throw JsonParserException("Expected 1 result. Visitor produced many.", parser)
      }
    } catch {
      case ve: TransformException => throw ve
      case NonFatal(t) =>
        throw JsonParserException("Parser or Visitor failure", parser, t)
    }
    finally {
      Try(parser.close()) // completely consumed.
      Try(generator.close()) // we created it, so we close it.
    }
  }
}
