package com.rallyhealth.weepickle.v1.core

import org.scalactic.TypeCheckedTripleEquals
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks

import scala.util.Try

class UtilTests
  extends AnyFreeSpec
    with Matchers
    with ScalaCheckDrivenPropertyChecks
    with TypeCheckedTripleEquals {

  override implicit val generatorDrivenConfig: PropertyCheckConfiguration = PropertyCheckConfiguration(
    minSuccessful = 500
  )

  "parseLong" - {
    "valid" - {
      "trimmed" in {
        forAll { l: Long =>
          val s = l.toString
          Util.parseLong(s, 0, s.length) should ===(l)
        }
      }

      "padded" in {
        forAll { (head: String, l: Long, tail: String) =>
          val s = l.toString
          Util.parseLong(s"$head$s$tail", head.length, s.length) should ===(l)
        }
      }
    }
  }

  "failures" in {
    forAll { (s: String) =>
      Try(s.toLong).isSuccess shouldBe Try(Util.parseLong(s, 0, s.length)).isSuccess
    }
  }
}
