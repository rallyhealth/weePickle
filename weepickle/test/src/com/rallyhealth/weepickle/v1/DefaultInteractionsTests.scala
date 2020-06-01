package com.rallyhealth.weepickle.v1

import com.rallyhealth.weejson.v1.WeeJson
import com.rallyhealth.weejson.v1.jackson.{FromJson, ToJson}
import com.rallyhealth.weepickle.v1.WeePickle.{From, FromScala, FromTo, OptionFrom, To, ToOption, ToValue, macroFromTo, to}
import com.rallyhealth.weepickle.v1.implicits.dropDefault
import utest._

/**
  * Tests the interaction of various features affecting how defaults are handled.
  * - Values may use default From/To type classes, or client may have something higher priority in scope
  * - Defaults may be explicit stated or derived. E.g., unless stated explicitly, [[Option]]s
  *   are assumed to be [[None]] by default
  * - Inbound data may include data or data may be missing (when default should be applied)
  * - Outbound data may retain or drop defaults based on presence/absence of @dropDefault
  *
  * The interactions can be subtle. For example, WeePickle automatically assumed [[None]] as a default for
  * option types, but, until recently, only respected the [[None]] default for @dropDefault if [[None]] is
  * stated explicitly (below in simpleInteractions). Another weird interaction was when you defined a
  * non-default implicit [[To]]/[[From]] which maps to/from a default. The @dropDefault logic respected
  * the value before to the application of the from/to logic rather than after it, leading to surprising
  * results, e.g., values that should have been dropped were not. These tests are meant to cover these
  * interactions demonstrating these issues no longer exist in the code.
  */
object DefaultInteractionsTests extends TestSuite {
  case class SimpleTestData(
    @dropDefault explicitDefault: Option[String] = None,
    @dropDefault derivedDefault: Option[String]
  )
  object SimpleTestData {
    implicit val pickler: FromTo[SimpleTestData] = macroFromTo
  }

  case class ComplexTestData(
    @dropDefault nonBlankDefaultNoneDefaultDropped: Option[String] = None,
    @dropDefault blankDefaultNoneDefaultDropped: Option[String] = None,
    @dropDefault noneDefaultNoneDefaultDropped: Option[String] = None,
    @dropDefault missingDefaultNoneDefaultDropped: Option[String] = None,
    nonBlankDefaultNone: Option[String] = None,
    blankDefaultNone: Option[String] = None,
    noneDefaultNone: Option[String] = None,
    missingDefaultNone: Option[String] = None,
    @dropDefault nonBlankDefaultDropped: Option[String],
    @dropDefault blankDefaultDropped: Option[String],
    @dropDefault noneDefaultDropped: Option[String],
    @dropDefault missingDefaultDropped: Option[String],
    nonBlank: Option[String],
    blank: Option[String],
    none: Option[String],
    missing: Option[String]
  )

  /*
   * Treat blanks as None
   */
  object ComplexTestData {

    implicit def OptionStringFrom: From[Option[String]] = OptionFrom[String].comap {
      case Some("") => None
      case other    => other
    }

    implicit def ToOptionString: To[Option[String]] = ToOption[String].map {
      case Some("") => None
      case other    => other
    }

    implicit val pickler: FromTo[ComplexTestData] = macroFromTo
  }

