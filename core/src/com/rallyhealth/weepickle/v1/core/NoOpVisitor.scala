package com.rallyhealth.weepickle.v1.core
import java.time.Instant

/**
  * NoOpVisitor discards all JSON AST information.
  *
  * This is the simplest possible visitor. It could be useful for
  * checking JSON for correctness (via parsing) without worrying about
  * saving the data.
  *
  * It will always return Unit on any successful parse, no matter the
  * content.
  */
object NoOpVisitor extends NoOpVisitor[Unit](())

/**
  * Visitor returning null for all instances.
  * Useful in combination with side-effecting visitors,
  * since null is a valid subtype of AnyRef.
  */
object NullVisitor extends NoOpVisitor[Null](null)

class NoOpVisitor[J](returnValue: J) extends Visitor[Any, J] {
  def visitArray(length: Int, index: Int): ArrVisitor[Any, J] = new ArrVisitor[Any, J] {
    def subVisitor = NoOpVisitor.this
    def visitValue(v: Any, index: Int): Unit = ()
    def visitEnd(index: Int): J = returnValue
  }
  def visitObject(length: Int, index: Int): ObjVisitor[Any, J] = new ObjVisitor[Any, J] {
    def subVisitor = NoOpVisitor.this
    def visitKey(index: Int) = NoOpVisitor
    def visitKeyValue(s: Any): Unit = ()
    def visitValue(v: Any, index: Int): Unit = ()
    def visitEnd(index: Int): J = returnValue
  }

  def visitNull(index: Int): J = returnValue
  def visitFalse(index: Int): J = returnValue
  def visitTrue(index: Int): J = returnValue
  def visitFloat64StringParts(s: CharSequence, decIndex: Int, expIndex: Int, index: Int) = returnValue
  def visitString(s: CharSequence, index: Int) = returnValue

  def visitFloat64(d: Double, index: Int) = returnValue

  def visitFloat32(d: Float, index: Int) = returnValue

  def visitInt32(i: Int, index: Int) = returnValue

  def visitInt64(i: Long, index: Int) = returnValue
  def visitUInt64(i: Long, index: Int) = returnValue

  def visitFloat64String(s: String, index: Int) = returnValue

  def visitBinary(bytes: Array[Byte], offset: Int, len: Int, index: Int) = returnValue

  def visitExt(tag: Byte, bytes: Array[Byte], offset: Int, len: Int, index: Int) = returnValue

  def visitChar(s: Char, index: Int) = returnValue

  def visitTimestamp(instant: Instant, index: Int): J = returnValue
}
