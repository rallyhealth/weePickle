package com.rallyhealth.weejson.v1.play

import play.api.libs.json._
import com.rallyhealth.weepickle.v1.core.{ArrVisitor, ObjVisitor, Visitor}
import com.rallyhealth.weepickle.v1.WeePickle._

import scala.collection.mutable.ArrayBuffer

object PlayJson extends com.rallyhealth.weejson.v1.AstTransformer[JsValue] {
  def transform[T](i: JsValue, to: Visitor[_, T]): T = i match {
    case JsArray(xs)   => transformArray(to, xs)
    case JsBoolean(b)  => if (b) to.visitTrue() else to.visitFalse()
    case JsNull        => to.visitNull()
    case JsNumber(d)   => to.visitFloat64String(d.toString)
    case JsObject(kvs) => transformObject(to, kvs)
    case JsString(s)   => to.visitString(s)
  }
  def visitArray(length: Int): ArrVisitor[JsValue, JsValue] = new AstArrVisitor[Array](JsArray(_))

  def visitObject(length: Int): ObjVisitor[JsValue, JsValue] =
    new AstObjVisitor[ArrayBuffer[(String, JsValue)]](JsObject(_))

  def visitNull(): JsValue = JsNull

  def visitFalse(): JsValue = JsBoolean(false)

  def visitTrue(): JsValue = JsBoolean(true)

  def visitFloat64StringParts(cs: CharSequence, decIndex: Int, expIndex: Int): JsValue = {
    JsNumber(BigDecimal(cs.toString))
  }

  def visitString(cs: CharSequence): JsValue = JsString(cs.toString)

  implicit val FromJsValue: From[JsValue] = new From[JsValue] {
    def transform0[Out](in: JsValue, out: Visitor[_, Out]): Out = PlayJson.transform(in, out)
  }

  implicit val ToJsValue: To[JsValue] = new To.Delegate[JsValue, JsValue](this)

}
