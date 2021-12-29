package com.rallyhealth.weejson.v1

import com.rallyhealth.weejson.v1.CanonicalizeNumsVisitor._
import org.scalacheck.{Arbitrary, Gen, Shrink}

import java.time.Instant
import scala.collection.mutable.ArrayBuffer

trait GenBufferedValue {

  import BufferedValueOps._
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
      Gen.const(Null),
      arbBinary.arbitrary,
      arbExt.arbitrary,
      arbTimestamp.arbitrary
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
    Gen.oneOf(generators(0), generators(1), generators.drop(2): _*)
      .map(b => b.transform(BufferedValue.Builder.canonicalize))
  }

  implicit val arbValue: Arbitrary[BufferedValue] = Arbitrary {
    Gen
      .chooseNum(0, 5)
      .flatMap(depth => genValue(depth))
  }

  implicit val arbNum: Arbitrary[AnyNum] = Arbitrary {
    Arbitrary.arbitrary[BigDecimal].map(AnyNum(_))
  }

  implicit val arbBinary: Arbitrary[Binary] = Arbitrary {
    Arbitrary.arbitrary[Array[Byte]].map(b => Binary(b))
  }

  implicit val arbExt: Arbitrary[Ext] = Arbitrary {
    Gen.choose[Byte](Byte.MinValue, Byte.MaxValue).flatMap { tag =>
      Arbitrary.arbitrary[Array[Byte]].map(b => Ext(tag, b))
    }
  }

  implicit val arbTimestamp: Arbitrary[Timestamp] = Arbitrary {
    Gen.choose[Instant](Instant.MIN, Instant.MAX).map(Timestamp(_))
  }

  implicit val shrinkValue: Shrink[BufferedValue] = Shrink[BufferedValue] { bv =>
    import BufferedValue._
    bv match {
      case obj: BufferedValue.Obj =>
        obj.value0.size match {
          case 1 =>
            val unwrappedValue = Stream(obj.value0.head._2)
            val shrunkContents = obj.value0.toStream.flatMap { case (k, v) => shrinkValue.shrink(v).map(v => BufferedValue.Obj(k -> v)) }
            unwrappedValue ++ shrunkContents
          case _ => Shrink.shrink(obj.value0).map(BufferedValue.Obj(_: _*)) // each element individually
        }
      case arr: BufferedValue.Arr =>
        arr.value.length match {
          case 1 =>
            val unwrappedValue = arr.value.head
            val shrunkContents = arr.value.toStream.flatMap(v => shrinkValue.shrink(v).map(BufferedValue.Arr(_)))
            unwrappedValue +: shrunkContents
          case _ =>
            Shrink.shrink(arr.value).map(BufferedValue.Arr(_)) // each element individually
        }
      case NumDouble(d) => NumLong(d.longValue) +: Shrink.shrink(d).map(NumDouble(_))
      case NumLong(long) => Null +: Shrink.shrink(long).map(NumLong(_))
      case Str(s) => Shrink.shrink(s).map(Str(_))
      case Timestamp(s) => Shrink.shrink(s).map(Timestamp(_))
      case Ext(tag, bytes) => Shrink.shrink(bytes).map(Ext(tag, _))
      case Binary(bytes) => Shrink.shrink(bytes).map(Binary(_))
      case _ => Stream.empty // Null, True, False, Num
    }
  }
}
