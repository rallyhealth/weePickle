package com.rallyhealth.weejson.v1.upicklecompat

import com.rallyhealth.weepickle.v1.core.Visitor
import upickle.core
import upickle.core.{ArrVisitor, ObjVisitor}

class WUVisitor[T, J](weepickle: Visitor[T, J]) extends upickle.core.Visitor[T, J] {

  override def visitArray(length: Int, index: Int): ArrVisitor[T, J] = {
    val arr = weepickle.visitArray(length)
    new ArrVisitor[T, J] {
      override def subVisitor: core.Visitor[_, _] = new WUVisitor(arr.subVisitor)

      override def visitValue(v: T, index: Int): Unit = arr.visitValue(v)

      override def visitEnd(index: Int): J = arr.visitEnd()
    }
  }

  override def visitObject(length: Int, jsonableKeys: Boolean, index: Int): ObjVisitor[T, J] = {
    val obj = weepickle.visitObject(length)

    new ObjVisitor[T, J] {
      override def visitKey(index: Int): core.Visitor[_, _] = new WUVisitor(obj.visitKey())

      override def visitKeyValue(v: Any): Unit = obj.visitKeyValue(v)

      override def subVisitor: core.Visitor[_, _] = new WUVisitor(obj.subVisitor)

      override def visitValue(v: T, index: Int): Unit = obj.visitValue(v)

      override def visitEnd(index: Int): J = obj.visitEnd()
    }
  }

  override def visitNull(index: Int): J = weepickle.visitNull()

  override def visitFalse(index: Int): J = weepickle.visitFalse()

  override def visitTrue(index: Int): J = weepickle.visitTrue()

  override def visitFloat64StringParts(s: CharSequence, decIndex: Int, expIndex: Int, index: Int): J = weepickle.visitFloat64StringParts(s, decIndex, expIndex)

  override def visitFloat64(d: Double, index: Int): J = weepickle.visitFloat64(d)

  override def visitFloat32(d: Float, index: Int): J = weepickle.visitFloat32(d)

  override def visitInt32(i: Int, index: Int): J = weepickle.visitInt32(i)

  override def visitInt64(i: Long, index: Int): J = weepickle.visitInt64(i)

  override def visitUInt64(i: Long, index: Int): J = weepickle.visitUInt64(i)

  override def visitFloat64String(s: String, index: Int): J = weepickle.visitFloat64String(s)

  override def visitString(s: CharSequence, index: Int): J = weepickle.visitString(s)

  override def visitChar(s: Char, index: Int): J = weepickle.visitChar(s)

  override def visitBinary(bytes: Array[Byte], offset: Int, len: Int, index: Int): J = weepickle.visitBinary(bytes, offset, len)

  override def visitExt(tag: Byte, bytes: Array[Byte], offset: Int, len: Int, index: Int): J = weepickle.visitExt(tag, bytes, offset, len)
}
