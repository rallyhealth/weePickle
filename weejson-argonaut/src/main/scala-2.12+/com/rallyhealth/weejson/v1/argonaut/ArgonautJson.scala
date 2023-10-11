package com.rallyhealth.weejson.v1.argonaut

import argonaut.{Json, JsonNumber, JsonObject}
import com.rallyhealth.weepickle.v1.core.{ArrVisitor, ObjVisitor, Visitor}

import scala.collection.mutable.ArrayBuffer

object ArgonautJson extends com.rallyhealth.weejson.v1.AstTransformer[Json] {
  override def transform[T](i: Json, to: Visitor[_, T]) = i.fold(
    to.visitNull(),
    if (_) to.visitTrue() else to.visitFalse(),
    n =>
      n.toDouble match {
        case Some(d) => to.visitFloat64(d)
        case None    => to.visitFloat64String(n.asJson.toString())
      },
    (s: String) => to.visitString(s),
    arr => transformArray(to, arr),
    obj => transformObject(to, obj.toList)
  )

  def visitArray(length: Int): ArrVisitor[Json, Json] = new AstArrVisitor[List](xs => Json.jArray(xs))
  def visitObject(length: Int): ObjVisitor[Json, Json] = new AstObjVisitor[ArrayBuffer[(String, Json)]](
    vs => Json.jObject(JsonObject.fromIterable(vs))
  )

  def visitNull(): Json = Json.jNull

  def visitFalse(): Json = Json.jFalse

  def visitTrue(): Json = Json.jTrue

  def visitFloat64StringParts(cs: CharSequence, decIndex: Int, expIndex: Int): Json = {
    Json.jNumber(JsonNumber.unsafeDecimal(cs.toString))
  }

  def visitString(cs: CharSequence): Json = Json.jString(cs.toString)
}
