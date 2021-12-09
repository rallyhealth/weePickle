package com.rallyhealth.weejson.v1.play

import com.rallyhealth.weejson.v1.AstTransformer
import com.rallyhealth.weepickle.v1.WeePickle
import com.rallyhealth.weepickle.v1.WeePickle._
import com.rallyhealth.weepickle.v1.core.{ArrVisitor, ObjVisitor, StringVisitor, Visitor}
import play.api.libs.json._

import java.util.{LinkedHashMap => JLinkedHashMap}
import scala.collection.compat.immutable.ArraySeq
import scala.collection.mutable.ArrayBuffer
import scala.jdk.CollectionConverters._

object PlayJson extends PlayJson {

  override def visitTrue(): JsValue = JsValueSingletons.jsTrue

  override def visitFalse(): JsValue = JsValueSingletons.jsFalse
}

class PlayJson extends AstTransformer[JsValue] {
  def transform[T](i: JsValue, to: Visitor[_, T]): T = (i: @unchecked) match {
    case JsArray(xs)   => transformArray(to, xs)
    case JsBoolean(b)  => if (b) to.visitTrue() else to.visitFalse()
    case JsNull        => to.visitNull()
    case JsNumber(d)   => to.visitFloat64String(d.toString)
    case JsObject(kvs) => transformObject(to, kvs)
    case JsString(s)   => to.visitString(s)
  }

  def visitArray(length: Int): ArrVisitor[JsValue, JsValue] = {
    new ArrVisitor[JsValue, JsValue] {
      // initCapacity=4 covers 90% of real-world objs. Faster overall. (JsValueBench)
      private[this] val buf = new ArrayBuffer[AnyRef](if (length >= 0) length else 4)

      override def subVisitor: Visitor[_, _] = PlayJson.this

      override def visitValue(v: JsValue): Unit = buf += v

      override def visitEnd(): JsValue = {
        if (buf.isEmpty) JsValueSingletons.EmptyJsArray
        else JsArray(ArraySeq.unsafeWrapArray(buf.toArray).asInstanceOf[ArraySeq[JsValue]])
      }
    }
  }

  def visitObject(
    length: Int
  ): ObjVisitor[JsValue, JsValue] =
    new ObjVisitor[JsValue, JsValue] {
      private[this] var key: String = _
      // initCapacity=4 covers 88% of real-world objs. Faster overall. (JsValueBench)
      private[this] val buf = new ArrayBuffer[(String, JsValue)](if (length >= 0) length else 4)

      override def visitKey(): Visitor[_, _] = StringVisitor

      override def visitKeyValue(v: Any): Unit = key = v.toString

      override def subVisitor: Visitor[_, _] = PlayJson.this

      override def visitValue(v: JsValue): Unit = buf += (key -> v)

      override def visitEnd(): JsValue = {
        if (buf.isEmpty) JsValueSingletons.EmptyJsObject
        else if (buf.size <= 4) JsObject(buf.toMap) // preserves order
        else JsObject(new JLinkedHashMap[String, JsValue](buf.size).asScala ++= buf)
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
