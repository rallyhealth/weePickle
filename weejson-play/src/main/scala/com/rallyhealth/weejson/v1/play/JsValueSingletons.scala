package com.rallyhealth.weejson.v1.play

import play.api.libs.json.{JsBoolean, JsObject}

/**
  * Shared values to reduce memory usage.
  */
object JsValueSingletons {

  val jsTrue = JsBoolean(true)
  val jsFalse = JsBoolean(false)
  val jsObjectEmpty = JsObject.empty
}
