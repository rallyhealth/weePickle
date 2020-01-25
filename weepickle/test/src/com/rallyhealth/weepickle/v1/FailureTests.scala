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
    // format: off
    test("tooManyFields"){
      val b63 = Big63(
        0, 1, 2, 3, 4, 5, 6, 7,
        8, 9, 10, 11, 12, 13, 14,
        15, 16, 17, 18, 19, 20, 21,
        22, 23, 24, 25, 26, 27, 28,
        29, 30, 31, 32, 33, 34, 35,
        36, 37, 38, 39, 40, 41, 42,
        43, 44, 45, 46, 47, 48, 49,
        50, 51, 52, 53, 54, 55, 56,
        57, 58, 59, 60, 61, 62
      )
      val b64 = Big64(
        0, 1, 2, 3, 4, 5, 6, 7,
        8, 9, 10, 11, 12, 13, 14,
        15, 16, 17, 18, 19, 20, 21,
        22, 23, 24, 25, 26, 27, 28,
        29, 30, 31, 32, 33, 34, 35,
        36, 37, 38, 39, 40, 41, 42,
        43, 44, 45, 46, 47, 48, 49,
        50, 51, 52, 53, 54, 55, 56,
        57, 58, 59, 60, 61, 62, 63
      )
      val b65 = Big65(
        0, 1, 2, 3, 4, 5, 6, 7,
        8, 9, 10, 11, 12, 13, 14,
        15, 16, 17, 18, 19, 20, 21,
        22, 23, 24, 25, 26, 27, 28,
        29, 30, 31, 32, 33, 34, 35,
        36, 37, 38, 39, 40, 41, 42,
        43, 44, 45, 46, 47, 48, 49,
        50, 51, 52, 53, 54, 55, 56,
        57, 58, 59, 60, 61, 62, 63,
        64
      )
      // format: on
      implicit val b63rw: com.rallyhealth.weepickle.v1.WeePickle.FromTo[Big63] =
        com.rallyhealth.weepickle.v1.WeePickle.macroFromTo
      implicit val b64rw: com.rallyhealth.weepickle.v1.WeePickle.FromTo[Big64] =
        com.rallyhealth.weepickle.v1.WeePickle.macroFromTo
      val written63 = FromScala(b63).transform(ToJson.string)
      assert(FromJson(written63).transform(ToScala[Big63]) == b63)
      val written64 = FromScala(b64).transform(ToJson.string)
      assert(FromJson(written64).transform(ToScala[Big64]) == b64)
      val err = compileError(
        "{implicit val b64rw: com.rallyhealth.weepickle.v1.WeePickle.FromTo[Big65] = com.rallyhealth.weepickle.v1.WeePickle.macroFromTo}"
      )
      assert(err.msg.contains("weepickle does not support serializing case classes with >64 fields"))
    }
  }
}

// format: off
case class Big63(_0: Byte, _1: Byte, _2: Byte, _3: Byte, _4: Byte, _5: Byte, _6: Byte, _7: Byte,
  _8: Byte, _9: Byte, _10: Byte, _11: Byte, _12: Byte, _13: Byte, _14: Byte,
  _15: Byte, _16: Byte, _17: Byte, _18: Byte, _19: Byte, _20: Byte, _21: Byte,
  _22: Byte, _23: Byte, _24: Byte, _25: Byte, _26: Byte, _27: Byte, _28: Byte,
  _29: Byte, _30: Byte, _31: Byte, _32: Byte, _33: Byte, _34: Byte, _35: Byte,
  _36: Byte, _37: Byte, _38: Byte, _39: Byte, _40: Byte, _41: Byte, _42: Byte,
  _43: Byte, _44: Byte, _45: Byte, _46: Byte, _47: Byte, _48: Byte, _49: Byte,
  _50: Byte, _51: Byte, _52: Byte, _53: Byte, _54: Byte, _55: Byte, _56: Byte,
  _57: Byte, _58: Byte, _59: Byte, _60: Byte, _61: Byte, _62: Byte)
case class Big64(_0: Byte, _1: Byte, _2: Byte, _3: Byte, _4: Byte, _5: Byte, _6: Byte, _7: Byte,
  _8: Byte, _9: Byte, _10: Byte, _11: Byte, _12: Byte, _13: Byte, _14: Byte,
  _15: Byte, _16: Byte, _17: Byte, _18: Byte, _19: Byte, _20: Byte, _21: Byte,
  _22: Byte, _23: Byte, _24: Byte, _25: Byte, _26: Byte, _27: Byte, _28: Byte,
  _29: Byte, _30: Byte, _31: Byte, _32: Byte, _33: Byte, _34: Byte, _35: Byte,
  _36: Byte, _37: Byte, _38: Byte, _39: Byte, _40: Byte, _41: Byte, _42: Byte,
  _43: Byte, _44: Byte, _45: Byte, _46: Byte, _47: Byte, _48: Byte, _49: Byte,
  _50: Byte, _51: Byte, _52: Byte, _53: Byte, _54: Byte, _55: Byte, _56: Byte,
  _57: Byte, _58: Byte, _59: Byte, _60: Byte, _61: Byte, _62: Byte, _63: Byte)
case class Big65(_0: Byte, _1: Byte, _2: Byte, _3: Byte, _4: Byte, _5: Byte, _6: Byte, _7: Byte,
  _8: Byte, _9: Byte, _10: Byte, _11: Byte, _12: Byte, _13: Byte, _14: Byte,
  _15: Byte, _16: Byte, _17: Byte, _18: Byte, _19: Byte, _20: Byte, _21: Byte,
  _22: Byte, _23: Byte, _24: Byte, _25: Byte, _26: Byte, _27: Byte, _28: Byte,
  _29: Byte, _30: Byte, _31: Byte, _32: Byte, _33: Byte, _34: Byte, _35: Byte,
  _36: Byte, _37: Byte, _38: Byte, _39: Byte, _40: Byte, _41: Byte, _42: Byte,
  _43: Byte, _44: Byte, _45: Byte, _46: Byte, _47: Byte, _48: Byte, _49: Byte,
  _50: Byte, _51: Byte, _52: Byte, _53: Byte, _54: Byte, _55: Byte, _56: Byte,
  _57: Byte, _58: Byte, _59: Byte, _60: Byte, _61: Byte, _62: Byte, _63: Byte,
  _64: Byte)
// format: on
