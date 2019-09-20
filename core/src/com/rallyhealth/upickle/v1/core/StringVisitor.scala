package com.rallyhealth.upickle.v1.core

object StringVisitor extends SimpleVisitor[Nothing, Any] {
  def expectedMsg = "expected string"
  override def visitString(s: CharSequence, index: Int) = s
}
