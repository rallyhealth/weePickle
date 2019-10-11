package com.rallyhealth.weepickle.v0.example

import java.io.StringWriter

import acyclic.file
import com.rallyhealth.ujson.v0.json4s.Json4sJson
import com.rallyhealth.weepickle.v0.TestUtil
import utest._
import com.rallyhealth.ujson.v0.StringRenderer

import Simple._

object JvmExampleTests extends TestSuite {

  import TestUtil._
  val tests = Tests {
    test("sources"){
      import com.rallyhealth.weepickle.v0.default._
      val original = """{"myFieldA":1,"myFieldB":"gg"}"""

      import java.nio.file.Files
      val f = Files.createTempFile("", "")
      Files.write(f, original.getBytes)

      read[Thing](f) ==> Thing(1, "gg")
      read[Thing](f.toFile) ==> Thing(1, "gg")
      read[Thing](Files.newByteChannel(f)) ==> Thing(1, "gg")
    }
    test("other"){
      test("argonaut"){
        import com.rallyhealth.ujson.v0.argonaut.ArgonautJson
        val argJson: argonaut.Json = ArgonautJson(
          """["hello", "world"]"""
        )

        val updatedArgJson = argJson.withArray(_.map(_.withString(_.toUpperCase)))

        val items: Seq[String] = ArgonautJson.transform(
          updatedArgJson,
          com.rallyhealth.weepickle.v0.default.reader[Seq[String]]
        )

        items ==> Seq("HELLO", "WORLD")

        val rewritten = com.rallyhealth.weepickle.v0.default.transform(items).to(ArgonautJson)

        val stringified = ArgonautJson.transform(rewritten, StringRenderer()).toString

        stringified ==> """["HELLO","WORLD"]"""
      }
      test("circe"){
        import com.rallyhealth.ujson.v0.circe.CirceJson
        val circeJson: io.circe.Json = CirceJson(
          """["hello", "world"]"""
        )

        val updatedCirceJson =
          circeJson.mapArray(_.map(x => x.mapString(_.toUpperCase)))

        val items: Seq[String] = CirceJson.transform(
          updatedCirceJson,
          com.rallyhealth.weepickle.v0.default.reader[Seq[String]]
        )

        items ==> Seq("HELLO", "WORLD")

        val rewritten = com.rallyhealth.weepickle.v0.default.transform(items).to(CirceJson)

        val stringified = CirceJson.transform(rewritten, StringRenderer()).toString

        stringified ==> """["HELLO","WORLD"]"""
      }
      test("json4s"){
        import org.json4s.JsonAST
        val json4sJson: JsonAST.JValue = Json4sJson(
          """["hello", "world"]"""
        )

        val updatedJson4sJson = JsonAST.JArray(
          for(v <- json4sJson.children)
            yield JsonAST.JString(v.values.toString.toUpperCase())
        )

        val items: Seq[String] = Json4sJson.transform(
          updatedJson4sJson,
          com.rallyhealth.weepickle.v0.default.reader[Seq[String]]
        )

        items ==> Seq("HELLO", "WORLD")

        val rewritten = com.rallyhealth.weepickle.v0.default.transform(items).to(Json4sJson)

        val stringified = Json4sJson.transform(rewritten, StringRenderer()).toString

        stringified ==> """["HELLO","WORLD"]"""
      }
      test("playJson"){
        import com.rallyhealth.ujson.v0.play.PlayJson
        import play.api.libs.json._
        val playJson: play.api.libs.json.JsValue = PlayJson(
          """["hello", "world"]"""
        )

        val updatedPlayJson = JsArray(
          for(v <- playJson.as[JsArray].value)
            yield JsString(v.as[String].toUpperCase())
        )

        val items: Seq[String] = PlayJson.transform(
          updatedPlayJson,
          com.rallyhealth.weepickle.v0.default.reader[Seq[String]]
        )

        items ==> Seq("HELLO", "WORLD")

        val rewritten = com.rallyhealth.weepickle.v0.default.transform(items).to(PlayJson)

        val stringified = PlayJson.transform(rewritten, StringRenderer()).toString

        stringified ==> """["HELLO","WORLD"]"""
      }
      test("crossAst"){
        import com.rallyhealth.ujson.v0.circe.CirceJson
        val circeJson: io.circe.Json = CirceJson(
          """["hello", "world"]"""
        )

        val updatedCirceJson =
          circeJson.mapArray(_.map(x => x.mapString(_.toUpperCase)))

        import com.rallyhealth.ujson.v0.play.PlayJson
        import play.api.libs.json._

        val playJson: play.api.libs.json.JsValue = CirceJson.transform(
          updatedCirceJson,
          PlayJson
        )

        val updatedPlayJson = JsArray(
          for(v <- playJson.as[JsArray].value)
            yield JsString(v.as[String].reverse)
        )

        val stringified = PlayJson.transform(updatedPlayJson, StringRenderer()).toString

        stringified ==> """["OLLEH","DLROW"]"""
      }
    }

  }
}


