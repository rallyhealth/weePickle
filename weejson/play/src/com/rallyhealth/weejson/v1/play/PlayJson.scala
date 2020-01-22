package com.rallyhealth.weejson.v1.play

import play.api.libs.json._
import com.rallyhealth.weepickle.v1.core.{ArrVisitor, ObjVisitor, Visitor}
import com.rallyhealth.weepickle.v1.WeePickle._

import scala.collection.mutable.ArrayBuffer

object PlayJson extends com.rallyhealth.weejson.v1.AstTransformer[JsValue] {
  def transform[T](j: JsValue, f: Visitor[_, T]): T = j match{
    case JsArray(xs) => transformArray(f, xs)
    case JsBoolean(b) => if (b) f.visitTrue() else f.visitFalse()
    case JsNull => f.visitNull()
    case JsNumber(d) => f.visitFloat64String(d.toString)
    case JsObject(kvs) => transformObject(f, kvs)
    case JsString(s) => f.visitString(s)
  }
  def visitArray(length: Int): ArrVisitor[JsValue, JsValue] = new AstArrVisitor[Array](JsArray(_))

  def visitObject(length: Int): ObjVisitor[JsValue, JsValue] = new AstObjVisitor[ArrayBuffer[(String, JsValue)]](JsObject(_))

  def visitNull(): JsValue = JsNull

  def visitFalse(): JsValue = JsBoolean(false)

  def visitTrue(): JsValue = JsBoolean(true)

  def visitFloat64StringParts(cs: CharSequence, decIndex: Int, expIndex: Int): JsValue = {
    JsNumber(BigDecimal(cs.toString))
  }

  def visitString(cs: CharSequence): JsValue = JsString(cs.toString)

  implicit val JsValueWriter: Writer[JsValue] = new Writer[JsValue] {
    def write0[R](out: Visitor[_, R], v: JsValue): R = PlayJson.transform(v, out)
  }

  implicit val JsValueReader: Reader[JsValue] = new Reader.Delegate[JsValue, JsValue](this)

}
