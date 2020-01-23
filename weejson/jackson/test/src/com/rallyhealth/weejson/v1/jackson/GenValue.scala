package com.rallyhealth.weejson.v1.jackson

import com.rallyhealth.weejson.v1._
import org.scalacheck.{Arbitrary, Gen}

import scala.collection.mutable.ArrayBuffer

trait GenValue {

  def genArray(depth: Int): Gen[Arr] = {
    for {
      n <- Gen.chooseNum(0, 10)
      arr <- Gen.containerOfN[ArrayBuffer, Value](n, genValue(depth)).map(Arr(_))
    } yield arr
  }

  def genObject(depth: Int): Gen[Obj] =
    for {
      n <- Gen.chooseNum(0, 10)
      genKV = for {
        k <- Gen.alphaNumStr
        v <- genValue(depth)
      } yield k -> v
      obj <- Gen.buildableOfN[Map[String, Value], (String, Value)](n, genKV).map(Obj.from)
    } yield obj

  def genValue(depth: Int): Gen[Value] = {
    val nonRecursive = List(
      Gen.alphaNumStr.map(Str),
      Arbitrary.arbitrary[Double].map(Num(_)),
      Arbitrary.arbitrary[Boolean].map(Bool(_)),
      Gen.const(Null)
    )

    val maybeRecursive: List[Gen[Value]] = depth match {
      case 0 => Nil
      case _ =>
        List(
          genArray(depth - 1),
          genObject(depth - 1)
        )
    }

    val generators: List[Gen[Value]] = nonRecursive ++ maybeRecursive
    Gen.oneOf(generators(0), generators(1), generators.drop(2): _*) // scalacheck API :\
  }

  implicit val arbJsValue: Arbitrary[Value] = Arbitrary {
    Gen
      .chooseNum(0, 5)
      .flatMap(depth => genValue(depth))
  }
}
