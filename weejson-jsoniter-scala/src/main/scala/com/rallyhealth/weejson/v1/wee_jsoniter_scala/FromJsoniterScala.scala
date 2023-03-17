package com.rallyhealth.weejson.v1.wee_jsoniter_scala

import com.github.plokhotnyuk.jsoniter_scala.core._
import com.rallyhealth.weejson.v1.wee_jsoniter_scala.WeePickleJsonValueCodecs.VisitorJsonValueEncoder
import com.rallyhealth.weepickle.v1.core.{FromInput, Visitor}

import java.io.InputStream
import java.nio.ByteBuffer

/**
  * A very fast UTF-8-only JSON parser.
  *
  * This integration:
  *  - tracks paths and returns a JsonPointer on error
  *  - does not deduplicate string keys (as jackson-core does)
  *  - throws when a fixed depth limit is reached
  *
  * @see https://github.com/plokhotnyuk/jsoniter-scala
  */
object FromJsoniterScala extends FromJsoniterScala(
  ReaderConfig
    .withAppendHexDumpToParseException(false) // avoid leaking sensitive plaintext data
)

class FromJsoniterScala(config: ReaderConfig) {

  def apply(
    bytes: Array[Byte]
  ): FromInput = new FromInput {

    override def transform[T](
      to: Visitor[_, T]
    ): T = readFromArray(bytes, config)(readerCodec(to))
  }

  def apply(
    in: InputStream
  ): FromInput = new FromInput {

    override def transform[T](
      to: Visitor[_, T]
    ): T = readFromStream(in, config)(readerCodec(to))
  }

  def apply(
    buf: ByteBuffer
  ): FromInput = new FromInput {

    override def transform[T](
      to: Visitor[_, T]
    ): T = readFromByteBuffer(buf, config)(readerCodec(to))
  }

  private def readerCodec[J](
    v: Visitor[_, J]
  ): JsonValueCodec[J] = new VisitorJsonValueEncoder[J](v, maxDepth = 64)
}
