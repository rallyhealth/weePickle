package com.rallyhealth.weejson.v1.play

import play.api.libs.json.{JsArray, JsBoolean, JsObject}

/**
  * Shared values to reduce memory usage.
  */
object JsValueSingletons {

  final val jsTrue = JsBoolean(true)
  final val jsFalse = JsBoolean(false)
  object EmptyJsObject extends JsObject(Map.empty)
  object EmptyJsArray extends JsArray()
}
