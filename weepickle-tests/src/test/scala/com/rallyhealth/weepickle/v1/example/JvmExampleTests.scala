package com.rallyhealth.weepickle.v1.example

import java.io.StringWriter

import acyclic.file
import com.rallyhealth.weejson.v1.json4s.Json4sJson
import com.rallyhealth.weepickle.v1._
import utest._
import com.rallyhealth.weejson.v1.StringRenderer
import Simple._
import com.rallyhealth.weejson.v1.jackson.FromJson
import com.rallyhealth.weepickle.v1.WeePickle._

object JvmExampleTests extends TestSuite {

  import TestUtil._
  val tests = Tests {
    test("sources") {
      val original = """{"myFieldA":1,"myFieldB":"gg"}"""

      import java.nio.file.Files
      val f = Files.createTempFile("", "")
      Files.write(f, original.getBytes)

      FromJson(f).transform(ToScala[Thing]) ==> Thing(1, "gg")
      FromJson(f.toFile).transform(ToScala[Thing]) ==> Thing(1, "gg")
    }
    test("other") {
      test("argonaut") {
        import com.rallyhealth.weejson.v1.argonaut.ArgonautJson
        val argJson: argonaut.Json = FromJson("""["hello", "world"]""").transform(ArgonautJson)

        val updatedArgJson = argJson.withArray(_.map(_.withString(_.toUpperCase)))

        val items: Seq[String] = ArgonautJson.transform(
          updatedArgJson,
          com.rallyhealth.weepickle.v1.WeePickle.to[Seq[String]]
        )

        items ==> Seq("HELLO", "WORLD")

        val rewritten = FromScala(items).transform(ArgonautJson)

        val stringified = ArgonautJson.transform(rewritten, StringRenderer()).toString

        stringified ==> """["HELLO","WORLD"]"""
      }
      test("circe") {
        import com.rallyhealth.weejson.v1.circe.CirceJson
        val circeJson: io.circe.Json = FromJson("""["hello", "world"]""").transform(CirceJson)

        val updatedCirceJson =
          circeJson.mapArray(_.map(x => x.mapString(_.toUpperCase)))

        val items: Seq[String] = CirceJson.transform(
          updatedCirceJson,
          com.rallyhealth.weepickle.v1.WeePickle.to[Seq[String]]
        )

        items ==> Seq("HELLO", "WORLD")

        val rewritten = FromScala(items).transform(CirceJson)

        val stringified = CirceJson.transform(rewritten, StringRenderer()).toString

        stringified ==> """["HELLO","WORLD"]"""
      }
      test("json4s") {
        import org.json4s.JsonAST
        val json4sJson: JsonAST.JValue = FromJson("""["hello", "world"]""").transform(Json4sJson)

        val updatedJson4sJson = JsonAST.JArray(
          for (v <- json4sJson.children)
            yield JsonAST.JString(v.values.toString.toUpperCase())
        )

        val items: Seq[String] = Json4sJson.transform(
          updatedJson4sJson,
          com.rallyhealth.weepickle.v1.WeePickle.to[Seq[String]]
        )

        items ==> Seq("HELLO", "WORLD")

        val rewritten = FromScala(items).transform(Json4sJson)

        val stringified = Json4sJson.transform(rewritten, StringRenderer()).toString

        stringified ==> """["HELLO","WORLD"]"""
      }
      test("playJson") {
        import com.rallyhealth.weejson.v1.play.PlayJson
        import play.api.libs.json._
        val playJson: play.api.libs.json.JsValue = FromJson("""["hello", "world"]""").transform(PlayJson)

        val updatedPlayJson = JsArray(
          for (v <- playJson.as[JsArray].value)
            yield JsString(v.as[String].toUpperCase())
        )

        val items: Seq[String] = PlayJson.transform(
          updatedPlayJson,
          com.rallyhealth.weepickle.v1.WeePickle.to[Seq[String]]
        )

        items ==> Seq("HELLO", "WORLD")

        val rewritten = FromScala(items).transform(PlayJson)

        val stringified = PlayJson.transform(rewritten, StringRenderer()).toString

        stringified ==> """["HELLO","WORLD"]"""
      }
      test("crossAst") {
        import com.rallyhealth.weejson.v1.circe.CirceJson
        val circeJson: io.circe.Json = FromJson("""["hello", "world"]""").transform(CirceJson)

        val updatedCirceJson =
          circeJson.mapArray(_.map(x => x.mapString(_.toUpperCase)))

        import com.rallyhealth.weejson.v1.play.PlayJson
        import play.api.libs.json._

        val playJson: play.api.libs.json.JsValue = CirceJson.transform(
          updatedCirceJson,
          PlayJson
        )

        val updatedPlayJson = JsArray(
          for (v <- playJson.as[JsArray].value)
            yield JsString(v.as[String].reverse)
        )

        val stringified = PlayJson.transform(updatedPlayJson, StringRenderer()).toString

        stringified ==> """["OLLEH","DLROW"]"""
      }
    }

  }
}
