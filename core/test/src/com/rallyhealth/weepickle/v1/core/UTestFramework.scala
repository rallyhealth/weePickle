package com.rallyhealth.weepickle.v0.core

class UTestFramework extends utest.runner.Framework {
  override def exceptionStackFrameHighlighter(s: StackTraceElement) = {
    s.getClassName.startsWith("com.rallyhealth.weepickle.v0.") ||
    s.getClassName.startsWith("com.rallyhealth.weepack.v0.") ||
    s.getClassName.startsWith("com.rallyhealth.weejson.v0.")
  }
}
