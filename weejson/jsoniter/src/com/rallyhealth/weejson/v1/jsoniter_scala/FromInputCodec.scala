package com.rallyhealth.weejson.v1.jsoniter_scala

import com.github.plokhotnyuk.jsoniter_scala.core.{JsonReader, JsonValueCodec, JsonWriter}
import com.rallyhealth.weepickle.v1.core.FromInput

object FromInputCodec {

  implicit def encodeCodec: JsonValueCodec[FromInput] = new JsonValueCodec[FromInput] {
    override def decodeValue(
      in: JsonReader,
      default: FromInput
    ): FromInput = throw new UnsupportedOperationException("only supports encoding")

    override def encodeValue(x: FromInput, out: JsonWriter): Unit = {
      x.transform(new JsonWriterVisitor(out))
    }

    override def nullValue: FromInput = null
  }
}
