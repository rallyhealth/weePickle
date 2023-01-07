package com.rallyhealth.weejson.v1

import com.rallyhealth.weejson.v1.jackson.{FromJson, ToJson}
import org.scalactic.TypeCheckedTripleEquals
import org.scalatest.matchers.should.Matchers
import org.scalatest.propspec.AnyPropSpec
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

class BufferedValueSpec
    extends AnyPropSpec
    with Matchers
    with ScalaCheckPropertyChecks
    with GenBufferedValue
    with TypeCheckedTripleEquals {
  import BufferedValueOps._

  override implicit val generatorDrivenConfig: PropertyCheckConfiguration = PropertyCheckConfiguration(
    minSuccessful = 100
  )

  property("roundtrip: Visitor") {
    forAll { (value: BufferedValue) =>
      value.transform(BufferedValue.Builder) should ===(value)
    }
  }

  property("roundtrip: String") {
    forAll { (value: BufferedValue) =>
      FromJson(value.transform(ToJson.string)).transform(BufferedValue.Builder) === (value)
    }
  }
}
