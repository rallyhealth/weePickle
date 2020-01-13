package com.rallyhealth.weepickle.v1.core

import com.rallyhealth.weepickle.v0.core.Visitor.{ArrDelegate, ObjDelegate}
import com.rallyhealth.weepickle.v0.core.{ArrVisitor, ObjVisitor, Visitor}

/**
  * Notifies the `callback` of top-level results returned by the [[delegate]] visitor.
  *
  * Useful from extracting return values from visitors that are used in side-effecting positions.
  */
class CallbackVisitor[T, J](delegate: Visitor[T, J])(callback: J => Unit) extends Visitor.Delegate[T, J](delegate) {

  private def emitFluently(j: J): J = {
    if (j != null) callback(j)
    j
  }

  override def visitObject(length: Int, index: Int): ObjVisitor[T, J] = new ObjDelegate(super.visitObject(length, index)) {
    override def visitEnd(index: Int): J = emitFluently(super.visitEnd(index))
  }

  override def visitArray(length: Int, index: Int): ArrVisitor[T, J] = new ArrDelegate(super.visitArray(length, index)) {
    override def visitEnd(index: Int): J = emitFluently(super.visitEnd(index))
  }

  override def visitNull(index: Int): J = emitFluently(super.visitNull(index))

  override def visitTrue(index: Int): J = emitFluently(super.visitTrue(index))

  override def visitFalse(index: Int): J = emitFluently(super.visitFalse(index))

  override def visitString(s: CharSequence, index: Int): J = emitFluently(super.visitString(s, index))

  override def visitFloat64StringParts(s: CharSequence, decIndex: Int, expIndex: Int, index: Int): J = emitFluently(super.visitFloat64StringParts(s, decIndex, expIndex, index))

  override def visitFloat64(d: Double, index: Int): J = emitFluently(super.visitFloat64(d, index))

  override def visitFloat32(d: Float, index: Int): J = emitFluently(super.visitFloat32(d, index))

  override def visitInt32(i: Int, index: Int): J = emitFluently(super.visitInt32(i, index))

  override def visitInt64(i: Long, index: Int): J = emitFluently(super.visitInt64(i, index))

  override def visitUInt64(i: Long, index: Int): J = emitFluently(super.visitUInt64(i, index))

  override def visitFloat64String(s: String, index: Int): J = emitFluently(super.visitFloat64String(s, index))

  override def visitChar(s: Char, index: Int): J = emitFluently(super.visitChar(s, index))

  override def visitBinary(bytes: Array[Byte], offset: Int, len: Int, index: Int): J = emitFluently(super.visitBinary(bytes, offset, len, index))

  override def visitExt(tag: Byte, bytes: Array[Byte], offset: Int, len: Int, index: Int): J = emitFluently(super.visitExt(tag, bytes, offset, len, index))
}
