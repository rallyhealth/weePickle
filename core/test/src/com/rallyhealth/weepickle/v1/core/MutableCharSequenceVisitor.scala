package com.rallyhealth.weepickle.v1.core

import java.nio.CharBuffer

/**
  * For testing that the delegate visitor doesn't make assumptions
  * about the immutability of the CharSequences it receives.
  */
class MutableCharSequenceVisitor[T, J](v: Visitor[T, J]) extends Visitor.Delegate[T, J](v) {

  private def withMutableString[R](s: CharSequence)(f: CharSequence => R): R = {
    val buf = s.toString.toCharArray
    val r = f(CharBuffer.wrap(buf))
    // Clear the buffer with something recognizable.
    for (i <- 0 until buf.length) {
      buf(i) = 'M' // for Mutable
    }
    r
  }

  override def visitArray(length: Int): ArrVisitor[T, J] = {
    val arr = super.visitArray(length)
    new ArrVisitor[T, J] {
      override def subVisitor: Visitor[_, _] = new MutableCharSequenceVisitor(arr.subVisitor)

      override def visitValue(v: T): Unit = arr.visitValue(v)

      override def visitEnd(): J = arr.visitEnd()
    }
  }

  override def visitObject(length: Int): ObjVisitor[T, J] = {
    val obj = super.visitObject(length)
    new ObjVisitor[T, J] {
      override def visitKey(): Visitor[_, _] = obj.visitKey()

      override def visitKeyValue(v: Any): Unit = obj.visitKeyValue(v)

      override def subVisitor: Visitor[_, _] = new MutableCharSequenceVisitor(obj.subVisitor)

      override def visitValue(v: T): Unit = obj.visitValue(v)

      override def visitEnd(): J = obj.visitEnd()
    }
  }

  override def visitString(cs: CharSequence): J = withMutableString(cs) { mut =>
    super.visitString(mut)
  }

  override def visitFloat64StringParts(cs: CharSequence, decIndex: Int, expIndex: Int): J = withMutableString(cs) { mut =>
    super.visitFloat64StringParts(mut, decIndex, expIndex)
  }
}
