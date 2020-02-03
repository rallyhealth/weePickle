package com.rallyhealth.weepickle.v1.core

import scala.util.Try

/**
  * Input data, ready to push through a Visitor.
  */
trait FromInput {
  @throws[Throwable]("if things go wrong")
  def transform[T](to: Visitor[_, T]): T
  def validate[T](to: Visitor[_, T]): Try[T] = Try(transform(to))
}
