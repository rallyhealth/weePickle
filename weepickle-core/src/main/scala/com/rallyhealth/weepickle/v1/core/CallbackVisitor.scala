package com.rallyhealth.weepickle.v1.core

import java.time.Instant

import com.rallyhealth.weepickle.v1.core.Visitor.{ArrDelegate, ObjDelegate}

/**
  * Notifies the `callback` of top-level results returned by the delegate visitor.
  *
  * Useful from extracting return values from visitors that are used in side-effecting positions.
  */
class CallbackVisitor[T, J](delegate: Visitor[T, J])(callback: J => Unit) extends Visitor.Delegate[T, J](delegate) {

  private def emitFluently(j: J): J = {
    callback(j)
    j
  }

  override def visitObject(length: Int): ObjVisitor[T, J] = new ObjDelegate(super.visitObject(length)) {
    override def visitEnd(): J = emitFluently(super.visitEnd())
  }

  override def visitArray(length: Int): ArrVisitor[T, J] = new ArrDelegate(super.visitArray(length)) {
    override def visitEnd(): J = emitFluently(super.visitEnd())
  }

  override def visitNull(): J = emitFluently(super.visitNull())

  override def visitTrue(): J = emitFluently(super.visitTrue())

  override def visitFalse(): J = emitFluently(super.visitFalse())

  override def visitString(cs: CharSequence): J = emitFluently(super.visitString(cs))

  override def visitFloat64StringParts(cs: CharSequence, decIndex: Int, expIndex: Int): J =
    emitFluently(super.visitFloat64StringParts(cs, decIndex, expIndex))

  override def visitFloat64(d: Double): J = emitFluently(super.visitFloat64(d))

  override def visitFloat32(d: Float): J = emitFluently(super.visitFloat32(d))

  override def visitInt32(i: Int): J = emitFluently(super.visitInt32(i))

  override def visitInt64(l: Long): J = emitFluently(super.visitInt64(l))

  override def visitUInt64(ul: Long): J = emitFluently(super.visitUInt64(ul))

  override def visitFloat64String(s: String): J = emitFluently(super.visitFloat64String(s))

  override def visitChar(c: Char): J = emitFluently(super.visitChar(c))

  override def visitBinary(bytes: Array[Byte], offset: Int, len: Int): J =
    emitFluently(super.visitBinary(bytes, offset, len))

  override def visitExt(tag: Byte, bytes: Array[Byte], offset: Int, len: Int): J =
    emitFluently(super.visitExt(tag, bytes, offset, len))

  override def visitTimestamp(instant: Instant): J = emitFluently(super.visitTimestamp(instant))
}
