package com.rallyhealth.weepickle.v1.core
import java.time.Instant

/**
  * A visitor that throws an error for all the visit methods which it does not define,
  * letting you only define the handlers you care about.
  */
trait SimpleVisitor[-T, +J] extends Visitor[T, J] {
  def expectedMsg: String
  def visitNull(): J = null.asInstanceOf[J]
  def visitTrue(): J =  throw new Abort(expectedMsg + " got boolean")
  def visitFalse(): J = throw new Abort(expectedMsg + " got boolean")

  def visitString(cs: CharSequence): J = {
    throw new Abort(expectedMsg + " got string")
  }
  def visitFloat64StringParts(cs: CharSequence, decIndex: Int, expIndex: Int): J = {
    throw new Abort(expectedMsg + " got number")
  }

  def visitObject(length: Int): ObjVisitor[T, J] = {
    throw new Abort(expectedMsg + " got dictionary")
  }
  def visitArray(length: Int): ArrVisitor[T, J] = {
    throw new Abort(expectedMsg + " got sequence")
  }

  def visitFloat64(d: Double): J = throw new Abort(expectedMsg + " got float64")

  def visitFloat32(d: Float): J = throw new Abort(expectedMsg + " got float32")

  def visitInt32(i: Int): J = throw new Abort(expectedMsg + " got int32")

  def visitInt64(l: Long): J = throw new Abort(expectedMsg + " got int64")

  def visitUInt64(ul: Long): J = throw new Abort(expectedMsg + " got uint64")

  def visitFloat64String(s: String): J = throw new Abort(expectedMsg + " got float64 string")

  def visitChar(c: Char): J = throw new Abort(expectedMsg + " got char")

  def visitBinary(bytes: Array[Byte], offset: Int, len: Int): J = throw new Abort(expectedMsg + " got binary")

  def visitExt(tag: Byte, bytes: Array[Byte], offset: Int, len: Int): J = throw new Abort(expectedMsg + " got ext")

  def visitTimestamp(instant: Instant): J = throw new Abort(expectedMsg + " got timestamp")
}
