package com.rallyhealth.weepack.v1

import com.rallyhealth.weepickle.v1.core.Visitor

/**
  * A Visitor specialized to work with msgpack types. Forwards the
  * not-msgpack-related methods to their msgpack equivalents.
  */
trait MsgVisitor[-T, +J] extends Visitor[T, J] {

  def visitFloat64String(s: String): J = this.visitFloat64(s.toDouble)

  def visitFloat64StringParts(cs: CharSequence, decIndex: Int, expIndex: Int): J = {
    this.visitFloat64(cs.toString.toDouble)
  }
}
