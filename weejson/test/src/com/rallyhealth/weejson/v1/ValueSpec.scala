package com.rallyhealth.weejson.v1

import org.scalactic.TypeCheckedTripleEquals
import org.scalatest._
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

class ValueSpec
  extends PropSpec
    with Matchers
    with ScalaCheckPropertyChecks
    with GenValue
    with OptionValues
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

  val obj = Obj(
    "foo" -> Arr(
      0,
      Obj(
        "bar" -> true
      ),
      2
    )
  )

  property("dynamic lookup or throw") {
    obj.foo(1).bar.bool shouldBe true
    an[Exception] shouldBe thrownBy {
      obj.doesNotExist
    }
  }

  property("dynamic lookup opt") {
    obj.opt(_.foo(1).bar).value.bool should ===(true)
    obj.opt(_.doesNotExist) should ===(None)
    obj.opt(_.foo(4)) should ===(None)
    obj.opt(_.foo(1).doesNotExist) should ===(None)
  }
}
