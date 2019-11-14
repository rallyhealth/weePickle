package com.rallyhealth.weejson.v0

import org.scalatest._
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

class BoolSpec extends PropSpec with Matchers with ScalaCheckPropertyChecks with Inside {

  property("com.rallyhealth.weejson.v0.Bool apply") {
    com.rallyhealth.weejson.v0.Bool(true) shouldBe com.rallyhealth.weejson.v0.True
    com.rallyhealth.weejson.v0.Bool(false) shouldBe com.rallyhealth.weejson.v0.False
  }

  property("com.rallyhealth.weejson.v0.Bool.value") {
    forAll { bool: Boolean =>
      com.rallyhealth.weejson.v0.Bool(bool).value shouldBe bool
    }
  }

  property("com.rallyhealth.weejson.v0.Bool unapply") {
    forAll { bool: Boolean =>
      val jsb = com.rallyhealth.weejson.v0.Bool(bool)
      inside(jsb) {
        case com.rallyhealth.weejson.v0.Bool(value) =>
          value shouldBe bool
          jsb.value shouldBe value
      }
    }
  }
}
