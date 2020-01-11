package com.rallyhealth.weejson.v1

import com.rallyhealth.weejson.v0.{Value, WeeJson}
import org.scalactic.TypeCheckedTripleEquals
import org.scalatest._
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

class ValueSpec
  extends PropSpec
    with Matchers
    with ScalaCheckPropertyChecks
    with GenValue
    with TypeCheckedTripleEquals {

  override implicit val generatorDrivenConfig: PropertyCheckConfiguration = PropertyCheckConfiguration(
    minSuccessful = 100
  )

  property("roundtrip: Visitor") {
    forAll { value: Value =>
      value.transform(Value) should ===(value)
    }
  }

  property("roundtrip: String") {
    forAll { value: Value =>
      WeeJson.read(WeeJson.write(value)) === (value)
    }
  }
}
