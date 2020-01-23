package com.rallyhealth.weepickle.v1.core

/**
  * Input data, ready to push through a Visitor.
  */
trait Transmittable {

  def transmit[T](into: Visitor[_, T]): T
}
