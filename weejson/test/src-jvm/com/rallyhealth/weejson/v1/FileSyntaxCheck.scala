package com.rallyhealth.weejson.v0

import com.rallyhealth.weepickle.v0.core.NoOpVisitor

import scala.util.Try

class FileSyntaxCheck extends SyntaxCheck {
  override def isValidSyntax(s: String): Boolean = {
    TestUtil.withTemp(s) { t =>
      Try(Readable.fromFile(t).transform(NoOpVisitor)).isSuccess
    }
  }
}
