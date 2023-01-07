package com.rallyhealth.weejson.v1.jackson

import com.rallyhealth.weepickle.v1.core.FromInput
import utest._

import java.io.{ByteArrayInputStream, StringReader}
import java.nio.file.{Files, Path}

object FromJsonTests extends TestSuite {
  override val tests = Tests {
    test("replay") {
      test("FromJson(String)")(replay(FromJson("true")))
      test("FromJson(Array[Byte])")(replay(FromJson("true".getBytes)))
      test("FromJson(Path)")(replay(FromJson(path)))
      test("FromJson(File)")(replay(FromJson(path.toFile)))

      def replay(fromInput: FromInput) = {
        fromInput.transform(TrueVisitor) ==> true
        fromInput.transform(TrueVisitor) ==> true
        fromInput.transform(TrueVisitor) ==> true
      }
      def path: Path = Files.write(Files.createTempFile("", ".json"), "true".getBytes)
    }

    test("no replay") {
      test("FromJson(InputStream)")(noReplay(FromJson(new ByteArrayInputStream("true".getBytes))))
      test("FromJson(Reader)")(noReplay(FromJson(new StringReader("true"))))

      def noReplay(fromInput: FromInput) = {
        fromInput.transform(TrueVisitor) ==> true
        intercept[Exception](fromInput.transform(TrueVisitor))
      }
    }
  }
}
