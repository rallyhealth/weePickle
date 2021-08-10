package com.rallyhealth.weejson.v1.upicklecompat

import com.rallyhealth.weepickle.v1.core.{ArrVisitor, ObjVisitor, Visitor}

import java.time.Instant

class UWVisitor[T, J](visitor: upickle.core.Visitor[T, J]) extends Visitor[T, J] {

  override def visitArray(length: Int): ArrVisitor[T, J] = {
    val arr = visitor.visitArray(length, -1)
    new ArrVisitor[T, J] {
      override def subVisitor: Visitor[_, _] = new UWVisitor(arr.subVisitor)

      override def visitValue(v: T): Unit = arr.visitValue(v, -1)

      override def visitEnd(): J = arr.visitEnd(-1)
    }
  }

  override def visitObject(length: Int): ObjVisitor[T, J] = {
    val obj = visitor.visitObject(length, -1)
    new ObjVisitor[T, J] {
      override def visitKey(): Visitor[_, _] = new UWVisitor(obj.visitKey(-1))

      override def visitKeyValue(v: Any): Unit = obj.visitKeyValue(v)

      override def subVisitor: Visitor[_, _] = new UWVisitor(obj.subVisitor)

      override def visitValue(v: T): Unit = obj.visitValue(v, -1)

      override def visitEnd(): J = obj.visitEnd(-1)
    }
  }

  override def visitNull(): J = visitor.visitNull(-1)

  override def visitFalse(): J = visitor.visitFalse(-1)

  override def visitTrue(): J = visitor.visitTrue(-1)

  override def visitFloat64StringParts(cs: CharSequence, decIndex: Int, expIndex: Int): J = visitor.visitFloat64StringParts(cs, decIndex, expIndex, -1)

  override def visitFloat64(d: Double): J = visitor.visitFloat64(d, -1)

  override def visitFloat32(d: Float): J = visitor.visitFloat32(d, -1)

  override def visitInt32(i: Int): J = visitor.visitInt32(i, -1)

  override def visitInt64(l: Long): J = visitor.visitInt64(l, -1)

  override def visitUInt64(ul: Long): J = visitor.visitUInt64(ul, -1)

  override def visitFloat64String(s: String): J = visitor.visitFloat64String(s, -1)

  override def visitString(cs: CharSequence): J = visitor.visitString(cs, -1)

  override def visitChar(c: Char): J = visitor.visitChar(c, -1)

  override def visitBinary(bytes: Array[Byte], offset: Int, len: Int): J = visitor.visitBinary(bytes, offset, len, -1)

  override def visitExt(tag: Byte, bytes: Array[Byte], offset: Int, len: Int): J = visitor.visitExt(tag, bytes, offset, len, -1)

  override def visitTimestamp(instant: Instant): J = visitor.visitString(instant.toString, -1)
}
