package com.rallyhealth.weejson.v0

import com.rallyhealth.weejson.v0.jackson.{DefaultJsonFactory, WeeJackson}
import com.rallyhealth.weepickle.v0.core.Visitor


object ByteArrayParser extends Transformer[Array[Byte]] {

  override def transform[T](j: Array[Byte], f: Visitor[_, T]): T = {
    WeeJackson.parseSingle(DefaultJsonFactory.Instance.createParser(j), f)
  }
}
