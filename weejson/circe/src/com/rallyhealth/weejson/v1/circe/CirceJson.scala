package com.rallyhealth.weejson.v1.circe

import com.rallyhealth.weepickle.v1.core.{ArrVisitor, ObjVisitor, Visitor}
import io.circe.{Json, JsonNumber}

import scala.collection.mutable.ArrayBuffer
object CirceJson extends com.rallyhealth.weejson.v1.AstTransformer[Json]{

  override def transform[T](j: Json, f: Visitor[_, T]) = j.fold(
    f.visitNull(),
    if (_) f.visitTrue() else f.visitFalse(),
    n => f.visitFloat64(n.toDouble),
    (s: String) => f.visitString(s),
    arr => transformArray(f, arr),
    obj => transformObject(f, obj.toList)
  )

  def visitArray(length: Int): ArrVisitor[Json, Json] = new AstArrVisitor[Vector](x => Json.arr(x:_*))

  def visitObject(length: Int): ObjVisitor[Json, Json] = new AstObjVisitor[ArrayBuffer[(String, Json)]](vs => Json.obj(vs.toSeq:_*))

  def visitNull(): Json = Json.Null

  def visitFalse(): Json = Json.False

  def visitTrue(): Json = Json.True

  def visitFloat64StringParts(cs: CharSequence, decIndex: Int, expIndex: Int): Json = {
    Json.fromJsonNumber(
      if (decIndex == -1 && expIndex == -1) JsonNumber.fromIntegralStringUnsafe(cs.toString)
      else JsonNumber.fromDecimalStringUnsafe(cs.toString)
    )
  }

  def visitString(cs: CharSequence): Json = Json.fromString(cs.toString)
}
