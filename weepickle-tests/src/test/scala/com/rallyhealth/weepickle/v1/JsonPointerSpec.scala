package com.rallyhealth.weepickle.v1

import com.rallyhealth.weejson.v1.jackson.FromJson
import com.rallyhealth.weepickle.v1.core.{FromInput, NoOpVisitor, TransformException}
import org.scalactic.TypeCheckedTripleEquals
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

import java.io.{ByteArrayInputStream, StringReader}
import java.nio.file.Files
import scala.language.{existentials, implicitConversions}

abstract class JsonPointerSpec(parse: Array[Byte] => FromInput, depthLimit: Int = 100)
  extends AnyFreeSpec
    with Matchers
    with TypeCheckedTripleEquals {

  private implicit def toBytes(s: String): Array[Byte] = s.getBytes

  def assertPath(json: String, expectedPointer: String) = {
    val t = intercept[TransformException](parse(json).transform(NoOpVisitor))
    t.jsonPointer shouldBe expectedPointer
  }

  val scenarios = Seq(
    ("", ""),
    ("unquoted", ""),
    (""" {"a": """, "/a"),
    (""" {"a": 1, "b"  """, "/b"),
    (""" {"foo/bar": """, "/foo~1bar"),
    (""" {"foo~bar": """, "/foo~0bar"),
    (""" [0,1,2 """, "/2"),
    (""" [[[ """, "/0/0"),
    (""" [[0,[0,1,[ """, "/0/1/2")
  )
  for ((json, expected) <- scenarios) {
    json in assertPath(json, expected)
  }
}

class FromJsonBytesJsonPointerSpec extends JsonPointerSpec(FromJson(_))

class FromJsonStringJsonPointerSpec extends JsonPointerSpec(b => FromJson(new String(b)))

class FromJsonInputStreamJsonPointerSpec extends JsonPointerSpec(b => FromJson(new ByteArrayInputStream(b)))

class FromJsonReaderJsonPointerSpec extends JsonPointerSpec(b => FromJson(new StringReader(new String(b))))

class FromJsonPathJsonPointerSpec extends JsonPointerSpec(
  b => FromJson(Files.write(Files.createTempFile("JsonPointerSpec", ".json"), b))
)

class FromJsonFileJsonPointerSpec extends JsonPointerSpec(
  b => FromJson(Files.write(Files.createTempFile("JsonPointerSpec", ".json"), b).toFile)
)