  val tests = Tests {
    test("simpleInteractions") {
      val emptyJson = WeeJson.read("{}")
      val emptyData = SimpleTestData(derivedDefault = None)

      test("defaults applied json to scala") {
        val resultData = emptyJson.transform(to[SimpleTestData])
        assert(emptyData == resultData)
      }

      test("defaults/drops scala to json") {
        val resultJson = FromScala(emptyData).transform(ToValue)
        assert(emptyJson == resultJson)
      }
    }

    test("complexInteractions") {

      val testWithBlanks = ComplexTestData(
        nonBlankDefaultNoneDefaultDropped = Some("something"),
        blankDefaultNoneDefaultDropped = Some(""),
        nonBlankDefaultNone = Some("something"),
        blankDefaultNone = Some(""),
        nonBlankDefaultDropped = Some("something"),
        blankDefaultDropped = Some(""),
        noneDefaultDropped = None,
        missingDefaultDropped = None,
        nonBlank = Some("something"),
        blank = Some(""),
        none = None,
        missing = None
      )

      /*
       * Round-trip conversions result in Nones where there were blanks.
       * Everything else is preserved.
       */
      val testWithBlanksAsNone = testWithBlanks.copy(
        blankDefaultNoneDefaultDropped = None,
        blankDefaultNone = None,
        blankDefaultDropped = None,
        blank = None
      )

      /*
       * These are dropped -- tagged with @dropDefault, explicitly or implicitly (through blank logic)
       * have a value of None, and have explicit or derived default of None:
       *  - blankDefaultNoneDefaultDropped -- implicitly None (custom), explicit default None
       *  - noneDefaultNoneDefaultDropped -- explicitly None, explicit default None
       *  - missingDefaultNoneDefaultDropped  -- implicitly None (missing), explicit default None
       *  - blankDefaultDropped  -- implicitly None (custom), derived default None
       *  - noneDefaultDropped  -- explicitly None, derived default None
       *  - missingDefaultDropped  -- implicitly None (missing), derived default None
       */
      val expectedJsonOut =
        s"""{
           |  "nonBlankDefaultNoneDefaultDropped": "something",
           |  "nonBlankDefaultNone": "something",
           |  "blankDefaultNone": null,
           |  "noneDefaultNone": null,
           |  "missingDefaultNone": null,
           |  "nonBlankDefaultDropped": "something",
           |  "nonBlank": "something",
           |  "blank": null,
           |  "none": null,
           |  "missing": null
           |}
           |""".stripMargin

      val minJsonIn =
        s"""{
           |  "nonBlankDefaultNoneDefaultDropped": "something",
           |  "nonBlankDefaultNone": "something",
           |  "nonBlankDefaultDropped": "something",
           |  "nonBlank": "something"
           |}
           |""".stripMargin

      val testWithBlanksAsJson = FromScala(testWithBlanks).transform(ToJson.string)
      val testWithNonesAsJson = FromScala(testWithBlanksAsNone).transform(ToJson.string)

      test("defaults/drops applied from test with blanks") {
        assert(
          FromJson(expectedJsonOut).transform(ToValue) ==
            FromJson(testWithBlanksAsJson).transform(ToValue)
        )
      }

      test("defaults/drops applied from test with blanks forced as Nones") {
        assert(
          FromJson(expectedJsonOut).transform(ToValue) ==
            FromJson(testWithNonesAsJson).transform(ToValue)
        )
      }

      test("defaults applied back (from data with blanks)") {
        val withBlanksBack = FromJson(testWithBlanksAsJson).transform(to[ComplexTestData])
        assert(withBlanksBack == testWithBlanksAsNone)
        assert(
          FromScala(withBlanksBack).transform(ToValue) ==
            FromJson(testWithBlanksAsJson).transform(ToValue)
        )
      }

      test("defaults applied back (from data with Nones)") {
        val withNonesBack = FromJson(testWithNonesAsJson).transform(to[ComplexTestData])
        assert(withNonesBack == testWithBlanksAsNone)
        assert(
          FromScala(withNonesBack).transform(ToValue) ==
            FromJson(testWithBlanksAsJson).transform(ToValue)
        )
      }

      test("defaults applied back (from expected out)") {
        val withExpectations = FromJson(expectedJsonOut).transform(to[ComplexTestData])
        assert(withExpectations == testWithBlanksAsNone)
        assert(
          FromScala(withExpectations).transform(ToValue) ==
            FromJson(testWithBlanksAsJson).transform(ToValue)
        )
      }

      test("defaults applied back (from min)") {
        val withMin = FromJson(minJsonIn).transform(to[ComplexTestData])
        assert(withMin == testWithBlanksAsNone)
        assert(
          FromScala(withMin).transform(ToValue) ==
            FromJson(testWithBlanksAsJson).transform(ToValue)
        )
      }
    }
  }
}
