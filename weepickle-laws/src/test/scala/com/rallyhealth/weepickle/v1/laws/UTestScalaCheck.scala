package com.rallyhealth.weepickle.v1.laws

import org.scalacheck.Test.PropException
import org.scalacheck.util.Pretty
import org.scalacheck.{Prop, Test}

import scala.language.implicitConversions
import scala.util.control.NoStackTrace

/**
  * Adapted from https://github.com/lihaoyi/utest/issues/2#issuecomment-67300735
  */
trait UTestScalaCheck {

  protected[this] object UTestReporter extends Test.TestCallback {

    private val prettyParams = Pretty.defaultParams

    override def onTestResult(
      name: String,
      res: org.scalacheck.Test.Result
    ) = {
      if (!res.passed) {
        val msg =
          s"ScalaCheck property failed:\n${Pretty.pretty(res, prettyParams)}"
        val cause = Some(res.status).collect {
          case PropException(_, t, _) => t
        }.orNull
        throw new AssertionError(msg, cause) with NoStackTrace
      }
    }
  }

  /**
    * We're going to throw rather than returning a status.
    */
  protected implicit def propUnit(u: Unit): Prop = Prop(true)

  def check(
    prop: Prop
  ): Unit = prop.check(Test.Parameters.default.withTestCallback(UTestReporter))
}
