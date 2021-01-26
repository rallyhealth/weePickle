package com.rallyhealth.weepickle.v1.core

class RootArrVisitor(root: Visitor[_, _]) extends ArrVisitor[Any, Nothing] {

  override def subVisitor: Visitor[_, _] = root

  override def visitValue(v: Any): Unit = ()

  override def visitEnd(): Nothing = throw new IllegalStateException("programming error: illegal call to RootArrVisitor")
}
