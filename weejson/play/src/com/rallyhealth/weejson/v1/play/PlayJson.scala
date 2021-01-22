package com.rallyhealth.weejson.v1.play

import com.rallyhealth.weepickle.v1.WeePickle
import com.rallyhealth.weepickle.v1.WeePickle._
import com.rallyhealth.weepickle.v1.core.{ArrVisitor, ObjVisitor, Visitor}
import play.api.libs.json._

import java.util.{LinkedHashMap => JLinkedHashMap}
import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.jdk.CollectionConverters._

object PlayJson extends PlayJson

class PlayJson extends com.rallyhealth.weejson.v1.AstTransformer[JsValue] {
  def transform[T](i: JsValue, to: Visitor[_, T]): T = i match {
    case JsArray(xs)   => transformArray(to, xs)
    case JsBoolean(b)  => if (b) to.visitTrue() else to.visitFalse()
    case JsNull        => to.visitNull()
    case JsNumber(d)   => to.visitFloat64String(d.toString)
    case JsObject(kvs) => transformObject(to, kvs)
    case JsString(s)   => to.visitString(s)
  }
  def visitArray(length: Int): ArrVisitor[JsValue, JsValue] = new AstArrVisitor[Array](JsArray(_))

  def visitObject(
    length: Int
  ): ObjVisitor[JsValue, JsValue] =
    new AstObjVisitor[ArrayBuffer[(String, JsValue)]](
      buf => {
        // Goals:
        // 1. resist DoS.
        // 2. preserve ordering like JsObject does
        // 3. minimize heap usage.

        // ignore length because it's probably -1 for JSON.
        // Intermediate ArrayBuffer is slightly wasteful, but simple for determining length upfront.
        // Future optimization: handle all this incrementally in a Factory?
        val size = buf.size
        val map = if (size <= 4) {

          /**
            * ordered and optimal up to size 4
            * https://github.com/AudaxHealthInc/lib-stream-util/pull/19
            */
          buf.toMap
        } else if (size < 100) {

          /**
            * scala.LHM is decent up to size 100, but then DoS collisions become painful.
            *
            * https://github.com/scala/bug/issues/11203
            * https://github.com/playframework/play-json/issues/186
            * https://github.com/spray/spray-json/issues/277
            */
          val lhm = mutable.LinkedHashMap[String, JsValue]()
          lhm.sizeHint(buf.length)
          lhm ++= buf
        } else {

          /**
            * java.LHM has the DoS mitigations, but uses most heap.
            * https://github.com/AudaxHealthInc/lib-stream-util/pull/19
            */
          val jlhm = new JLinkedHashMap[String, JsValue](size).asScala
          jlhm ++= buf
          jlhm
        }
        new JsObject(map)
      }
    )


  def visitNull(): JsValue = JsNull

  val visitFalse: JsValue = JsBoolean(false)

  val visitTrue: JsValue = JsBoolean(true)

  def visitFloat64StringParts(cs: CharSequence, decIndex: Int, expIndex: Int): JsValue = {
    JsNumber(BigDecimal(cs.toString))
  }

  override def visitFloat64(
    d: Double
  ): JsValue = {
    JsNumber(WeePickle.ToBigDecimal.visitFloat64(d))
  }

  override def visitInt64(
    l: Long
  ): JsValue = {
    JsNumber(WeePickle.ToBigDecimal.visitInt64(l))
  }


  def visitString(cs: CharSequence): JsValue = JsString(cs.toString)

  implicit val FromJsValue: From[JsValue] = new From[JsValue] {
    def transform0[Out](in: JsValue, out: Visitor[_, Out]): Out = PlayJson.transform(in, out)
  }

  implicit val ToJsValue: To[JsValue] = new To.Delegate[JsValue, JsValue](this)

}
