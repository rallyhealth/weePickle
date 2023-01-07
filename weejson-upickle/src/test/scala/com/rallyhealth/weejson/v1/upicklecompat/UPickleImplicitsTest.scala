package com.rallyhealth.weejson.v1.upicklecompat

import com.rallyhealth.weejson.v1.jackson.{FromJson, ToJson}
import com.rallyhealth.weejson.v1.upicklecompat.UPickleImplicits._
import com.rallyhealth.weepickle.v1.WeePickle
import com.rallyhealth.weepickle.v1.WeePickle.{FromScala, ToScala, FromTo}
import upickle.default.{ReadWriter, read, write}
import utest._

object UPickleImplicitsTest extends TestSuite {

  case class Cow(
    b: Boolean,
    i: Int,
    l: Long,
    d: Double,
    s: String,
    c: Char
  )

  override val tests: Tests = Tests {

    val cow = Cow(true, 42, -1L, 3.14d, "lol i am a cow", '!')

    def roundtrip()(implicit rw: ReadWriter[Cow], pickler: WeePickle.FromTo[Cow]): Cow = {
      val json = FromScala(cow).transform(ToJson.string)
      json ==> write(cow)

      read[Cow](json) ==> cow
      FromJson(json).transform(ToScala[Cow]) ==> cow

      FromScala(cow).transform(ToScala[Cow]) ==> cow
      upickle.default.transform(cow).to[Cow]
    }

    test("upickle as weepickle") {
      implicit val rw: ReadWriter[Cow] = upickle.default.macroRW
      implicit val pickler: FromTo[Cow] = rw.asFromTo
      roundtrip()
    }

    test("weepickle as upickle") {
      implicit val pickler: FromTo[Cow] = WeePickle.macroFromTo
      implicit val rw: ReadWriter[Cow] = pickler.asReadWriter
      roundtrip()
    }
  }
}
