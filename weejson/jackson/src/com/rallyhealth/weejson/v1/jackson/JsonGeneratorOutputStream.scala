package com.rallyhealth.weejson.v1.jackson

import java.io.OutputStream

import com.fasterxml.jackson.core.json.async.NonBlockingJsonParser
import com.fasterxml.jackson.core.{JsonGenerator, JsonToken}

import scala.util.Try
import scala.util.control.NonFatal

/**
  * Uses the fast jackson [[NonBlockingJsonParser]] to parse the input bytes,
  * and push the data to a [[JsonGenerator]].
  *
  * Remember to call [[close()]], or else you might miss some data.
  */
class JsonGeneratorOutputStream(
  generator: JsonGenerator,
  parser: NonBlockingJsonParser
) extends OutputStream {

  def this(generator: JsonGenerator) = this(
    generator,
    DefaultJsonFactory.Instance
      .createNonBlockingByteArrayParser()
      .asInstanceOf[NonBlockingJsonParser]
  )

  override def write(b: Int): Unit = {
    // intentionally unoptimized. Use BufferedOutputStream.
    write(Array(b.byteValue()))
  }

  override def write(bytes: Array[Byte], idx: Int, len: Int): Unit = {
    try {
      parser.feedInput(bytes, idx, idx + len)
      digest()
    } catch {
      case NonFatal(cause) =>
        throw JsonParserException("Parser or Visitor failure", parser, cause)
    }
  }

  private def digest(): Unit = {
    var token = parser.nextToken()

    while (token != null && token != JsonToken.NOT_AVAILABLE) {
      generator.copyCurrentEvent(parser)
      token = parser.nextToken()
    }
  }

  override def flush(): Unit = {
    generator.flush()
  }

  override def close(): Unit = {
    try {
      // flush parser's internal buffer.
      parser.endOfInput()
      digest()
    }
    finally {
      Try(generator.close())
      Try(parser.close())
    }
  }
}
