package com.rallyhealth.weejson.v1.jackson

import java.io.StringWriter

import com.rallyhealth.weejson.v1._
import com.rallyhealth.weejson.v1.jackson.DefaultJsonFactory.Instance
import org.scalactic.TypeCheckedTripleEquals
import org.scalatest.{FreeSpec, Matchers}
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks

class WeeJacksonSpec
    extends FreeSpec
    with Matchers
    with ScalaCheckDrivenPropertyChecks
    with GenValue
    with TypeCheckedTripleEquals {

  override implicit val generatorDrivenConfig: PropertyCheckConfiguration = PropertyCheckConfiguration(
    minSuccessful = 500
  )
  "parse" - {
    "like WeeJson" in forAll { value: Value =>
      val str = WeeJson.write(value)
      FromJson(str).transform(Value) should ===(value)
    }
  }

  "visitor" - {
    "like WeeJson.write" in {
      forAll { value: Value =>
        val writer = new StringWriter()
        value.transform(JsonRenderer(Instance.createGenerator(writer)))
        writer.toString should ===(value.transform(StringRenderer()).toString)
      }
    }
  }
}
