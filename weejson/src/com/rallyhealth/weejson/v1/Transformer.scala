package com.rallyhealth.weejson.v0
import com.rallyhealth.weepickle.v0.core.Visitor

trait Transformer[I] {
  def transform[T](j: I, f: Visitor[_, T]): T
  def transformable[T](j: I) = Readable.fromTransformer(j, this)
}
