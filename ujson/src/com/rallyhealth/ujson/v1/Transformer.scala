package com.rallyhealth.ujson.v1
import com.rallyhealth.upickle.v1.core.Visitor

trait Transformer[I] {
  def transform[T](j: I, f: Visitor[_, T]): T
  def transformable[T](j: I) = Readable.fromTransformer(j, this)
}
