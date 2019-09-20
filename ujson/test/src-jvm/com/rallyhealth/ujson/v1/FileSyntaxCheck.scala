package com.rallyhealth.ujson.v1

import com.rallyhealth.upickle.v1.core.NoOpVisitor

import scala.util.Try

class FileSyntaxCheck extends SyntaxCheck {
  override def isValidSyntax(s: String): Boolean = {
    TestUtil.withTemp(s) { t =>
      Try(Readable.fromFile(t).transform(NoOpVisitor)).isSuccess
    }
  }
}
