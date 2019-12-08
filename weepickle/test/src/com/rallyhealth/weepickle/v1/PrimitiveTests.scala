package com.rallyhealth.weepickle.v0

import java.math.MathContext
import java.net.URI

import com.rallyhealth.weepickle.v0.TestUtil._
import utest._

object PrimitiveTests extends TestSuite {

  def tests = Tests {
    test("Unit"){
      rw((), "{}")
    }
    test("Boolean"){
      test("true") - rw(true, "true")
      test("false") - rw(false, "false")
    }
    test("String"){
      test("plain") - rw("i am a cow", """ "i am a cow" """)
      test("quotes") - rw("i am a \"cow\"", """ "i am a \"cow\"" """)
      test("unicode"){
        rw("叉烧包")
        com.rallyhealth.weepickle.v0.WeePickle.write("叉烧包") ==> "\"叉烧包\""
        com.rallyhealth.weepickle.v0.WeePickle.write("叉烧包", escapeUnicode = true) ==> "\"\\u53c9\\u70e7\\u5305\""
        com.rallyhealth.weepickle.v0.WeePickle.read[String]("\"\\u53c9\\u70e7\\u5305\"") ==> "叉烧包"
        com.rallyhealth.weepickle.v0.WeePickle.read[String]("\"叉烧包\"") ==> "叉烧包"
      }
      test("null") - rw(null: String, "null")
      test("chars"){
        for(i <- Char.MinValue until 55296/*Char.MaxValue*/) {
          rw(i.toString)
        }
      }
    }
    test("Symbol"){
      test("plain") - rw('i_am_a_cow, """ "i_am_a_cow" """)(com.rallyhealth.weepickle.v0.WeePickle.SymbolReader, com.rallyhealth.weepickle.v0.WeePickle.SymbolWriter)
      test("unicode") - rw('叉烧包, """ "叉烧包" """)
      test("null") - rw(null: Symbol, "null")
    }
    test("Long"){
      test("small") - rw(1: Long, """ "1" """)
      test("med") - rw(125123: Long, """ "125123" """)
      test("min") - rw(Int.MinValue.toLong - 1, """ "-2147483649" """)
      test("max") - rw(Int.MaxValue.toLong + 1, """ "2147483648" """)
      test("min") - rw(Long.MinValue, """ "-9223372036854775808" """)
      test("max") - rw(Long.MaxValue, """ "9223372036854775807" """)

    }
    test("BigInt"){
      test("whole") - rw(BigInt("125123"), """ "125123" """)
      test("fractional") - rw(BigInt("1251231542312"), """ "1251231542312" """)
      test("negative") - rw(BigInt("-1251231542312"), """ "-1251231542312" """)
      test("big") - rw(
        BigInt("23420744098430230498023841234712512315423127402740234"),
          """ "23420744098430230498023841234712512315423127402740234" """)
      test("null") - rw(null: BigInt, "null")
      test("abuse cases") {
        test("10k digits") - parses[BigInt](s""" "1${"0" * 9999}" """)
        test("100k digits") - abuseCase[BigInt](s""" "1${"0" * 99999}" """)
      }
    }
    test("BigDecimal"){
      test("whole") - rw(BigDecimal("125123"), """ "125123" """)
      test("fractional") - rw(BigDecimal("125123.1542312"), """ "125123.1542312" """)
      test("negative") - rw(BigDecimal("-125123.1542312"), """ "-125123.1542312" """)
      test("big") - rw(
        BigDecimal("234207440984302304980238412.15423127402740234"),
          """ "234207440984302304980238412.15423127402740234" """)
      test("null") - rw(null: BigDecimal, "null")
      test("json integer") - {
        WeePickle.read[BigDecimal]("123") ==> BigDecimal(123)
      }
      test("json float") - {
        WeePickle.read[BigDecimal]("123.4") ==> BigDecimal(123.4)
      }
      test("abuse cases") {
        /*
        Notes for anyone working with these cases in the future
        - Fiddling with the math context doesn't help because in the too many digits scenarios because it does a full precision big integer
        parse BEFORE turning it into the decimal
        - The performance across different compilation targets is radically different
        - scala.js being ~60x slower for 1m digits than jvm targets
        - If you need to kill a test run make sure you don't leave a stray node process in the background
         */

        test("greater than max int exponential") -abuseCase[BigDecimal](s""" "1E${Integer.MAX_VALUE.toLong + 1}" """)
        test("10k digits integer") - parses[BigDecimal](s""" "1${"0" * 9999}" """)
        test("100k digits integer") - abuseCase[BigDecimal](s""" "1${"0" * 99999}" """)
        test("10k digits after the decimal") - parses[BigDecimal](s""" ".${"9" * 9999}" """)
        test("100k digits after the decimal") - abuseCase[BigDecimal](s""" ".${"9" * 99999}" """)
        test("Not quite max int exponential") - parses[BigDecimal](s""" "1E${Integer.MAX_VALUE - 1}" """)
        // MathContext.UNLIMITED gives you unlimited precision normally you only get 128 bit decimal see [[BigDecimal.defaultMathContext]]
        test("amazingly small") - rw(BigDecimal("0.0000000000000000001", MathContext.UNLIMITED).pow(999))
        // For whatever reason the default java pow doesn't handle negative numbers, and the variant that does isn't exposed in the scala wrapper. Even then you can't do it with unlimited precision
        test("negative exponent") - rw(BigDecimal(1) / BigDecimal(10000000, MathContext.UNLIMITED).pow(999), """ "1E-6993" """)
      }
    }

    test("Int"){
      test("small") - rw(1, "1")
      test("med") - rw(125123, "125123")
      test("min") - rw(Int.MinValue, "-2147483648")
      test("max") - rw(Int.MaxValue, "2147483647")
    }

    test("Double"){
      test("whole") - rw(125123: Double, """125123.0""", """125123""")
      test("wholeLarge") - rw(1475741505173L: Double, """1475741505173.0""", """1475741505173""")
      test("fractional") - rw(125123.1542312, """125123.1542312""")
      test("negative") - rw(-125123.1542312, """-125123.1542312""")
      test("nan") - assert(
        com.rallyhealth.weepickle.v0.WeePickle.write(Double.NaN) == "\"NaN\""
      )
    }

    test("Short"){
      test("simple") - rw(25123: Short, "25123")
      test("min") - rw(Short.MinValue, "-32768")
      test("max") - rw(Short.MaxValue, "32767")
      test("all"){
        for (i <- Short.MinValue to Short.MaxValue) rw(i)
      }
    }

    test("Byte"){
      test("simple") - rw(125: Byte, "125")
      test("min") - rw(Byte.MinValue, "-128")
      test("max") - rw(Byte.MaxValue, "127")
      test("all"){
        for (i <- Byte.MinValue to Byte.MaxValue) rw(i)
      }
    }

    test("Float"){
      test("simple") - rw(125.125f, """125.125""")
      test("max") - rw(Float.MaxValue)
      test("min") - rw(Float.MinValue)
      test("minPos") - rw(Float.MinPositiveValue)
      test("inf") - rw(Float.PositiveInfinity, """ "Infinity" """)
      "neg-inf" - rw(Float.NegativeInfinity, """ "-Infinity" """)
      test("nan") - assert(
        com.rallyhealth.weepickle.v0.WeePickle.write(Float.NaN) == "\"NaN\""
      )
    }

    test("Char"){
      test("f") - rwNoBinaryJson('f', """ "f" """)
      test("plus") - rwNoBinaryJson('+', """ "+" """)

      test("all"){
        for(i <- Char.MinValue until 55296/*Char.MaxValue*/) rwNoBinaryJson(i)
      }
    }

    test("URI") - rw(URI.create("http://www.example.com/path?query=1&param=two#frag"), "\"http://www.example.com/path?query=1&param=two#frag\"")
  }
}
