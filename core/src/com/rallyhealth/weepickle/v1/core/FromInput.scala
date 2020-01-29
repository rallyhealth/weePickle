package com.rallyhealth.weepickle.v1.core

/**
  * Input data, ready to push through a Visitor.
  */
trait FromInput {

  def transform[T](to: Visitor[_, T]): T
}
