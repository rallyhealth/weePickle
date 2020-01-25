package com.rallyhealth.weepickle.v1

import com.rallyhealth._
import com.rallyhealth.weejson.v1.WeeJson
import com.rallyhealth.weepickle.v1.core.JsonPointerVisitor.JsonPointerException
import com.rallyhealth.weepickle.v1.core.{JsonPointerVisitor, NoOpVisitor}
import utest._

/**
  * @see [[JsonPointerVisitor]]
  */
object JsonPointerVisitorTests extends TestSuite {

  case class Foo(foo: List[String], s: String, i: Int, b: Boolean)

  implicit lazy val rw = weepickle.v1.WeePickle.macroFromTo[Foo]

  override def tests: Tests = Tests {
    test("failures") {
      def assertPathFailure(json: String, expectedPath: String) = {
        val cause = intercept[Exception](WeeJson.read(json).transform(JsonPointerVisitor(rw)))
        val failureAtPath = findException(cause)
        failureAtPath.get.jsonPointer ==> expectedPath
      }

      def findException(th: Throwable): Option[JsonPointerException] = {
        th.getSuppressed
          .collectFirst { case f: JsonPointerException => f }
          .orElse(Option(th.getCause).filter(_ != th).flatMap(findException))
      }

      test - assertPathFailure("""666""", "") // yes, empty string. https://tools.ietf.org/html/rfc6901#section-5
      test - assertPathFailure("""{"foo": -666, "s": "", "i": 5, "b": true}""", "/foo")
      test - assertPathFailure("""{"foo": [-666], "s": "", "i": 5, "b": true}""", "/foo/0")
      test - assertPathFailure("""{"foo": ["", -666], "s": "", "i": 5, "b": true}""", "/foo/1")
      test - assertPathFailure("""{"foo": ["", -666, ""], "s": "", "i": 5, "b": true}""", "/foo/1")
      test - assertPathFailure("""{"foo": [], "s": -666, "i": 5, "b": true}""", "/s")
      test - assertPathFailure("""{"foo": [], "s": "", "i": "-666", "b": true}""", "/i")
      test - assertPathFailure("""{"foo": [], "s": "", "i": 5, "b": -666}""", "/b")
      test - assertPathFailure("""{"foo": [], "s": "", "i": 5}""", "")
    }

    test("JSON Pointer rfc6901") {

      /**
        * {{{
        *    {
        *       "foo": ["bar", "baz"],
        *       "": 0,
        *       "a/b": 1,
        *       "c%d": 2,
        *       "e^f": 3,
        *       "g|h": 4,
        *       "i\\j": 5,
        *       "k\"l": 6,
        *       " ": 7,
        *       "m~n": 8
        *    }
        * }}}
        */
      test("compliance") - {
        val visitor = JsonPointerVisitor(NoOpVisitor)
        visitor.toString ==> ""

        val obj = visitor.visitObject(-1)
        obj.toString ==> ""

        obj.visitKey().visitString("foo")
        obj.toString ==> "/foo"
        obj.visitKeyValue(())

        val foo = obj.subVisitor
        foo.toString ==> "/foo"

        val arr = foo.visitArray(-1).narrow
        arr.toString ==> "/foo" // still describes the array, itself.

        val fooBar = arr.subVisitor
        fooBar.toString ==> "/foo/0"
        fooBar.visitString("bar")
        arr.visitValue(())
        fooBar.toString ==> "/foo/0"

        val fooBaz = arr.subVisitor
        fooBaz.toString ==> "/foo/1"
        fooBaz.visitString("baz")
        arr.visitValue(())
        fooBaz.toString ==> "/foo/1"

        arr.visitEnd()
        arr.toString ==> "/foo" // if visitEnd fails, pointing to the array seems more reasonable than /foo/1.
        obj.visitValue(())
        obj.toString ==> ""

        def appendKey(key: String, expectedPath: String) = {
          obj.visitKey().visitString(key)
          obj.toString ==> expectedPath
          obj.visitKeyValue(())
          val sub = obj.subVisitor
          sub.toString ==> expectedPath
          sub.visitInt32(42)
          obj.visitValue(())
        }

        appendKey("""""", """/""")
        appendKey("""a/b""", """/a~1b""")
        appendKey("""c%d""", """/c%d""")
        appendKey("""e^f""", """/e^f""")
        appendKey("""g|h""", """/g|h""")
        appendKey("""i\\j""", """/i\\j""")
        appendKey("""k\"l""", """/k\"l""")
        appendKey(""" """, """/ """)
        appendKey("""m~n""", """/m~0n""")

        obj.visitEnd()
        obj.toString ==> ""
        visitor.toString ==> ""
      }
    }
  }
}
