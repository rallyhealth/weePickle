package com.rallyhealth.weejson.v1.jackson

import com.rallyhealth.weepickle.v1.core.SimpleVisitor

object TrueVisitor extends SimpleVisitor[Boolean, Boolean] {

  override def expectedMsg: String = "expected true"

  override def visitTrue(): Boolean = true
}
