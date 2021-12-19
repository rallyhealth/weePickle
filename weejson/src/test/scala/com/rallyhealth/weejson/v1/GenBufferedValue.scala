package com.rallyhealth.weejson.v1

import org.scalacheck.{Arbitrary, Gen, Shrink}

import scala.collection.mutable.ArrayBuffer

trait GenBufferedValue {
  import com.rallyhealth.weejson.v1.BufferedValue._

  def genArray(depth: Int): Gen[Arr] = {
    for {
      n <- Gen.chooseNum(0, 10)
      arr <- Gen.containerOfN[ArrayBuffer, BufferedValue](n, genValue(depth)).map(BufferedValue.fromElements)
    } yield arr
  }

  def genObject(depth: Int): Gen[Obj] =
    for {
      n <- Gen.chooseNum(0, 10)
      genKV = for {
        k <- Gen.alphaNumStr
        v <- genValue(depth)
      } yield k -> v
      obj <- Gen.buildableOfN[Map[String, BufferedValue], (String, BufferedValue)](n, genKV).map(BufferedValue.fromAttributes)
    } yield obj

  def genValue(depth: Int): Gen[BufferedValue] = {
    val nonRecursive: List[Gen[BufferedValue]] = List(
      Gen.alphaNumStr.map(Str.apply),
      arbNum.arbitrary,
      Arbitrary.arbitrary[Boolean].map(Bool(_)),
      Gen.const(Null)
    )

    val maybeRecursive: List[Gen[BufferedValue]] = depth match {
      case 0 => Nil
      case _ =>
        List(
          genArray(depth - 1),
          genObject(depth - 1)
        )
    }

    val generators: List[Gen[BufferedValue]] = nonRecursive ++ maybeRecursive
    Gen.oneOf(generators(0), generators(1), generators.drop(2): _*) // scalacheck API :\
  }

  implicit val arbValue: Arbitrary[BufferedValue] = Arbitrary {
    Gen
      .chooseNum(0, 5)
      .flatMap(depth => genValue(depth))
  }

  implicit val arbNum: Arbitrary[AnyNum] = Arbitrary {
    Arbitrary.arbitrary[Double].map(BigDecimal(_)).map(AnyNum(_)) // TODO open to all BigDecimals after #102
  }

  implicit val shrinkValue: Shrink[BufferedValue] = Shrink[BufferedValue] {
    case Obj(map) => Shrink.shrink(map).map(Obj(_))
    case Arr(buf) => Shrink.shrink(buf).map(Arr(_))
    case NumDouble(d) => NumLong(d.longValue) +: Shrink.shrink(d).map(NumDouble(_))
    case NumLong(long) => Shrink.shrink(long).map(NumLong(_))
    case Str(s) => Shrink.shrink(s).map(Str(_))
    case Timestamp(s) => Shrink.shrink(s).map(Timestamp(_))
    case Ext(tag, bytes) => Shrink.shrink(bytes).map(Ext(tag, _))
    case _ => Stream.empty
  }
}
