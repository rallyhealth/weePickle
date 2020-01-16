package com.rallyhealth.weejson.v0.jackson

import java.io.StringWriter

import com.rallyhealth.weejson.v0._
import com.rallyhealth.weejson.v0.jackson.DefaultJsonFactory.Instance
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
      WeeJackson.parseSingle(str, Value) should ===(value)
    }

    "multiple results" - {
      "newline delim" in parseValuesDelimitedBy("\n")
      "space delim" in parseValuesDelimitedBy(" ")
      "tab delim" in parseValuesDelimitedBy("\t")

      def parseValuesDelimitedBy(delim: String) = {
        val input = Seq("{}", "[]", "true", "5", "null").mkString(delim)
        val values = WeeJackson.parseMultiple(Instance.createParser(input), Value)
        values should ===(List(Obj(), Arr(), True, Num(5), Null))
      }
    }
  }

  "visitor" - {
    "like WeeJson.write" in {
      forAll { value: Value =>
        val writer = new StringWriter()
        value.transform(WeeJackson.toGenerator(Instance.createGenerator(writer))).close()
        writer.toString should ===(WeeJson.write(value))
      }
    }
  }
}
