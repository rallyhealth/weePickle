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
    minSuccessful = 1000
  )

  "parseLong" - {
    "valid" - {
      "trimmed" in {
        forAll { (l: Long) =>
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

  "failures" - {
    "strings" in {
      forAll { (s: String) =>
        whenever(Try(s.toLong).isFailure) {
          assert(Try(Util.parseLong(s, 0, s.length)).isFailure)
        }
      }
    }

    "a" in (assert(Try(Util.parseLong("a", 0, 1)).isFailure))
    "-" in (assert(Try(Util.parseLong("-", 0, 1)).isFailure))
    "᥌" in (assert(Try(Util.parseLong("᥌", 0, 1)).isFailure)) // invalid per https://datatracker.ietf.org/doc/html/rfc7159#section-6
  }
}
