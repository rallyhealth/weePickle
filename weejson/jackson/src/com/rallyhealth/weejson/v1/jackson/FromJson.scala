package com.rallyhealth.weejson.v1.jackson

import java.io.{InputStream, Reader}

import com.fasterxml.jackson.core.{JsonFactory, JsonParser}
import com.rallyhealth.weepickle.v1.core.{Transformable, Visitor}

object FromJson extends JsonParserOps {

  override def apply(string: String): Transformable = super.apply(string)

  override def apply(bytes: Array[Byte]): Transformable = super.apply(bytes)

  override def apply(in: InputStream): Transformable = super.apply(in)

  override def apply(yaml: Reader): Transformable = super.apply(yaml)
}

abstract class JsonParserOps(factory: JsonFactory = DefaultJsonFactory.Instance) {

  def apply(string: String): Transformable = fromParser(factory.createParser(string))

  def apply(bytes: Array[Byte]): Transformable = fromParser(factory.createParser(bytes))

  def apply(in: InputStream): Transformable = fromParser(factory.createParser(in))

  def apply(yaml: Reader): Transformable = fromParser(factory.createParser(yaml))

  protected def fromParser(parser: JsonParser): Transformable = new Transformable {
    override def transform[T](into: Visitor[_, T]): T = WeeJackson.parseSingle(parser, into)
  }
}
