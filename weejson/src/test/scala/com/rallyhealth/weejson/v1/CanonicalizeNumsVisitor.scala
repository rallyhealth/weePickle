package com.rallyhealth.weejson.v1

import com.rallyhealth.weepickle.v1.core.Visitor.{ArrDelegate, ObjDelegate}
import com.rallyhealth.weepickle.v1.core.{ArrVisitor, ObjVisitor, Visitor}

import scala.language.implicitConversions

object CanonicalizeNumsVisitor {

  implicit class RichVisitor[T, J](val visitor: Visitor[T, J]) extends AnyVal {

    def canonicalize: Visitor[T, J] = new CanonicalizeNumsVisitor[T, J](visitor)
  }
}

/**
  * Forces numbers to their smallest possible representations
  * for better equivalence in test assertions.
  */
class CanonicalizeNumsVisitor[T, J](underlying: Visitor[T, J])
  extends Visitor.Delegate[T, J](underlying) {
  import CanonicalizeNumsVisitor.RichVisitor

  override def visitObject(length: Int): ObjVisitor[T, J] =
    new ObjDelegate[T, J](underlying.visitObject(length).narrow) {

      override def visitKey(): Visitor[_, _] = super.visitKey().canonicalize

      override def subVisitor: Visitor[Nothing, Any] = super.subVisitor.asInstanceOf[Visitor[Any, Any]].canonicalize
    }

  override def visitArray(length: Int): ArrVisitor[T, J] =
    new ArrDelegate[T, J](underlying.visitArray(length).narrow) {
      override def subVisitor: Visitor[Nothing, Any] = super.subVisitor.asInstanceOf[Visitor[Any, Any]].canonicalize
    }

  override def visitFloat32(f: Float): J = {
    val l = f.toLong
    if (l == f) visitInt64(l)
    else super.visitFloat32(f)
  }

  override def visitFloat64(d: Double): J = {
    val l = d.toLong
    val f = d.toFloat
    if (l == d) visitInt64(l)
    else if (f == d) visitFloat32(f)
    else super.visitFloat64(d)
  }

  override def visitInt64(l: Long): J = {
    val i = l.toInt
    if (i == l) visitInt32(i)
    else super.visitFloat64(l)
  }

  override def visitFloat64String(s: String): J = {
    val d = BigDecimal(s)
    if (d.isValidLong) visitInt64(d.longValue)
    else if (d.isDecimalDouble) visitFloat64(d.doubleValue)
    else super.visitFloat64String(s)
  }

  override def visitFloat64StringParts(
    cs: CharSequence,
    decIndex: Int,
    expIndex: Int
  ): J = {
    val d = BigDecimal(cs.toString)
    if (d.isValidLong) visitInt64(d.longValue)
    else if (d.isDecimalDouble) visitFloat64(d.doubleValue)
    else super.visitFloat64StringParts(cs, decIndex, expIndex)
  }
}
