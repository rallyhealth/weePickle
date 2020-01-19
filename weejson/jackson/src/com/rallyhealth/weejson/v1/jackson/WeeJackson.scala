package com.rallyhealth.weejson.v1.jackson

import java.io.{File, InputStream}
import java.nio.file.Path

import com.fasterxml.jackson.core.{JsonGenerator, JsonParser}
import com.rallyhealth.weejson.v1.jackson.DefaultJsonFactory.Instance
import com.rallyhealth.weepickle.v1.core.{CallbackVisitor, Visitor}

import scala.util.Try
import scala.util.control.NonFatal

/**
  * Adapter to the Jackson parsers and generators.
  *
  * @see https://github.com/FasterXML/jackson
  */
object WeeJackson {

  /**
    * Generates a single json value (e.g. `{"a": 1, "b": 2}`, `[1,2,3]`, `"pony"`, or `42`),
    * then closes, releasing underlying buffers back to the pool and preventing further writes.
    *
    * ==Configuration==
    * - [[com.fasterxml.jackson.core.json.JsonWriteFeature]]
    * - [[JsonGenerator.Feature]]
    *
    * @see https://github.com/FasterXML/jackson-core/wiki/JsonGenerator-Features
    */
  def toJsonSingle(generator: JsonGenerator): Visitor[Any, JsonGenerator] = {
    toJsonMultiple(generator).map { gen =>
      Try(gen.close())
      gen
    }
  }

  /**
    * Generates multiple json values (e.g. `{} "pony" 42`) separated by
    * [[com.fasterxml.jackson.core.PrettyPrinter.DEFAULT_ROOT_VALUE_SEPARATOR]].
    *
    * ==close()==
    * The caller is responsible for calling .close(). Otherwise, data may not be
    * flushed, buffers will not be released back to the pool, performance may
    * suffer due to buffer reallocation.
    *
    *  ==Configuration==
    * - [[com.fasterxml.jackson.core.json.JsonWriteFeature]]
    * - [[JsonGenerator.Feature]]
    *
    * @see https://github.com/FasterXML/jackson-core/wiki/JsonGenerator-Features
    */
  def toJsonMultiple(generator: JsonGenerator): Visitor[Any, JsonGenerator] = {
    new JsonGeneratorVisitor(generator).map { gen =>
      gen.flush()
      gen
    }
  }

  /**
    * Parses a single JSON value using the default Jackson parser.
    */
  def parseSingle[J](input: JsonInput, visitor: Visitor[_, J]): J = {
    parseMultiple(input, visitor) match {
      case Nil => throw VisitorException("Reached end of input, but Visitor produced no result.", input.parser, null)
      case head :: Nil => head
      case many => throw VisitorException("Expected 1 result. Visitor produced many.", input.parser, null)
    }
  }

  /**
    * Eagerly parses multiple elements separated by whitespace, e.g. """{} 5 true""".
    */
  def parseMultiple[Elem](
    input: JsonInput,
    visitor: Visitor[_, Elem]
  ): List[Elem] = {
    val p = input.parser
    val builder = List.newBuilder[Elem]
    val generator = new VisitorJsonGenerator(
      new CallbackVisitor(visitor)(builder += _)
    )

    try {
      while (p.nextToken() != null) {
        generator.copyCurrentEvent(p)
      }
    } catch {
      case NonFatal(t) =>
        throw VisitorException("Parser or Visitor failure", p, t)
      }
    finally {
      Try(p.close()) // completely consumed.
      Try(generator.close()) // we created it, so we close it.
    }

    builder.result()
  }

  /**
    * Magnet for data types that can be wrapped by a [[JsonParser]].
    */
  case class JsonInput(parser: JsonParser)

  object JsonInput {

    implicit def fromParser(s: JsonParser) = JsonInput(s)

    implicit def fromString(s: String) = JsonInput(Instance.createParser(s))

    implicit def fromInputStream(s: InputStream) = JsonInput(Instance.createParser(s))

    implicit def fromFile(s: File) = JsonInput(Instance.createParser(s))

    implicit def fromPath(s: Path) = JsonInput(Instance.createParser(s.toFile))

    implicit def fromByteArray(s: Array[Byte]) = JsonInput(Instance.createParser(s))
  }

}
