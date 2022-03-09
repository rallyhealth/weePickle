package com.rallyhealth.weepickle.v1

import com.rallyhealth.weepickle.v1.NumberSoup.ValidJsonNum
import org.scalacheck.{Arbitrary, Gen}

case class NumberSoup(value: String) {

  override def toString: String = value

  def isValid: Boolean = ValidJsonNum.pattern.matcher(value).matches()

  def isInvalid: Boolean = !isValid
}
object NumberSoup {
  val ValidJsonNum = """-?(0|[1-9]\d*)(\.\d+)?([eE][-+]?\d+)?""".r // based on https://datatracker.ietf.org/doc/html/rfc7159#page-6

  implicit val gen: Gen[NumberSoup] = {
    val numberParts = Gen.frequency(
      1 -> Gen.numStr,
      1 -> ".",
      1 -> "-",
      1 -> "+",
      1 -> "E",
      1 -> "e"
    )
    Gen.listOf(numberParts).map(_.mkString).map(NumberSoup(_))
  }
  implicit val arb = Arbitrary(gen)
}
