package com.rallyhealth.weejson.v1

import com.rallyhealth.weejson.v1.jackson.FromJson
import com.rallyhealth.weepickle.v1.core.Visitor

object StringParser extends Transformer[String] {

  override def transform[T](j: String, f: Visitor[_, T]): T = {
    FromJson(j).transform(f)
  }
}
