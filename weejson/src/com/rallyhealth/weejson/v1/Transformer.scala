package com.rallyhealth.weejson.v1

import com.rallyhealth.weepickle.v1.core.Visitor

import scala.util.Try

trait Transformer[I] {

  @throws[Throwable]("if things go wrong")
  def transform[T](i: I, to: Visitor[_, T]): T
  def validate[T](i: I, to: Visitor[_, T]): Try[T] = Try(transform(i, to))
}
