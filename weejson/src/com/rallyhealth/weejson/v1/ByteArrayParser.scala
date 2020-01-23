package com.rallyhealth.weejson.v1

import com.rallyhealth.weejson.v1.jackson.FromJson
import com.rallyhealth.weepickle.v1.core.Visitor


object ByteArrayParser extends Transformer[Array[Byte]] {

  override def transform[T](j: Array[Byte], f: Visitor[_, T]): T = {
    FromJson(j).transmit(f)
  }
}
