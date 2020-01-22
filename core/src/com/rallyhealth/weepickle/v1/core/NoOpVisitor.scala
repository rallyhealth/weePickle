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
  def visitArray(length: Int): ArrVisitor[Any, J] = new ArrVisitor[Any, J] {
    def subVisitor = NoOpVisitor.this
    def visitValue(v: Any): Unit = ()
    def visitEnd(): J = returnValue
  }
  def visitObject(length: Int): ObjVisitor[Any, J] = new ObjVisitor[Any, J] {
    def subVisitor = NoOpVisitor.this
    def visitKey(): Visitor[_, _] = NoOpVisitor
    def visitKeyValue(s: Any): Unit = ()
    def visitValue(v: Any): Unit = ()
    def visitEnd(): J = returnValue
  }

  def visitNull(): J = returnValue
  def visitFalse(): J = returnValue
  def visitTrue(): J = returnValue
  def visitFloat64StringParts(cs: CharSequence, decIndex: Int, expIndex: Int): J = returnValue
  def visitString(cs: CharSequence): J = returnValue

  def visitFloat64(d: Double): J = returnValue

  def visitFloat32(d: Float): J = returnValue

  def visitInt32(i: Int): J = returnValue

  def visitInt64(l: Long): J = returnValue
  def visitUInt64(ul: Long): J = returnValue

  def visitFloat64String(s: String): J = returnValue

  def visitBinary(bytes: Array[Byte], offset: Int, len: Int): J = returnValue

  def visitExt(tag: Byte, bytes: Array[Byte], offset: Int, len: Int): J = returnValue

  def visitChar(c: Char): J = returnValue

  def visitTimestamp(instant: Instant): J = returnValue
}
