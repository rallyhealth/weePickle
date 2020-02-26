package com.rallyhealth.weejson.v1.play

import com.rallyhealth.weejson.v1.jackson.FromJson
import com.rallyhealth.weepickle.v1.WeePickle._
import com.rallyhealth.weepickle.v1.core.ValidatingVisitor.{Errors, Ops}
import org.scalatest.{FunSpec, Matchers}

class ValidatingVisitorSpec extends FunSpec with Matchers {

  private def errorsToMap(es: Errors): Map[String, String] =
    es.map(jpe => jpe.getMessage -> jpe.getCause.getMessage).toMap

  case class C(
    cInt: Int
  )

  case class B(
    bString: String,
    bMaybeC: Option[C] = None
  )

  case class A(
    aString: String,
    aMaybeB: Option[B],
    aC: C,
    aListB: List[B],
    aListC: List[C]
  )

  implicit val cFromTo: FromTo[C] = macroFromTo
  implicit val bFromTo: FromTo[B] = macroFromTo
  implicit val aFromTo: FromTo[A] = macroFromTo

  describe("when validating a value") {
    val a = A("hi", Some(B("world", Some(C(0)))), C(1), List(B("."), B(".")), List(C(2), C(3)))
    val aMin = A("hi", None, C(1), List(), List())

    //import com.rallyhealth.weejson.v1.jackson.ToJson
    //val js = FromScala(a).transform(ToJson.string)
    //log.info(s"a js=$js")
    //val jsMin = FromScala(aMin).transform(ToJson.string)
    //log.info(s"a jsMin=$jsMin")

    it("does transform through value AST") {
      FromScala(a).transform(to[A].returningAllErrors) match {
        case Right(v) => v shouldBe a
        case Left(es) => fail(s"bummer: $es")
      }
    }

    it("does transform through js string to A") {
      val js =
        """{
          | "aString":"hi",
          | "aMaybeB":{"bString":"world","bMaybeC":{"cInt":0}},
          | "aC":{"cInt":1},
          | "aListB":[{"bString":"."}, {"bString":"."}],
          | "aListC":[{"cInt":2}, {"cInt":3}]
          |}""".stripMargin
      FromJson(js).transform(to[A].returningAllErrors) match {
        case Right(v) => v shouldBe a
        case Left(es) => fail(s"bummer: $es")
      }
    }

    it("does transform even if there are extra fields") {
      val jsWithBogus =
        """{
          | "aString":"hi", "bogus": true,
          | "aMaybeB":{"bString":"world", "bogus": true,"bMaybeC":{"cInt":0, "bogus": true}},
          | "aC":{"cInt":1, "bogus": true},
          | "aListB":[{"bString":".", "bogus": true},{"bString":"."}],
          | "aListC":[{"cInt":2, "bogus": true},{"cInt":3}]
          |}""".stripMargin
      FromJson(jsWithBogus).transform(to[A].returningAllErrors) match {
        case Right(v) => v shouldBe a
        case Left(es) => fail(s"bummer: $es")
      }
    }

    it("does transform through with min fields") {
      val jsMin =
        """{
          | "aString":"hi",
          | "aC":{"cInt":1},
          | "aListB":[],
          | "aListC":[]
          |}""".stripMargin
      FromJson(jsMin).transform(to[A].returningAllErrors) match {
        case Right(v) => v shouldBe aMin
        case Left(es) => fail(s"bummer: $es")
      }
    }

    it("detects when top-level field is missing") {
      val jsTopMissing =
        """{
          | "aMaybeB":{"bString":"world","bMaybeC":{"cInt":0}},
          | "aC":{"cInt":1},
          | "aListB":[{"bString":"."},{"bString":"."}],
          | "aListC":[{"cInt":2},{"cInt":3}]
          |}""".stripMargin
      val expectedErrors = Map("" -> "missing keys in dictionary: aString")
      FromJson(jsTopMissing).transform(to[A].returningAllErrors) match {
        case Right(_) => fail("didn't detect missing aString")
        case Left(es) => errorsToMap(es) should contain theSameElementsAs expectedErrors
      }
    }

    it("detects when top-level field is missing and type is bad") {
      val jsTopMissingBadType =
        """{
          | "aMaybeB":{"bString":"world","bMaybeC":{"cInt":0}},
          | "aC":true,
          | "aListB":[{"bString":"."},{"bString":"."}],
          | "aListC":[{"cInt":2},{"cInt":3}]
          |}""".stripMargin
      val expectedErrors = Map(
        "" -> "missing keys in dictionary: aString, aC",
        "/aC" -> "expected dictionary got boolean"
      )
      FromJson(jsTopMissingBadType).transform(to[A].returningAllErrors) match {
        case Right(_) => fail("didn't detect missing aString")
        case Left(es) => errorsToMap(es) should contain theSameElementsAs expectedErrors
      }
    }

    it("detects when lower-level field is missing") {
      val jsSubMissing =
        """{
          | "aString":"hi",
          | "aMaybeB":{},
          | "aC":{"cInt":1},
          | "aListB":[{"bString":"."},{"bString":"."}],
          | "aListC":[{"cInt":2},{"cInt":3}]
          |}""".stripMargin
      val expectedErrors = Map("/aMaybeB" -> "missing keys in dictionary: bString")
      FromJson(jsSubMissing).transform(to[A].returningAllErrors) match {
        case Right(_) => fail("didn't detect missing bString")
        case Left(es) => errorsToMap(es) should contain theSameElementsAs expectedErrors
      }
    }

    it("detects when fields have the wrong types") {
      val jsMessedUpTypes =
        """{
          | "aString":0,
          | "aMaybeB":{"bString":0,"bMaybeC":{"cInt":0}},
          | "aC":{"cInt":true},
          | "aListB":[{"bString":false},{"bString":1}],
          | "aListC":[{"cInt":"2"},{"cInt":"3"}]
          |}""".stripMargin
      val expectedErrors = Map(
        "/aString" -> "expected string got int32",
        "/aMaybeB/bString" -> "expected string got int32",
        "/aC/cInt" -> "expected number got boolean",
        "/aListB/0/bString" -> "expected string got boolean",
        "/aListB/1/bString" -> "expected string got int32",
        "/aListC/0/cInt" -> "expected number got string",
        "/aListC/1/cInt" -> "expected number got string",
        // and because types are wrong, fields show as missing too
        "" -> "missing keys in dictionary: aString, aC, aListB, aListC",
        "/aListB/1" -> "missing keys in dictionary: bString",
        "/aListC/0" -> "missing keys in dictionary: cInt",
        "/aC" -> "missing keys in dictionary: cInt",
        "/aMaybeB" -> "missing keys in dictionary: bString",
        "/aListB/0" -> "missing keys in dictionary: bString",
        "/aListC/1" -> "missing keys in dictionary: cInt"
      )
      FromJson(jsMessedUpTypes).transform(to[A].returningAllErrors) match {
        case Right(_) => fail("didn't detect missing bString")
        case Left(es) => errorsToMap(es) should contain theSameElementsAs expectedErrors
      }
    }
  }
}
