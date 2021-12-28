package com.rallyhealth.weepickle.v1

import com.rallyhealth.weejson.v1.CanonicalizeNumsVisitor._
import com.rallyhealth.weejson.v1.jackson.{FromJson, ToJson, ToPrettyJson}
import com.rallyhealth.weejson.v1.wee_jsoniter_scala.FromJsoniterScala
import com.rallyhealth.weejson.v1.{BufferedValue, GenBufferedValue, Value}
import com.rallyhealth.weepickle.v1.NumberSoup.ValidJsonNum
import com.rallyhealth.weepickle.v1.core.{FromInput, NoOpVisitor, Visitor}
import org.scalactic.TypeCheckedTripleEquals
import org.scalatest.freespec.AnyFreeSpec
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

import java.io.{ByteArrayInputStream, File, StringReader}
import java.nio.ByteBuffer
import java.nio.file.Files
import scala.concurrent.duration._
import scala.language.{existentials, implicitConversions}
import scala.util.Try

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
  "number soup" in forAll { soup: NumberSoup =>
    val isValid = ValidJsonNum.matches(soup.value)
    val parseSuccess = Try(parse(soup.value.getBytes()).transform(NoOpVisitor)).isSuccess
    assert(parseSuccess === isValid)
  }
  "net/JSONTestSuite" - {
    for {
      file <- new File("weepickle-tests/src/test/test_parsing").listFiles()
      name = file.getName
      if name.endsWith(".json")
    } {
      def parse[J](v: Visitor[_, J]) = FromJson(file).transform(v)

      name in {
        val start = System.currentTimeMillis()

        def duration = (System.currentTimeMillis() - start).nanos

        name.head match {
          case 'i' =>
            // check for fatal exceptions
            Try(parse(NoOpVisitor))
            Try(parse(Value))
          case 'y' =>
            // parser allows values AND `Value` understands them
            parse(Value)
          case 'n' =>
            intercept[Exception](parse(NoOpVisitor)) // parser should reject
        }
        assert(duration < 5.seconds, s"parsing $name exceeded than the 5s time limit")
      }
    }
  }

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

class FromJsonPathSpec
    extends ParserSpec(
      b => FromJson(Files.write(Files.createTempFile("FromJsonPathSpec", ".json"), b))
    )

class FromJsonFileSpec
    extends ParserSpec(
      b => FromJson(Files.write(Files.createTempFile("FromJsonFileSpec", ".json"), b).toFile)
    )

class FromJsoniterScalaBytesSpec extends ParserSpec(FromJsoniterScala(_), 62)

class FromJsoniterScalaInputStreamSpec extends ParserSpec(b => FromJsoniterScala(new ByteArrayInputStream(b)), 62)

class FromJsoniterScalaByteBufferSpec extends ParserSpec(b => FromJsoniterScala(ByteBuffer.wrap(b)), 62)
