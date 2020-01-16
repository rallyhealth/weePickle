package com.rallyhealth.weejson.v1.jackson

import java.io.StringWriter
import java.nio.charset.StandardCharsets.UTF_8

import com.rallyhealth.weejson.v1.jackson.DefaultJsonFactory.Instance
import com.rallyhealth.weejson.v1.{Value, WeeJson}
import org.scalactic.TypeCheckedTripleEquals
import org.scalatest.{FreeSpec, LoneElement, Matchers}
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks

class JsonGeneratorOutputStreamSpec
  extends FreeSpec
    with Matchers
    with ScalaCheckDrivenPropertyChecks
    with GenValue
    with LoneElement
    with TypeCheckedTripleEquals {

  override implicit val generatorDrivenConfig: PropertyCheckConfiguration = PropertyCheckConfiguration(
    minSuccessful = 500
  )

  "like WeeJson" in {
    forAll { input: Value =>
      val result: Value = {
        var output = Seq.newBuilder[Value]
        val writer = new StringWriter
        val generator = Instance.createGenerator(writer)
        val out = new JsonGeneratorOutputStream(generator)

        val strInput = WeeJson.write(input)
        out.write(strInput.getBytes(UTF_8))
        out.close()

        WeeJson.read(writer.toString)
      }

      result shouldBe input
    }
  }
}
