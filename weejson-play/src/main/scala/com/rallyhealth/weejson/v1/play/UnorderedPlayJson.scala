package com.rallyhealth.weejson.v1.play

import com.rallyhealth.weepickle.v1.core.{ObjVisitor, StringVisitor, Visitor}
import play.api.libs.json._

import scala.collection.immutable.TreeMap
import scala.collection.mutable.ArrayBuffer

object UnorderedPlayJson extends UnorderedPlayJson

/**
  * Uses ~0.6x of the heap of [[PlayJson]] in exchange for undefined order of JsObject keys.
  * Ideal for {{{jsValue.as[T]}}} where order is irrelevant and inputs may be large.
  *
  * Throughput of {{{ .transform(UnorderedPlayJson).as[T] }}} is typically
  * only 0.8x of [[PlayJson]] when memory is plentiful. Becomes faster than
  * [[PlayJson]] when the memory savings would prevent GC pressure, e.g. large
  * messages, high parallelism, or both.
  *
  * Heap usage for a particularly large JSON file captured from the wild:
  *  - 814 MB:   `Array[Byte]`
  *  - 5,629 MB: `play.Json.parse()`
  *  - 5,243 MB: `FromJson().transform(PlayJson)` (`java.util.LinkedHashMap` initialCapacity=2)
  *  - 3,355 MB: `FromJson().transform(UnorderedPlayJson)` (`Map1-4` + `TreeMap`)
  *  - 3,107 MB: `FromJson().transform(UnorderedPlayJson)` (jsObjectEmpty + `Map1-4` + `TreeMap`)
  */
class UnorderedPlayJson extends PlayJson {

  override def visitObject(length: Int): ObjVisitor[JsValue, JsValue] = new ObjVisitor[JsValue, JsValue] {
    private[this] var key: String = _
    // initCapacity=4 covers 88% of real-world objs. Faster overall. (JsValueBench)
    private[this] val buf = new ArrayBuffer[(String, JsValue)](if (length >= 0) length else 4)

    override def visitKey(): Visitor[_, _] = StringVisitor

    override def visitKeyValue(v: Any): Unit = key = v.toString

    override def subVisitor: Visitor[_, _] = UnorderedPlayJson.this

    override def visitValue(v: JsValue): Unit = buf += (key -> v)

    override def visitEnd(): JsValue = {
      if (buf.isEmpty) JsValueSingletons.EmptyJsObject
      else if (buf.size <= 4) JsObject(buf.toMap) // preserves order
      else JsObject((TreeMap.newBuilder[String, JsValue] ++= buf).result())
    }
  }
}
