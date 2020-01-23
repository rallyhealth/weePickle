package com.rallyhealth.weejson.v1.json4s

import org.json4s.JsonAST._
import com.rallyhealth.weepickle.v1.core.{ArrVisitor, ObjVisitor, Visitor}

object Json4sJson extends Json4sJson(false, false)

class Json4sJson(useBigDecimalForDouble: Boolean, useBigIntForLong: Boolean)
    extends com.rallyhealth.weejson.v1.AstTransformer[JValue] {
  def transform[T](j: JValue, f: Visitor[_, T]) = j match {
    case JArray(xs)   => transformArray(f, xs)
    case JBool(b)     => if (b) f.visitTrue() else f.visitFalse()
    case JDecimal(d)  => f.visitFloat64String(d.toString)
    case JDouble(d)   => f.visitFloat64(d)
    case JInt(i)      => f.visitFloat64StringParts(i.toString, -1, -1)
    case JLong(l)     => f.visitFloat64StringParts(l.toString, -1, -1)
    case JNothing     => f.visitNull()
    case JNull        => f.visitNull()
    case JObject(kvs) => transformObject(f, kvs)
    case JSet(xs)     => transformArray(f, xs)
    case JString(s)   => f.visitString(s)
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
