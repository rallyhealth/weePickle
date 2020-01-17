package com.rallyhealth.weejson.v1.jackson

import java.io.{File, InputStream}
import java.nio.file.Path

import com.fasterxml.jackson.core.{JsonGenerator, JsonParser}
import com.rallyhealth.weepickle.v1.core.Visitor
import com.rallyhealth.weepickle.v1.core.CallbackVisitor
import DefaultJsonFactory.Instance

import scala.util.Try
import scala.util.control.NonFatal

/**
  * Adapter to the Jackson parsers and generators.
  *
  * @see https://github.com/FasterXML/jackson
  */
object WeeJackson {

  def toGenerator(generator: JsonGenerator): Visitor[_, JsonGenerator] = new JsonGeneratorVisitor(generator)

  /**
    * Parses a single JSON value using the default Jackson parser.
    */
  def parseSingle[J](parser: Parser, visitor: Visitor[_, J]): J = {
    parseMultiple(parser, visitor) match {
      case Nil => throw VisitorException("Reached end of input, but Visitor produced no result.", parser.parser, null)
      case head :: Nil => head
      case many => throw VisitorException("Expected 1 result. Visitor produced many.", parser.parser, null)
    }
  }

  /**
    * Eagerly parses multiple elements separated by whitespace, e.g. """{} 5 true""".
    */
  def parseMultiple[Elem](
    parser: Parser,
    visitor: Visitor[_, Elem]
  ): List[Elem] = {
    val p = parser.parser
    val builder = List.newBuilder[Elem]
    val generator = new VisitorJsonGenerator(
      new CallbackVisitor(visitor)(builder += _)
    )

    try {
      while (p.nextToken() != null) {
        generator.copyCurrentEvent(p)
      }

      builder.result()
    } catch {
      case NonFatal(t) =>
        throw VisitorException("Parser or Visitor failure", p, t)
      }
    finally {
      Try(p.close())
      Try(generator.close())
    }
  }

  /**
    * Magnet.
    */
  case class Parser(parser: JsonParser)

  object Parser {

    implicit def fromParser(s: JsonParser) = Parser(s)

    implicit def fromString(s: String) = Parser(Instance.createParser(s))

    implicit def fromInputStream(s: InputStream) = Parser(Instance.createParser(s))

    implicit def fromFile(s: File) = Parser(Instance.createParser(s))

    implicit def fromPath(s: Path) = Parser(Instance.createParser(s.toFile))

    implicit def fromByteArray(s: Array[Byte]) = Parser(Instance.createParser(s))
  }

}
