package com.rallyhealth.weejson.v0.jackson

import java.io.InputStream

import com.fasterxml.jackson.core.{JsonGenerator, JsonParser}
import com.rallyhealth.weejson.v0.Readable
import com.rallyhealth.weejson.v0.jackson.DefaultJsonFactory.Instance
import com.rallyhealth.weepickle.v0.core.Visitor
import com.rallyhealth.weepickle.v1.core.CallbackVisitor

import scala.util.Try

/**
  * Adapter to the Jackson parsers and generators.
  *
  * @see https://github.com/FasterXML/jackson
  */
object WeeJackson {

  /**
    * Creates a Visitor that writes to the [[com.fasterxml.jackson.core.JsonGenerator]].
    *
    * Supported formats: https://github.com/FasterXML/jackson#data-format-modules
    *
    * @example
    * {{{
    *   val readable: Readable = Obj("pony" -> "twilight sparkle")
    *   readable.transform(
    *     WeeJackson.writeTo(
    *       DefaultJsonFactory.Instance.createGenerator(new FileWriter(new File("/tmp/pony.json")))
    *     )
    *   )
    * }}}
    */
  def visitor(generator: JsonGenerator): Visitor[_, JsonGenerator] = new JsonGeneratorVisitor(generator)

  /**
    * Parses a single JSON value using the default Jackson parser.
    *
    * @example
    * {{{
    *   WeeJackson.parse("""{"pony" -> "twilight sparkle"}""").transform(Value)
    * }}}
    */
  def parse(s: String): Readable = parse(Instance.createParser(s))

  /**
    * Parses a single JSON value using the default Jackson parser.
    *
    * @example
    * {{{
    *   val in = new FileInputStream("/tmp/pony.json")
    *   WeeJackson.parse(in).transform(Value)
    * }}}
    */
  def parse(in: InputStream): Readable = parse(Instance.createParser(in))

  /**
    * Parses a single JSON value using the default Jackson parser.
    *
    * @example
    * {{{
    *   val bytes = """{"pony" -> "twilight sparkle"}""".getBytes(UTF_8)
    *   WeeJackson.parse(bytes).transform(Value)
    * }}}
    */
  def parse(bytes: Array[Byte]): Readable = parse(Instance.createParser(bytes))

  /**
    * Parses a single JSON value using the default Jackson parser.
    */
  def parse(parser: JsonParser): Readable = new Readable {

    override def transform[J](visitor: Visitor[_, J]): J = {
      parseMultiple(parser, visitor) match {
        case Nil => throw VisitorException(parser, new NoSuchElementException("Reached end of input, but Visitor produced no result."))
        case head :: Nil => head
        case many => throw new VisitorException(s"Expected 1 result. Visitor produced many.", null)
      }
    }
  }

  /**
    * Eagerly parses multiple elements separated by whitespace, e.g. """{} 5 true""".
    */
  def parseMultiple[Elem](
    parser: JsonParser,
    visitor: Visitor[_, Elem]
  ): List[Elem] = {
    val builder = List.newBuilder[Elem]
    val generator = new VisitorJsonGenerator(
      new CallbackVisitor(visitor)(builder += _)
    )

    try {
      while (parser.nextToken() != null) {
        generator.copyCurrentEvent(parser)
      }

      builder.result()
    }
    finally {
      Try(parser.close())
      Try(generator.close())
    }
  }
}
