package com.rallyhealth.weejson.v1.play

import play.api.libs.json.{JsBoolean, JsObject}

/**
  * Shared values to reduce memory usage.
  */
object JsValueSingletons {

  final val jsTrue = JsBoolean(true)
  final val jsFalse = JsBoolean(false)
  final val jsObjectEmpty = JsObject(List.empty)
}
