package com.rallyhealth.weepickle.v1

import com.rallyhealth.weepickle.v1.core.{ArrVisitor, NoOpVisitor, ObjArrVisitor, ObjVisitor, Visitor}

/**
  * Returns the max depth encountered.
  * Useful for creating valid test inputs that can be sent to a parser that throws at high depth.
  */
class MaxDepthVisitor extends NoOpVisitor[Int](0) {

  private[this] var maxDepth = 0
  private[this] var currentDepth = 0

  private def inc() = {
    currentDepth += 1
    maxDepth = math.max(maxDepth, currentDepth)
  }

  private def dec() = {
    currentDepth -= 1
  }

  private trait ObjArrBase {
    self: ObjArrVisitor[Any, Int] =>

    override def subVisitor: Visitor[_, _] = {
      inc()
      MaxDepthVisitor.this.map(_ => maxDepth)
    }

    override def visitValue(v: Any): Unit = dec()

    override def visitEnd(): Int = maxDepth
  }

  override def visitObject(length: Int): ObjVisitor[Any, Int] = new ObjVisitor[Any, Int] with ObjArrBase {
    override def visitKey(): Visitor[_, _] = {
      inc()
      NoOpVisitor
    }

    override def visitKeyValue(v: Any): Unit = dec()
  }

  override def visitArray(length: Int): ArrVisitor[Any, Int] = new ArrVisitor[Any, Int] with ObjArrBase
}
