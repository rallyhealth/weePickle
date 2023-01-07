package com.rallyhealth.weejson.v1.play

import com.rallyhealth.weepickle.v1.core.ObjVisitor
import play.api.libs.json._

import scala.collection.immutable.TreeMap
import scala.collection.mutable.ArrayBuffer

object UnorderedPlayJson extends UnorderedPlayJson

/**
  * Uses ~0.6x of the heap of [[PlayJson]] in exchange for undefined order of JsObject keys.
  * Ideal for {{{jsValue.as[T]}}} where order is irrelevant.
  *
  * Heap usage for a particularly large JSON file captured from the wild:
  *  - 814 MB:   `Array[Byte]`
  *  - 5,629 MB: `play.Json.parse()`
  *  - 5,243 MB: `FromJson().transform(PlayJson)` (`java.util.LinkedHashMap` initialCapacity=2)
  *  - 3,355 MB: `FromJson().transform(UnorderedPlayJson)` (`Map1-4` + `TreeMap`)
  *  - 3,107 MB: `FromJson().transform(UnorderedPlayJson)` (jsObjectEmpty + `Map1-4` + `TreeMap`)
  */
class UnorderedPlayJson extends PlayJson {

  override def visitObject(length: Int): ObjVisitor[JsValue, JsValue] = {
    new AstObjVisitor[ArrayBuffer[(String, JsValue)]](toJsObject)
  }

  private def toJsObject(buf: ArrayBuffer[(String, JsValue)]) = {
    if (buf.isEmpty) JsValueSingletons.jsObjectEmpty
    else if (buf.size <= 4) JsObject(buf.toMap)
    else JsObject((TreeMap.newBuilder[String, JsValue] ++= buf).result())
  }
}
