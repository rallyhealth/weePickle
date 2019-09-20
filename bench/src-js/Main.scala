package com.rallyhealth.upickle.v1

import com.rallyhealth.upickle.v1.ADTs.ADT0
import com.rallyhealth.upickle.v1.Common.{Data, bench}
import com.rallyhealth.upickle.v1.Defaults.ADTc
import com.rallyhealth.upickle.v1.Hierarchy.{A, B, C}
import com.rallyhealth.upickle.v1.Recursive.{End, LL, Node}
import scala.scalajs.js
object Main{
  def main(args: Array[String]): Unit = {
    for(duration <- Seq(500, 5000, 25000)){
      println("RUN JS: " + duration)
      println()
      rawJsonParseSerialize(duration)
      Common.playJson(duration)
      Common.circe(duration)
      Common.upickleDefault(duration)
      Common.upickleBinaryDefault(duration)
//      Common.genCodec(duration)
      upickleWebDefault(duration)
      Common.playJsonCached(duration)
      Common.circeCached(duration)
      Common.upickleDefaultCached(duration)
      Common.upickleDefaultBinaryCached(duration)
//      Common.genCodecCached(duration)
      upickleWebDefaultCached(duration)
      println()
    }
  }

  def rawJsonParseSerialize(duration: Int) = {

    Common.bench0[String, js.Any](duration, Common.benchmarkSampleJson)(
      js.JSON.parse(_),
      js.JSON.stringify(_)
    )
  }
  def upickleWebDefault(duration: Int) = {
    import com.rallyhealth.upickle.v1.default.{ReadWriter => RW}
    implicit def rw1: RW[Data] = com.rallyhealth.upickle.v1.default.macroRW
    implicit def rw2: RW[A] = com.rallyhealth.upickle.v1.default.macroRW
    implicit def rw3: RW[B] = com.rallyhealth.upickle.v1.default.macroRW
    implicit def rw4: RW[C] = com.rallyhealth.upickle.v1.default.macroRW
    implicit def rw5: RW[LL] = com.rallyhealth.upickle.v1.default.macroRW
    implicit def rw6: RW[Node] = com.rallyhealth.upickle.v1.default.macroRW
    implicit def rw7: RW[End.type] = com.rallyhealth.upickle.v1.default.macroRW
    implicit def rw8: RW[ADTc] = com.rallyhealth.upickle.v1.default.macroRW
    implicit def rw9: RW[ADT0] = com.rallyhealth.upickle.v1.default.macroRW
    bench[String](duration)(
      com.rallyhealth.upickle.v1.default.web.read[Data],
      com.rallyhealth.upickle.v1.default.web.write(_)
    )
  }
  def upickleWebDefaultCached(duration: Int) = {
    import com.rallyhealth.upickle.v1.default.{ReadWriter => RW}
    implicit lazy val rw1: RW[Data] = com.rallyhealth.upickle.v1.default.macroRW
    implicit lazy val rw2: RW[A] = com.rallyhealth.upickle.v1.default.macroRW
    implicit lazy val rw3: RW[B] = com.rallyhealth.upickle.v1.default.macroRW
    implicit lazy val rw4: RW[C] = com.rallyhealth.upickle.v1.default.macroRW
    implicit lazy val rw5: RW[LL] = com.rallyhealth.upickle.v1.default.macroRW
    implicit lazy val rw6: RW[Node] = com.rallyhealth.upickle.v1.default.macroRW
    implicit lazy val rw7: RW[End.type] = com.rallyhealth.upickle.v1.default.macroRW
    implicit lazy val rw8: RW[ADTc] = com.rallyhealth.upickle.v1.default.macroRW
    implicit lazy val rw9: RW[ADT0] = com.rallyhealth.upickle.v1.default.macroRW
    bench[String](duration)(
      com.rallyhealth.upickle.v1.default.web.read[Data],
      com.rallyhealth.upickle.v1.default.web.write(_)
    )
  }
}
