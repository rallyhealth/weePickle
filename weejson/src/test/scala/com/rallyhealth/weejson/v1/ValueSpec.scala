package com.rallyhealth.weejson.v1

import org.scalactic.TypeCheckedTripleEquals
import org.scalatest.matchers.should.Matchers
import org.scalatest.propspec.AnyPropSpec
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

class ValueSpec
    extends AnyPropSpec
    with Matchers
    with ScalaCheckPropertyChecks
    with GenValue
    with TypeCheckedTripleEquals {

  override implicit val generatorDrivenConfig: PropertyCheckConfiguration = PropertyCheckConfiguration(
    minSuccessful = 100
  )

  property("roundtrip: Visitor") {
    forAll { (value: Value) =>
      value.transform(Value) should ===(value)
    }
  }

  property("roundtrip: String") {
    forAll { (value: Value) =>
      WeeJson.read(WeeJson.write(value)) === (value)
    }
  }
}
