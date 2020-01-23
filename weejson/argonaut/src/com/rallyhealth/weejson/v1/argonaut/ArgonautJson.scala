package com.rallyhealth.weejson.v1.argonaut

import argonaut.{Json, JsonNumber, JsonObject}
import com.rallyhealth.weepickle.v1.core.{ArrVisitor, ObjVisitor, Visitor}

import scala.collection.mutable.ArrayBuffer

object ArgonautJson extends com.rallyhealth.weejson.v1.AstTransformer[Json] {
  override def transform[T](j: Json, f: Visitor[_, T]) = j.fold(
    f.visitNull(),
    if (_) f.visitTrue() else f.visitFalse(),
    n =>
      n.toDouble match {
        case Some(d) => f.visitFloat64(d)
        case None    => f.visitFloat64String(n.asJson.toString())
      },
    (s: String) => f.visitString(s),
    arr => transformArray(f, arr),
    obj => transformObject(f, obj.toList)
  )

  def visitArray(length: Int): ArrVisitor[Json, Json] = new AstArrVisitor[List](xs => Json.jArray(xs))
  def visitObject(length: Int): ObjVisitor[Json, Json] = new AstObjVisitor[ArrayBuffer[(String, Json)]](
    vs => Json.jObject(JsonObject.fromTraversableOnce(vs))
  )

  def visitNull(): Json = Json.jNull

  def visitFalse(): Json = Json.jFalse

  def visitTrue(): Json = Json.jTrue

  def visitFloat64StringParts(cs: CharSequence, decIndex: Int, expIndex: Int): Json = {
    Json.jNumber(JsonNumber.unsafeDecimal(cs.toString))
  }

  def visitString(cs: CharSequence): Json = Json.jString(cs.toString)
}
