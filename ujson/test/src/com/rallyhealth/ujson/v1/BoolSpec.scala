package com.rallyhealth.ujson.v1

import org.scalatest._
import org.scalatest.prop._

class BoolSpec extends PropSpec with Matchers with PropertyChecks with Inside {

  property("com.rallyhealth.ujson.v1.Bool apply") {
    com.rallyhealth.ujson.v1.Bool(true) shouldBe com.rallyhealth.ujson.v1.True
    com.rallyhealth.ujson.v1.Bool(false) shouldBe com.rallyhealth.ujson.v1.False
  }

  property("com.rallyhealth.ujson.v1.Bool.value") {
    forAll { bool: Boolean =>
      com.rallyhealth.ujson.v1.Bool(bool).value shouldBe bool
    }
  }

  property("com.rallyhealth.ujson.v1.Bool unapply") {
    forAll { bool: Boolean =>
      val jsb = com.rallyhealth.ujson.v1.Bool(bool)
      inside(jsb) {
        case com.rallyhealth.ujson.v1.Bool(value) =>
          value shouldBe bool
          jsb.value shouldBe value
      }
    }
  }
}
