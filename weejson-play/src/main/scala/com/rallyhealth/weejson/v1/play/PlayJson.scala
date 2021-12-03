package com.rallyhealth.weejson.v1.play

import com.rallyhealth.weepickle.v1.WeePickle
import com.rallyhealth.weepickle.v1.WeePickle._
import com.rallyhealth.weepickle.v1.core.{ArrVisitor, ObjVisitor, StringVisitor, Visitor}
import play.api.libs.json._

import java.util.{LinkedHashMap => JLinkedHashMap}
import scala.jdk.CollectionConverters._

object PlayJson extends PlayJson {

  override def visitTrue(): JsValue = JsValueSingletons.jsTrue

  override def visitFalse(): JsValue = JsValueSingletons.jsFalse
}

class PlayJson extends com.rallyhealth.weejson.v1.AstTransformer[JsValue] {
  def transform[T](i: JsValue, to: Visitor[_, T]): T = (i: @unchecked) match {
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
    new ObjVisitor[JsValue, JsValue] {
      private[this] var key: String = null
      private[this] val vs = new JLinkedHashMap[String, JsValue](math.max(length, 2)).asScala

      override def visitKey(): Visitor[_, _] = StringVisitor

      override def visitKeyValue(v: Any): Unit = key = v.toString

      override def subVisitor: Visitor[_, _] = PlayJson.this

      override def visitValue(v: JsValue): Unit = vs.put(key, v)

      override def visitEnd(): JsValue = {
        if (vs.isEmpty) JsValueSingletons.jsObjectEmpty
        else JsObject(vs)
      }
    }

  def visitNull(): JsValue = JsNull

  def visitFalse(): JsValue = PlayJson.visitFalse()

  def visitTrue(): JsValue = PlayJson.visitTrue()

  def visitFloat64StringParts(cs: CharSequence, decIndex: Int, expIndex: Int): JsValue = {
    JsNumber(BigDecimal(cs.toString))
  }

  override def visitFloat64(d: Double): JsValue = {
    JsNumber(WeePickle.ToBigDecimal.visitFloat64(d))
  }

  override def visitInt64(l: Long): JsValue = {
    JsNumber(WeePickle.ToBigDecimal.visitInt64(l))
  }

  def visitString(cs: CharSequence): JsValue = JsString(cs.toString)

  implicit def FromJsValue: From[JsValue] = new From[JsValue] {
    def transform0[Out](in: JsValue, out: Visitor[_, Out]): Out = PlayJson.this.transform(in, out)
  }

  implicit def ToJsValue: To[JsValue] = new To.Delegate[JsValue, JsValue](this)
}
