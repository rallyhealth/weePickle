package com.rallyhealth.weejson.v0

import com.rallyhealth.weejson.v0.jackson.{DefaultJsonFactory, WeeJackson}
import com.rallyhealth.weepickle.v0.core.Visitor

object StringParser extends Transformer[String] {

  override def transform[T](j: String, f: Visitor[_, T]): T = {
    WeeJackson.parseSingle(DefaultJsonFactory.Instance.createParser(j), f)
  }
}
