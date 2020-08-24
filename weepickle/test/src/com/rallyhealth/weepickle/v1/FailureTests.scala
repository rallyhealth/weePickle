package com.rallyhealth.weepickle.v1

import com.rallyhealth.weejson.v1.jackson.{FromJson, ToJson}
import com.rallyhealth.weepickle.v1.WeePickle.{FromScala, ToScala}
import com.rallyhealth.weepickle.v1.core.TransformException
import utest._
case class Fee(i: Int, s: String)
sealed trait Fi
object Fi {
  implicit def rw2: com.rallyhealth.weepickle.v1.WeePickle.FromTo[Fi] =
    com.rallyhealth.weepickle.v1.WeePickle.FromTo.merge(Fo.rw2, Fum.rw2)
  case class Fo(i: Int) extends Fi
  object Fo {
    implicit def rw2: com.rallyhealth.weepickle.v1.WeePickle.FromTo[Fo] =
      com.rallyhealth.weepickle.v1.WeePickle.macroFromTo
  }
  case class Fum(s: String) extends Fi
  object Fum {
    implicit def rw2: com.rallyhealth.weepickle.v1.WeePickle.FromTo[Fum] =
      com.rallyhealth.weepickle.v1.WeePickle.macroFromTo
  }
}

/**
  * Generally, every failure should be a Invalid.Json or a
  * InvalidData. If any assertion errors, match errors, number
  * format errors or similar leak through, we've failed
  */
object FailureTests extends TestSuite {

  def tests = Tests {
//    test("test"){
//      read[com.rallyhealth.weejson.v1.Value](""" {unquoted_key: "keys must be quoted"} """)
//    }

    test("jsonFailures") {
      // Run through the test cases from the json.org validation suite,
      // skipping the ones which we don't support yet (e.g. leading zeroes,
      // extra commas) or will never support (e.g. too deep)

      val failureCases = Seq(
//        """ "A JSON payload should be an object or array, not a string." """,
        """ {"Extra value after close": true} "misplaced quoted value" """,
        """ {"Illegal expression": 1 + 2} """,
        """ {"Illegal invocation": alert()} """,
        """ {"Numbers cannot have leading zeroes": 013} """,
        """ {"Numbers cannot be hex": 0x14} """,
        """ ["Illegal backslash escape: \x15"] """,
        """ [\naked] """,
        """ ["Illegal backslash escape: \017"] """,
//        """ [[[[[[[[[[[[[[[[[[[["Too deep"]]]]]]]]]]]]]]]]]]]] """,
        """ {"Missing colon" null} """,
        """ {"Double colon":: null} """,
        """ {"Comma instead of colon", null} """,
        """ ["Colon instead of comma": false] """,
        """ ["Bad value", truth] """,
        """ ['single quote'] """,
        """ ["	tab	character	in	string	"] """,
        """ ["tab\   character\   in\  string\  "] """,
        """ ["line
          break"] """,
        """ ["line\
          break"] """,
        """ [0e] """,
        """ {unquoted_key: "keys must be quoted"} """,
        """ [0e+-1] """,
        """ ["mismatch"} """,
        """ ["extra comma",] """,
        """ ["double extra comma",,] """,
        """ [   , "<-- missing value"] """,
        """ ["Comma after the close"], """,
        """ ["Extra close"]] """,
        """ {"Extra comma": true,} """
      ).map(_.trim())
      val res =
        for (failureCase <- failureCases)
          yield try {
            intercept[Exception] { FromJson(failureCase).transform(ToScala[com.rallyhealth.weejson.v1.Value]) }
            None
          } catch {
            case _: Throwable =>
              Some(failureCase)
          }

      val nonFailures = res.flatten
      assert(nonFailures.isEmpty)
      intercept[Exception] {
        FromJson(""" {"Comma instead if closing brace": true, """).transform(ToScala[com.rallyhealth.weejson.v1.Value])
      }
      intercept[Exception] { FromJson(""" ["Unclosed array" """).transform(ToScala[com.rallyhealth.weejson.v1.Value]) }
    }

    test("facadeFailures") {
      def assertErrorMsgDefault[T: com.rallyhealth.weepickle.v1.WeePickle.To](s: String, msgs: String*) = {
        val err = intercept[TransformException] { FromJson(s).transform(ToScala[T]) }
        val errMsgs = Seq(err.getMessage, err.getCause.getMessage)
        for (msg <- msgs) assert(errMsgs.exists(_.contains(msg)))
        err
      }
      test("caseClass") {
        // Separate this guy out because the read macro and
        // the intercept macro play badly with each other

        test("invalidTag") {
          test - assertErrorMsgDefault[Fi.Fo](
            """{"$type": "omg"}]""",
            "invalid tag for tagged object: omg",
            "jsonPointer=/$type index=11 line=1 col=12 token=VALUE_STRING"
          )
          test - assertErrorMsgDefault[Fi](
            """{"$type": "omg"}]""",
            "invalid tag for tagged object: omg",
            "jsonPointer=/$type index=11 line=1 col=12 token=VALUE_STRING"
          )
        }

        test("taggedInvalidBody") {
          test - assertErrorMsgDefault[Fi.Fo](
            """{"$type": "com.rallyhealth.weepickle.v1.Fi.Fo", "i": true, "z": null}""",
            "expected number got boolean",
            "jsonPointer=/i index=54 line=1 col=55 token=VALUE_TRUE"
          )
          test - assertErrorMsgDefault[Fi](
            """{"$type": "com.rallyhealth.weepickle.v1.Fi.Fo", "i": true, "z": null}""",
            "expected number got boolean",
            "jsonPointer=/i index=54 line=1 col=55 token=VALUE_TRUE"
          )
        }
      }
    }
    test("compileErrors") {
      compileError("write(new Object)")
      compileError("""read[Object]("")""")
//      compileError("""read[Array[Object]]("")""").msg
      // Make sure this doesn't hang the compiler =/
      compileError("implicitly[com.rallyhealth.weepickle.v1.WeePickle.To[Nothing]]")
    }
    test("expWholeNumbers") {
      FromJson("0e0").transform(ToScala[Byte]) ==> 0.toByte
      FromJson("0e0").transform(ToScala[Short]) ==> 0
      FromJson("0e0").transform(ToScala[Char]) ==> 0.toChar
      FromJson("0e0").transform(ToScala[Int]) ==> 0
      FromJson("0e0").transform(ToScala[Long]) ==> 0

      FromJson("10e1").transform(ToScala[Byte]) ==> 100
      FromJson("10e1").transform(ToScala[Short]) ==> 100
      FromJson("10e1").transform(ToScala[Char]) ==> 100
      FromJson("10e1").transform(ToScala[Int]) ==> 100
      FromJson("10e1").transform(ToScala[Long]) ==> 100

      FromJson("10.1e1").transform(ToScala[Byte]) ==> 101
      FromJson("10.1e1").transform(ToScala[Short]) ==> 101
      FromJson("10.1e1").transform(ToScala[Char]) ==> 101
      FromJson("10.1e1").transform(ToScala[Int]) ==> 101
      FromJson("10.1e1").transform(ToScala[Long]) ==> 101

      // Not supporting these for now, since AFAIK none of the
      // JSON serializers I know generate numbers of this form
      //      FromJson("10e-1").transform(ToScala[Byte]) ==> 1
      //      FromJson("10e-1").transform(ToScala[Short]) ==> 1
      //      FromJson("10e-1").transform(ToScala[Char]) ==> 1
      //      FromJson("10e-1").transform(ToScala[Int]) ==> 1
      //      FromJson("10e-1").transform(ToScala[Long]) ==> 1
    }
  }
}
