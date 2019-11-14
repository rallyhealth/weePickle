package com.rallyhealth.weejson.v0

import org.scalatest.{Matchers, PropSpec}
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

class FileJNumIndexCheck extends PropSpec with Matchers with ScalaCheckPropertyChecks {

  property("visitFloat64StringParts provides the correct indices with parseFromFile") {
    forAll { (value: BigDecimal) =>
      val json = s"""{ "num": ${value.toString} }"""
      TestUtil.withTemp(json) { t =>
        Readable.fromFile(t).transform(JNumIndexCheckFacade) shouldBe true
      }
    }
  }

  property("visitFloat64StringParts provides the correct indices at the top level with parseFromFile") {
    forAll { (value: BigDecimal) =>
      TestUtil.withTemp(value.toString) { t =>
        Readable.fromFile(t).transform(JNumIndexCheckFacade) shouldBe true
      }
    }
  }
}
