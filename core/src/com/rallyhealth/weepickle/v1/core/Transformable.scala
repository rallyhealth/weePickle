package com.rallyhealth.weepickle.v1.core

/**
  * Input data, ready to push through a Visitor.
  */
trait Transformable {

  def transform[T](into: Visitor[_, T]): T
}
