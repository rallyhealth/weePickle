package com.rallyhealth.weepickle.v1

import com.rallyhealth.weejson.v1.CanonicalizeNumsVisitor._
import com.rallyhealth.weejson.v1.jackson.{FromJson, ToJson, ToPrettyJson}
import com.rallyhealth.weejson.v1.{BufferedValue, GenBufferedValue}
import com.rallyhealth.weepickle.v1.core.FromInput
import org.scalactic.TypeCheckedTripleEquals
import org.scalatest.freespec.AnyFreeSpec
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

import java.io.{ByteArrayInputStream, StringReader}
import java.nio.file.Files
import scala.language.{existentials, implicitConversions}

abstract class ParserSpec(parse: Array[Byte] => FromInput, depthLimit: Int = 100)
  extends AnyFreeSpec
    with ScalaCheckPropertyChecks
    with GenBufferedValue
    with TypeCheckedTripleEquals {

  import com.rallyhealth.weejson.v1.BufferedValue._
  import com.rallyhealth.weejson.v1.BufferedValueOps._

  override implicit val generatorDrivenConfig: PropertyCheckConfiguration = PropertyCheckConfiguration(
    minSuccessful = 500
  )

  private implicit def toBytes(s: String): Array[Byte] = s.getBytes

  "roundtrip" in testJson()
  "deep arr" in testDepth(Arr(_))
  "deep obj" in testDepth(b => Obj("k" -> b))

  private def testJson(tweak: BufferedValue => BufferedValue = identity) = {
    forAll { (b: BufferedValue) =>
      val value = tweak(b)
      val expected = value.transform(ToJson.string)

      def testInput(s: Array[Byte]) = assert(parse(s).transform(ToJson.string) === expected)

      testInput(expected)
      testInput(s"\n $expected \n ")
      testInput(value.transform(ToPrettyJson.bytes))

      assert(parse(expected.getBytes()).transform(BufferedValue.Builder.canonicalize) === value)
    }
  }

  private def testDepth(f: BufferedValue => BufferedValue) = {
    testJson { in =>
      val depth = in.transform(new MaxDepthVisitor)
      val numPads = math.max(depthLimit - depth, 0)
      (0 to numPads).foldLeft(in)((p, _) => f(p))
    }
  }
}

class FromJsonBytesSpec extends ParserSpec(FromJson(_))

class FromJsonStringSpec extends ParserSpec(b => FromJson(new String(b)))

class FromJsonInputStreamSpec extends ParserSpec(b => FromJson(new ByteArrayInputStream(b)))

class FromJsonReaderSpec extends ParserSpec(b => FromJson(new StringReader(new String(b))))

class FromJsonPathSpec extends ParserSpec(
  b => FromJson(Files.write(Files.createTempFile("FromJsonPathSpec", ".json"), b))
)

class FromJsonFileSpec extends ParserSpec(
  b => FromJson(Files.write(Files.createTempFile("FromJsonFileSpec", ".json"), b).toFile)
)

