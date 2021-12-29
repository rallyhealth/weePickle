package com.rallyhealth.weejson.v1

import com.rallyhealth.weejson.v1.jackson.{FromJson, ToJson}
import org.scalactic.TypeCheckedTripleEquals
import org.scalatest.matchers.should.Matchers
import org.scalatest.propspec.AnyPropSpecLike
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

import scala.util.Random

class BufferedValueSpec
    extends GenBufferedValue(jsonReversible = false)
    with AnyPropSpecLike
    with Matchers
    with ScalaCheckPropertyChecks
    with TypeCheckedTripleEquals {
  import BufferedValueOps._

  override implicit val generatorDrivenConfig: PropertyCheckConfiguration = PropertyCheckConfiguration(
    minSuccessful = 500
  )

  property("roundtrip: Visitor") {
    forAll { (value: BufferedValue) =>
      value.transform(BufferedValue.Builder) should ===(value)
    }
  }

  property("roundtrip: Visitor with object shuffling") {
    forAll { (value: BufferedValue) =>
      shuffleObjs(value.transform(BufferedValue.Builder)) should ===(value)
    }
  }

  property("roundtrip: String") {
    forAll { (value: BufferedValue) =>
      FromJson(value.transform(ToJson.string)).transform(BufferedValue.Builder) === (value)
    }
  }

  private def shuffleObjs(v: BufferedValue): BufferedValue = v match {
    case BufferedValue.Obj(attributes @ _*) =>
      BufferedValue.fromAttributes(Random.shuffle(attributes).map {
        case (key, value) =>
          key -> shuffleObjs(value)
      })

    case BufferedValue.Arr(elements @ _*) =>
      BufferedValue.fromElements(elements.map(shuffleObjs))

    case other => other
  }
}
