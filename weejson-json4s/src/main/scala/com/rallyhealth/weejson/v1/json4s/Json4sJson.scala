package com.rallyhealth.weejson.v1.json4s

import org.json4s.JsonAST._
import com.rallyhealth.weepickle.v1.core.{ArrVisitor, ObjVisitor, Visitor}

object Json4sJson extends Json4sJson(false, false)

class Json4sJson(useBigDecimalForDouble: Boolean, useBigIntForLong: Boolean)
    extends com.rallyhealth.weejson.v1.AstTransformer[JValue] {
  def transform[T](i: JValue, to: Visitor[_, T]) = i match {
    case JArray(xs)   => transformArray(to, xs)
    case JBool(b)     => if (b) to.visitTrue() else to.visitFalse()
    case JDecimal(d)  => to.visitFloat64String(d.toString)
    case JDouble(d)   => to.visitFloat64(d)
    case JInt(i)      => to.visitFloat64StringParts(i.toString, -1, -1)
    case JLong(l)     => to.visitFloat64StringParts(l.toString, -1, -1)
    case JNothing     => to.visitNull()
    case JNull        => to.visitNull()
    case JObject(kvs) => transformObject(to, kvs)
    case JSet(xs)     => transformArray(to, xs)
    case JString(s)   => to.visitString(s)
  }

  def visitArray(length: Int): ArrVisitor[JValue, JValue] = new AstArrVisitor[List](x => JArray(x))
  def visitObject(length: Int): ObjVisitor[JValue, JValue] = new AstObjVisitor[List[(String, JValue)]](JObject(_))

  def visitNull(): JValue = JNull

  def visitFalse(): JValue = JBool(false)

  def visitTrue(): JValue = JBool(true)

  override def visitFloat64(d: Double): JValue = JDouble(d)

  def visitFloat64StringParts(cs: CharSequence, decIndex: Int, expIndex: Int): JValue = {
    if (decIndex == -1 && expIndex == -1) {
      if (useBigIntForLong) JInt(BigInt(cs.toString))
      else JLong(com.rallyhealth.weepickle.v1.core.Util.parseLong(cs, 0, cs.length))
    } else {
      if (useBigDecimalForDouble) JDecimal(BigDecimal(cs.toString))
      else JDouble(cs.toString.toDouble)
    }
  }

  def visitString(cs: CharSequence): JValue = JString(cs.toString)
}
