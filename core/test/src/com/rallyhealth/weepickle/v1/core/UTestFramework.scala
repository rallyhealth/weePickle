package com.rallyhealth.weepickle.v1.core

class UTestFramework extends utest.runner.Framework {
  override def exceptionStackFrameHighlighter(s: StackTraceElement) = {
    s.getClassName.startsWith("com.rallyhealth.weepickle.v1.") ||
    s.getClassName.startsWith("com.rallyhealth.upack.v1.") ||
    s.getClassName.startsWith("com.rallyhealth.ujson.v1.")
  }
}
