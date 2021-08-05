package com.rallyhealth.weepickle.v1.core

import scala.reflect.ClassTag

trait Annotator { this: Types =>

  def tagName: String // Default discriminator field name

  def annotate[V](rw: CaseR[V], tagName: String, tag: String): TaggedTo[V]

  def annotate[V](rw: CaseW[V], tagName: String, tag: String)(implicit c: ClassTag[V]): TaggedFrom[V]

}
