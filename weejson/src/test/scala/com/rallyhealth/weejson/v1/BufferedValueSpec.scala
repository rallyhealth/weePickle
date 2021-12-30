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

  // covered by com.rallyhealth.weepickle.v1.ParserSpec, where jsonReversible = true
  //  property("roundtrip: String") {
  //    forAll { (value: BufferedValue) =>
  //      FromJson(value.transform(ToJson.string)).transform(BufferedValue.Builder) should ===(value)
  //    }
  //  }

  property("numeric equivalence: Long range/precision") {
    forAll { (value: Long) =>
      assert(equalsAndHashCode(
        () => BufferedValue.Num(value.toString, -1, -1),
        () => BufferedValue.NumDouble(value.toDouble),
        () => BufferedValue.NumLong(value)
      ))
    }
  }

  property("numeric equivalence: Double range/precision") {
    forAll { (value: Double) =>
      assert(equalsAndHashCode(
        () => BufferedValue.Num(value.toString, -1, -1),
        () => BufferedValue.NumDouble(value)
      ))
    }
  }

  property("numeric equivalence: BigDecimal range/precision") {
    forAll { (value: BigDecimal) =>
      assert(
        if (value.toDouble.isInfinite) equalsAndHashCode(
          () => BufferedValue.Num(value.toString, -1, -1)
        )
        else equalsAndHashCode(
          () => BufferedValue.Num(value.toString, -1, -1),
          () => BufferedValue.NumDouble(value.toDouble)
        )
      )
    }
  }

  private def equalsAndHashCode(v1: () => BufferedValue, v2: () => BufferedValue, v3: () => BufferedValue): Boolean =
    equalsAndHashCode(v1, v2) && equalsAndHashCode(v1, v3) && equalsAndHashCode(v2, v3)

  private def equalsAndHashCode(v1: () => BufferedValue, v2: () => BufferedValue): Boolean =
    equalsAndHashCode(v1) && equalsAndHashCode(v2) &&
    (v1() === v2()) && (v2() === v1()) && (v1().hashCode === v2().hashCode)

  private def equalsAndHashCode(v1: () => BufferedValue): Boolean =
    (v1() === v1()) && (v1().hashCode === v1().hashCode)

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
