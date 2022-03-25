package com.rallyhealth.weepickle.v1

import com.rallyhealth.weepickle.v1.NumberSoup.ValidJsonNum
import org.scalacheck.{Arbitrary, Gen}

/**
  * @param value some random concatenation of number-related characters that may or may not be a valid number.
  */
case class NumberSoup(value: String) {

  override def toString: String = s""""$value""""

  def isValidJson: Boolean = ValidJsonNum.pattern.matcher(value).matches()

  def isInvalidJson: Boolean = !isValidJson
}
object NumberSoup {
  val ValidJsonNum = """\s*-?(0|[1-9]\d*)(\.\d+)?([eE][-+]?\d+)?\s*""".r // based on https://datatracker.ietf.org/doc/html/rfc7159#page-6

  implicit val gen: Gen[NumberSoup] = {
    val maybeSpace = Gen.option(Gen.oneOf(" ", "  ", "\n"))
    val numberParts = Gen.frequency(
      1 -> Gen.numStr,
      1 -> ".",
      1 -> "-",
      1 -> "+",
      1 -> "E",
      1 -> "e",
      1 -> "0",
      1 -> " "
    )
    for {
      prefix <- maybeSpace
      parts <- Gen.listOf(numberParts).map(_.mkString)
      suffix <- maybeSpace
    } yield NumberSoup(s"$prefix$parts$suffix")
  }
  implicit val arb: Arbitrary[NumberSoup] = Arbitrary(gen)
}
