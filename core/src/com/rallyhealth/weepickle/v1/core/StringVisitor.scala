package com.rallyhealth.weepickle.v0.core

object StringVisitor extends SimpleVisitor[Nothing, Any] {
  def expectedMsg = "expected string"
  override def visitString(s: CharSequence, index: Int): CharSequence = s
}
