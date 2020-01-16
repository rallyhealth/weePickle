package com.rallyhealth.weepickle.v1

import com.rallyhealth.weepickle.v1.ADTs.ADT0
import com.rallyhealth.weepickle.v1.Common.{Data, bench}
import com.rallyhealth.weepickle.v1.Defaults.ADTc
import com.rallyhealth.weepickle.v1.Hierarchy.{A, B, C}
import com.rallyhealth.weepickle.v1.Recursive.{End, LL, Node}
import scala.scalajs.js
object Main{
  def main(args: Array[String]): Unit = {
    for(duration <- Seq(500, 5000, 25000)){
      println("RUN JS: " + duration)
      println()
      rawJsonParseSerialize(duration)
      Common.playJson(duration)
      Common.circe(duration)
      Common.weepickleDefault(duration)
      Common.weepickleBinaryDefault(duration)
//      Common.genCodec(duration)
      weepickleWebDefault(duration)
      Common.playJsonCached(duration)
      Common.circeCached(duration)
      Common.weepickleDefaultCached(duration)
      Common.weepickleDefaultBinaryCached(duration)
//      Common.genCodecCached(duration)
      weepickleWebDefaultCached(duration)
      println()
    }
  }

  def rawJsonParseSerialize(duration: Int) = {

    Common.bench0[String, js.Any](duration, Common.benchmarkSampleJson)(
      js.JSON.parse(_),
      js.JSON.stringify(_)
    )
  }
  def weepickleWebDefault(duration: Int) = {
    import com.rallyhealth.weepickle.v1.WeePickle.{ReaderWriter => RW}
    implicit def rw1: RW[Data] = com.rallyhealth.weepickle.v1.WeePickle.macroRW
    implicit def rw2: RW[A] = com.rallyhealth.weepickle.v1.WeePickle.macroRW
    implicit def rw3: RW[B] = com.rallyhealth.weepickle.v1.WeePickle.macroRW
    implicit def rw4: RW[C] = com.rallyhealth.weepickle.v1.WeePickle.macroRW
    implicit def rw5: RW[LL] = com.rallyhealth.weepickle.v1.WeePickle.macroRW
    implicit def rw6: RW[Node] = com.rallyhealth.weepickle.v1.WeePickle.macroRW
    implicit def rw7: RW[End.type] = com.rallyhealth.weepickle.v1.WeePickle.macroRW
    implicit def rw8: RW[ADTc] = com.rallyhealth.weepickle.v1.WeePickle.macroRW
    implicit def rw9: RW[ADT0] = com.rallyhealth.weepickle.v1.WeePickle.macroRW
    bench[String](duration)(
      com.rallyhealth.weepickle.v1.WeePickle.web.read[Seq[Data]],
      com.rallyhealth.weepickle.v1.WeePickle.web.write(_)
    )
  }
  def weepickleWebDefaultCached(duration: Int) = {
    import com.rallyhealth.weepickle.v1.WeePickle.{ReaderWriter => RW}
    implicit lazy val rw1: RW[Data] = com.rallyhealth.weepickle.v1.WeePickle.macroRW
    implicit lazy val rw2: RW[A] = com.rallyhealth.weepickle.v1.WeePickle.macroRW
    implicit lazy val rw3: RW[B] = com.rallyhealth.weepickle.v1.WeePickle.macroRW
    implicit lazy val rw4: RW[C] = com.rallyhealth.weepickle.v1.WeePickle.macroRW
    implicit lazy val rw5: RW[LL] = com.rallyhealth.weepickle.v1.WeePickle.macroRW
    implicit lazy val rw6: RW[Node] = com.rallyhealth.weepickle.v1.WeePickle.macroRW
    implicit lazy val rw7: RW[End.type] = com.rallyhealth.weepickle.v1.WeePickle.macroRW
    implicit lazy val rw8: RW[ADTc] = com.rallyhealth.weepickle.v1.WeePickle.macroRW
    implicit lazy val rw9: RW[ADT0] = com.rallyhealth.weepickle.v1.WeePickle.macroRW
    bench[String](duration)(
      com.rallyhealth.weepickle.v1.WeePickle.web.read[Seq[Data]],
      com.rallyhealth.weepickle.v1.WeePickle.web.write(_)
    )
  }
}
