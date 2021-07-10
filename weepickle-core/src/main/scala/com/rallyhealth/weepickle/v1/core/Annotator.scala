package com.rallyhealth.weepickle.v1.core

import scala.reflect.ClassTag

// TBD if needed here - only used in Scala 3 macro implicits, include tagName from WeePickle opt
trait Annotator { this: Types =>
//  def annotate[V](rw: CaseR[V], n: String): TaggedTo[V]
//  def annotate[V](rw: CaseW[V], n: String)(implicit c: ClassTag[V]): TaggedFrom[V]

  def annotate[V](rw: CaseR[V], tagName: String, tag: String): TaggedTo[V]

  def annotate[V](rw: CaseW[V], tagName: String, tag: String)(implicit c: ClassTag[V]): TaggedFrom[V]

}
