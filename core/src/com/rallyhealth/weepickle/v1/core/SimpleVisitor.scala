package com.rallyhealth.weepickle.v1.core
import java.time.Instant

/**
  * A visitor that throws an error for all the visit methods which it does not define,
  * letting you only define the handlers you care about.
  */
trait SimpleVisitor[-T, +V] extends Visitor[T, V] {
  def expectedMsg: String
  def visitNull(index: Int): V = null.asInstanceOf[V]
  def visitTrue(index: Int): V =  throw new Abort(expectedMsg + " got boolean", index)
  def visitFalse(index: Int): V = throw new Abort(expectedMsg + " got boolean", index)

  def visitString(s: CharSequence, index: Int): V = {
    throw new Abort(expectedMsg + " got string", index)
  }
  def visitFloat64StringParts(s: CharSequence, decIndex: Int, expIndex: Int, index: Int): V = {
    throw new Abort(expectedMsg + " got number", index)
  }

  def visitObject(length: Int, index: Int): ObjVisitor[T, V] = {
    throw new Abort(expectedMsg + " got dictionary", index)
  }
  def visitArray(length: Int, index: Int): ArrVisitor[T, V] = {
    throw new Abort(expectedMsg + " got sequence", index)
  }

  def visitFloat64(d: Double, index: Int): V = throw new Abort(expectedMsg + " got float64", index)

  def visitFloat32(d: Float, index: Int): V = throw new Abort(expectedMsg + " got float32", index)

  def visitInt32(i: Int, index: Int): V = throw new Abort(expectedMsg + " got int32", index)

  def visitInt64(i: Long, index: Int): V = throw new Abort(expectedMsg + " got int64", index)

  def visitUInt64(i: Long, index: Int): V = throw new Abort(expectedMsg + " got uint64", index)

  def visitFloat64String(s: String, index: Int): V = throw new Abort(expectedMsg + " got float64 string", index)

  def visitChar(s: Char, index: Int): V = throw new Abort(expectedMsg + " got char", index)

  def visitBinary(bytes: Array[Byte], offset: Int, len: Int, index: Int): V = throw new Abort(expectedMsg + " got binary", index)

  def visitExt(tag: Byte, bytes: Array[Byte], offset: Int, len: Int, index: Int): V = throw new Abort(expectedMsg + " got ext", index)

  def visitTimestamp(instant: Instant, index: Int): V = throw new Abort(expectedMsg + " got timestamp", index)
}
