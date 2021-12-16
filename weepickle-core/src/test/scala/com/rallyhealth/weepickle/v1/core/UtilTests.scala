package com.rallyhealth.weepickle.v1.core

import org.scalactic.TypeCheckedTripleEquals
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks

import scala.util.Try

class UtilTests extends AnyFreeSpec with Matchers with ScalaCheckDrivenPropertyChecks with TypeCheckedTripleEquals {

  override implicit val generatorDrivenConfig: PropertyCheckConfiguration = PropertyCheckConfiguration(
    minSuccessful = 1000
  )

  "parseIntegralNum" - {
    def decIndex(s: String): Int = s.indexOf('.')

    def expIndex(s: String): Int = s.indexOf('E') match {
      case -1 => s.indexOf('e')
      case n  => n
    }

    "long" - {
      "exact" in {
        forAll { (l: Long) =>
          val sLong = l.toString
          Util.parseIntegralNum(sLong, decIndex(sLong), expIndex(sLong)) should ===(l)
          val sBd = BigDecimal(l).toString
          Util.parseIntegralNum(sBd, decIndex(sBd), expIndex(sBd)) should ===(l)
        }
      }
      "truncated" in {
        forAll { bd: BigDecimal =>
          // unfortunately there is still a gap where the parsing method doesn't support numbers between 1e+19 and Long.MaxValue,
          // e.g., 1.922438E+19 would fail with Abort("expected integer")
          // whenever(bd.abs < Double.MaxValue) {
          whenever(bd.abs < 1e+19) {
            val sBd = bd.toString
            Util.parseIntegralNum(sBd, decIndex(sBd), expIndex(sBd)) should ===(bd.toLong)
          }
        }
      }
    }

    "int" - {
      "exact" in {
        forAll { (i: Int) =>
          val sInt = i.toString
          Util.parseIntegralNum(sInt, decIndex(sInt), expIndex(sInt)).toInt should ===(i)
          val sBd = BigDecimal(i).toString
          Util.parseIntegralNum(sBd, decIndex(sBd), expIndex(sBd)).toInt should ===(i)
        }
      }
      "truncated" in {
        forAll { bd: BigDecimal =>
          whenever(bd.abs < Int.MaxValue) {
            val sBd = bd.toString
            Util.parseIntegralNum(sBd, decIndex(sBd), expIndex(sBd)).toInt should ===(bd.toInt)
          }
        }
      }
    }
  }

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
          Util.parseLong(s"$head$s$tail", head.length, head.length + s.length) should ===(l)
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

      def invalid(s: String) = {
        assert(Try(Util.parseLong(s, 0, s.length)).isFailure)
        assert(Try(Util.parseLong(" " + s, 1, 1 + s.length)).isFailure)
        assert(Try(Util.parseLong(s + " ", 0, s.length)).isFailure)
        assert(Try(Util.parseLong(" " + s + " ", 1, 1 + s.length)).isFailure)
      }
      "a" in invalid("a")
      "-" in invalid("-")
      "᥌" in invalid("᥌")
      "too long" in invalid(Long.MaxValue.toString + "1")

      "bounds" in {
        val s = "111"
        for {
          start <- -1 to s.length + 1
          end <- -1 to s.length + 1
          if end != start // NFE, not IndexOutOfBoundsException
        } {
          withClue(s"$s, $start, $end") {
            if (Try(s.substring(start, end)).isSuccess) {
              Util.parseLong(s, start, end)
            } else {
              intercept[IndexOutOfBoundsException](Util.parseLong(s, start, end))
            }
          }
        }

      }
    }
  }
}
