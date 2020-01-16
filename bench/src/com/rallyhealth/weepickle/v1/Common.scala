package com.rallyhealth.weepickle.v1

import java.nio.charset.StandardCharsets.UTF_8

object Common{
  import ADTs.ADT0
  import Defaults._
  import Generic.ADT
  import Hierarchy._
  import Recursive._
  type Data = ADT[Seq[(Int, Int)], String, A, LL, ADTc, ADT0]
  val benchmarkSampleData: Seq[Data] = Seq.fill(1000)(ADT(
    Vector((1, 2), (3, 4), (4, 5), (6, 7), (8, 9), (10, 11), (12, 13)),
    """
      |I am cow, hear me moo
      |I weigh twice as much as you
      |And I look good on the barbecueeeee
    """.stripMargin,
    C("lol i am a noob", "haha you are a noob"): A,
    Node(-11, Node(-22, Node(-33, Node(-44, End)))): LL,
    ADTc(i = 1234567890, s = "i am a strange loop"),
    ADT0()
  ))
  val benchmarkSampleJson = com.rallyhealth.weepickle.v1.WeePickle.write(benchmarkSampleData)
  val benchmarkSampleJsonBytes = benchmarkSampleJson.getBytes(UTF_8)
  val benchmarkSampleMsgPack = com.rallyhealth.weepickle.v1.WeePickle.writeMsgPack(benchmarkSampleData)

//  println("benchmarkSampleJson " + benchmarkSampleJson.size + " bytes")
//  println("benchmarkSampleMsgPack " + benchmarkSampleMsgPack.size + " bytes")
  def circe(duration: Int) = {
    import io.circe._
    import io.circe.generic.semiauto._
    import io.circe.parser._

    implicit def _r1: Decoder[Data] = deriveDecoder
    implicit def _r2: Decoder[A] = deriveDecoder
    implicit def _r3: Decoder[B] = deriveDecoder
    implicit def _r4: Decoder[C] = deriveDecoder
    implicit def _r5: Decoder[LL] = deriveDecoder
    implicit def _r6: Decoder[Node] = deriveDecoder
    implicit def _r7: Decoder[End.type] = deriveDecoder
    implicit def _r8: Decoder[ADTc] = deriveDecoder
    implicit def _r9: Decoder[ADT0] = deriveDecoder

    implicit def _w1: Encoder[Data] = deriveEncoder
    implicit def _w2: Encoder[A] = deriveEncoder
    implicit def _w3: Encoder[B] = deriveEncoder
    implicit def _w4: Encoder[C] = deriveEncoder
    implicit def _w5: Encoder[LL] = deriveEncoder
    implicit def _w6: Encoder[Node] = deriveEncoder
    implicit def _w7: Encoder[End.type] = deriveEncoder
    implicit def _w8: Encoder[ADTc] = deriveEncoder
    implicit def _w9: Encoder[ADT0] = deriveEncoder

    bench[String](duration)(
      decode[Seq[Data]](_).right.get,
      implicitly[Encoder[Seq[Data]]].apply(_).toString()
    )

  }

  def playJson(duration: Int) = {
    import play.api.libs.json._
    implicit def rw1: Format[Data] = play.api.libs.json.Json.format
    implicit def rw2: Format[A] = play.api.libs.json.Json.format
    implicit def rw3: Format[B] = play.api.libs.json.Json.format
    implicit def rw4: Format[C] = play.api.libs.json.Json.format
    implicit def rw5: Format[LL] = play.api.libs.json.Json.format
    implicit def rw6: Format[Node] = play.api.libs.json.Json.format
    implicit def rw7: Format[End.type] = new Format[End.type] {
      def reads(json: JsValue) = JsSuccess(End)

      def writes(o: Recursive.End.type) = JsObject(Nil)
    }
    implicit def rw8: Format[ADTc] = play.api.libs.json.Json.format
    implicit def rw9: Format[ADT0] = new Format[ADT0] {
      def reads(json: JsValue) = JsSuccess(ADT0())

      def writes(o: ADT0) = JsObject(Nil)
    }


    bench[String](duration)(
      s => Json.fromJson[Seq[Data]](Json.parse(s)).get,
      d => Json.stringify(Json.toJson(d))
    )
  }

  def weepickleDefault(duration: Int) = {

    bench[String](duration)(
      com.rallyhealth.weepickle.v1.WeePickle.read[Seq[Data]](_),
      com.rallyhealth.weepickle.v1.WeePickle.write(_)
    )
  }

  def weepickleBinaryDefault(duration: Int) = {

    bench[Array[Byte]](duration)(
      com.rallyhealth.weepickle.v1.WeePickle.readMsgPack[Seq[Data]](_),
      com.rallyhealth.weepickle.v1.WeePickle.writeMsgPack(_)
    )
  }

//  def genCodec(duration: Int) = {
//    import com.avsystem.commons.serialization._
//
//    implicit def gc1: GenCodec[Data] = GenCodec.materialize
//    implicit def gc2: GenCodec[A] = GenCodec.materialize
//    implicit def gc3: GenCodec[B] = GenCodec.materialize
//    implicit def gc4: GenCodec[C] = GenCodec.materialize
//    implicit def gc5: GenCodec[LL] = GenCodec.materialize
//    implicit def gc6: GenCodec[Node] = GenCodec.materialize
//    implicit def gc7: GenCodec[End.type] = GenCodec.materialize
//    implicit def gc8: GenCodec[ADTc] = GenCodec.materialize
//    implicit def gc9: GenCodec[ADT0] = GenCodec.materialize
//
//    bench[String](duration)(
//      json.JsonStringInput.read[Data](_),
//      json.JsonStringOutput.write[Data](_)
//    )
//  }

  def circeCached(duration: Int) = {
    import io.circe._
    import io.circe.generic.semiauto._
    import io.circe.parser._

    implicit lazy val _r1: Decoder[Data] = deriveDecoder
    implicit lazy val _r2: Decoder[A] = deriveDecoder
    implicit lazy val _r3: Decoder[B] = deriveDecoder
    implicit lazy val _r4: Decoder[C] = deriveDecoder
    implicit lazy val _r5: Decoder[LL] = deriveDecoder
    implicit lazy val _r6: Decoder[Node] = deriveDecoder
    implicit lazy val _r7: Decoder[End.type] = deriveDecoder
    implicit lazy val _r8: Decoder[ADTc] = deriveDecoder
    implicit lazy val _r9: Decoder[ADT0] = deriveDecoder

    implicit lazy val _w1: Encoder[Data] = deriveEncoder
    implicit lazy val _w2: Encoder[A] = deriveEncoder
    implicit lazy val _w3: Encoder[B] = deriveEncoder
    implicit lazy val _w4: Encoder[C] = deriveEncoder
    implicit lazy val _w5: Encoder[LL] = deriveEncoder
    implicit lazy val _w6: Encoder[Node] = deriveEncoder
    implicit lazy val _w7: Encoder[End.type] = deriveEncoder
    implicit lazy val _w8: Encoder[ADTc] = deriveEncoder
    implicit lazy val _w9: Encoder[ADT0] = deriveEncoder

    bench[String](duration)(
      decode[Seq[Data]](_).right.get,
      implicitly[Encoder[Seq[Data]]].apply(_).toString()
    )
  }

  def playJsonCached(duration: Int) = {
    import play.api.libs.json._
    implicit lazy val rw1: Format[Data] = play.api.libs.json.Json.format
    implicit lazy val rw2: Format[A] = play.api.libs.json.Json.format
    implicit lazy val rw3: Format[B] = play.api.libs.json.Json.format
    implicit lazy val rw4: Format[C] = play.api.libs.json.Json.format
    implicit lazy val rw5: Format[LL] = play.api.libs.json.Json.format
    implicit lazy val rw6: Format[Node] = play.api.libs.json.Json.format
    implicit lazy val rw7: Format[End.type] = new Format[End.type] {
      def reads(json: JsValue) = JsSuccess(End)

      def writes(o: Recursive.End.type) = JsObject(Nil)
    }
    implicit lazy val rw8: Format[ADTc] = play.api.libs.json.Json.format
    implicit lazy val rw9: Format[ADT0] = new Format[ADT0] {
      def reads(json: JsValue) = JsSuccess(ADT0())

      def writes(o: ADT0) = JsObject(Nil)
    }



    bench[String](duration)(
      s => Json.fromJson[Seq[Data]](Json.parse(s)).get,
      d => Json.stringify(Json.toJson(d))
    )

  }

  def weepickleDefaultCached(duration: Int) = {
    implicit lazy val rw1: com.rallyhealth.weepickle.v1.WeePickle.ReaderWriter[Data] = com.rallyhealth.weepickle.v1.WeePickle.macroRW
    implicit lazy val rw2: com.rallyhealth.weepickle.v1.WeePickle.ReaderWriter[A] = com.rallyhealth.weepickle.v1.WeePickle.macroRW
    implicit lazy val rw3: com.rallyhealth.weepickle.v1.WeePickle.ReaderWriter[B] = com.rallyhealth.weepickle.v1.WeePickle.macroRW
    implicit lazy val rw4: com.rallyhealth.weepickle.v1.WeePickle.ReaderWriter[C] = com.rallyhealth.weepickle.v1.WeePickle.macroRW
    implicit lazy val rw5: com.rallyhealth.weepickle.v1.WeePickle.ReaderWriter[LL] = com.rallyhealth.weepickle.v1.WeePickle.macroRW
    implicit lazy val rw6: com.rallyhealth.weepickle.v1.WeePickle.ReaderWriter[Node] = com.rallyhealth.weepickle.v1.WeePickle.macroRW
    implicit lazy val rw7: com.rallyhealth.weepickle.v1.WeePickle.ReaderWriter[End.type] = com.rallyhealth.weepickle.v1.WeePickle.macroRW
    implicit lazy val rw8: com.rallyhealth.weepickle.v1.WeePickle.ReaderWriter[ADTc] = com.rallyhealth.weepickle.v1.WeePickle.macroRW
    implicit lazy val rw9: com.rallyhealth.weepickle.v1.WeePickle.ReaderWriter[ADT0] = com.rallyhealth.weepickle.v1.WeePickle.macroRW

    bench[String](duration)(
      com.rallyhealth.weepickle.v1.WeePickle.read[Seq[Data]](_),
      com.rallyhealth.weepickle.v1.WeePickle.write(_)
    )
  }
  def weepickleDefaultCachedByteArray(duration: Int) = {
    implicit lazy val rw1: com.rallyhealth.weepickle.v1.WeePickle.ReaderWriter[Data] = com.rallyhealth.weepickle.v1.WeePickle.macroRW
    implicit lazy val rw2: com.rallyhealth.weepickle.v1.WeePickle.ReaderWriter[A] = com.rallyhealth.weepickle.v1.WeePickle.macroRW
    implicit lazy val rw3: com.rallyhealth.weepickle.v1.WeePickle.ReaderWriter[B] = com.rallyhealth.weepickle.v1.WeePickle.macroRW
    implicit lazy val rw4: com.rallyhealth.weepickle.v1.WeePickle.ReaderWriter[C] = com.rallyhealth.weepickle.v1.WeePickle.macroRW
    implicit lazy val rw5: com.rallyhealth.weepickle.v1.WeePickle.ReaderWriter[LL] = com.rallyhealth.weepickle.v1.WeePickle.macroRW
    implicit lazy val rw6: com.rallyhealth.weepickle.v1.WeePickle.ReaderWriter[Node] = com.rallyhealth.weepickle.v1.WeePickle.macroRW
    implicit lazy val rw7: com.rallyhealth.weepickle.v1.WeePickle.ReaderWriter[End.type] = com.rallyhealth.weepickle.v1.WeePickle.macroRW
    implicit lazy val rw8: com.rallyhealth.weepickle.v1.WeePickle.ReaderWriter[ADTc] = com.rallyhealth.weepickle.v1.WeePickle.macroRW
    implicit lazy val rw9: com.rallyhealth.weepickle.v1.WeePickle.ReaderWriter[ADT0] = com.rallyhealth.weepickle.v1.WeePickle.macroRW

    bench[Array[Byte]](duration)(
      com.rallyhealth.weepickle.v1.WeePickle.read[Seq[Data]](_),
      com.rallyhealth.weepickle.v1.WeePickle.write(_).getBytes
    )
  }
  def weepickleDefaultCachedReadable(duration: Int) = {
    implicit lazy val rw1: com.rallyhealth.weepickle.v1.WeePickle.ReaderWriter[Data] = com.rallyhealth.weepickle.v1.WeePickle.macroRW
    implicit lazy val rw2: com.rallyhealth.weepickle.v1.WeePickle.ReaderWriter[A] = com.rallyhealth.weepickle.v1.WeePickle.macroRW
    implicit lazy val rw3: com.rallyhealth.weepickle.v1.WeePickle.ReaderWriter[B] = com.rallyhealth.weepickle.v1.WeePickle.macroRW
    implicit lazy val rw4: com.rallyhealth.weepickle.v1.WeePickle.ReaderWriter[C] = com.rallyhealth.weepickle.v1.WeePickle.macroRW
    implicit lazy val rw5: com.rallyhealth.weepickle.v1.WeePickle.ReaderWriter[LL] = com.rallyhealth.weepickle.v1.WeePickle.macroRW
    implicit lazy val rw6: com.rallyhealth.weepickle.v1.WeePickle.ReaderWriter[Node] = com.rallyhealth.weepickle.v1.WeePickle.macroRW
    implicit lazy val rw7: com.rallyhealth.weepickle.v1.WeePickle.ReaderWriter[End.type] = com.rallyhealth.weepickle.v1.WeePickle.macroRW
    implicit lazy val rw8: com.rallyhealth.weepickle.v1.WeePickle.ReaderWriter[ADTc] = com.rallyhealth.weepickle.v1.WeePickle.macroRW
    implicit lazy val rw9: com.rallyhealth.weepickle.v1.WeePickle.ReaderWriter[ADT0] = com.rallyhealth.weepickle.v1.WeePickle.macroRW

    bench[String](duration)(
      x => com.rallyhealth.weepickle.v1.WeePickle.read[Seq[Data]](x: com.rallyhealth.weepickle.v1.geny.ReadableAsBytes),
      com.rallyhealth.weepickle.v1.WeePickle.write(_)
    )
  }

  def weepickleDefaultCachedReadablePath(duration: Int) = {
    implicit lazy val rw1: com.rallyhealth.weepickle.v1.WeePickle.ReaderWriter[Data] = com.rallyhealth.weepickle.v1.WeePickle.macroRW
    implicit lazy val rw2: com.rallyhealth.weepickle.v1.WeePickle.ReaderWriter[A] = com.rallyhealth.weepickle.v1.WeePickle.macroRW
    implicit lazy val rw3: com.rallyhealth.weepickle.v1.WeePickle.ReaderWriter[B] = com.rallyhealth.weepickle.v1.WeePickle.macroRW
    implicit lazy val rw4: com.rallyhealth.weepickle.v1.WeePickle.ReaderWriter[C] = com.rallyhealth.weepickle.v1.WeePickle.macroRW
    implicit lazy val rw5: com.rallyhealth.weepickle.v1.WeePickle.ReaderWriter[LL] = com.rallyhealth.weepickle.v1.WeePickle.macroRW
    implicit lazy val rw6: com.rallyhealth.weepickle.v1.WeePickle.ReaderWriter[Node] = com.rallyhealth.weepickle.v1.WeePickle.macroRW
    implicit lazy val rw7: com.rallyhealth.weepickle.v1.WeePickle.ReaderWriter[End.type] = com.rallyhealth.weepickle.v1.WeePickle.macroRW
    implicit lazy val rw8: com.rallyhealth.weepickle.v1.WeePickle.ReaderWriter[ADTc] = com.rallyhealth.weepickle.v1.WeePickle.macroRW
    implicit lazy val rw9: com.rallyhealth.weepickle.v1.WeePickle.ReaderWriter[ADT0] = com.rallyhealth.weepickle.v1.WeePickle.macroRW

    bench[java.nio.file.Path](duration)(
      file => com.rallyhealth.weepickle.v1.WeePickle.read[Seq[Data]](java.nio.file.Files.newInputStream(file): com.rallyhealth.weepickle.v1.geny.ReadableAsBytes),
      data => java.nio.file.Files.write(
        java.nio.file.Files.createTempFile("temp", ".json"),
        com.rallyhealth.weepickle.v1.WeePickle.write(data).getBytes
      ),
      checkEqual = false
    )
  }

  def weepickleDefaultBinaryCached(duration: Int) = {
    implicit lazy val rw1: com.rallyhealth.weepickle.v1.WeePickle.ReaderWriter[Data] = com.rallyhealth.weepickle.v1.WeePickle.macroRW
    implicit lazy val rw2: com.rallyhealth.weepickle.v1.WeePickle.ReaderWriter[A] = com.rallyhealth.weepickle.v1.WeePickle.macroRW
    implicit lazy val rw3: com.rallyhealth.weepickle.v1.WeePickle.ReaderWriter[B] = com.rallyhealth.weepickle.v1.WeePickle.macroRW
    implicit lazy val rw4: com.rallyhealth.weepickle.v1.WeePickle.ReaderWriter[C] = com.rallyhealth.weepickle.v1.WeePickle.macroRW
    implicit lazy val rw5: com.rallyhealth.weepickle.v1.WeePickle.ReaderWriter[LL] = com.rallyhealth.weepickle.v1.WeePickle.macroRW
    implicit lazy val rw6: com.rallyhealth.weepickle.v1.WeePickle.ReaderWriter[Node] = com.rallyhealth.weepickle.v1.WeePickle.macroRW
    implicit lazy val rw7: com.rallyhealth.weepickle.v1.WeePickle.ReaderWriter[End.type] = com.rallyhealth.weepickle.v1.WeePickle.macroRW
    implicit lazy val rw8: com.rallyhealth.weepickle.v1.WeePickle.ReaderWriter[ADTc] = com.rallyhealth.weepickle.v1.WeePickle.macroRW
    implicit lazy val rw9: com.rallyhealth.weepickle.v1.WeePickle.ReaderWriter[ADT0] = com.rallyhealth.weepickle.v1.WeePickle.macroRW

    bench[Array[Byte]](duration)(
      com.rallyhealth.weepickle.v1.WeePickle.readMsgPack[Seq[Data]](_),
      com.rallyhealth.weepickle.v1.WeePickle.writeMsgPack(_)
    )
  }

//  def genCodecCached(duration: Int) = {
//    import com.avsystem.commons.serialization._
//
//    implicit lazy val gc1: GenCodec[Data] = GenCodec.materialize
//    implicit lazy val gc2: GenCodec[A] = GenCodec.materialize
//    implicit lazy val gc3: GenCodec[B] = GenCodec.materialize
//    implicit lazy val gc4: GenCodec[C] = GenCodec.materialize
//    implicit lazy val gc5: GenCodec[LL] = GenCodec.materialize
//    implicit lazy val gc6: GenCodec[Node] = GenCodec.materialize
//    implicit lazy val gc7: GenCodec[End.type] = GenCodec.materialize
//    implicit lazy val gc8: GenCodec[ADTc] = GenCodec.materialize
//    implicit lazy val gc9: GenCodec[ADT0] = GenCodec.materialize
//
//    bench[String](duration)(
//      json.JsonStringInput.read[Data](_),
//      json.JsonStringOutput.write[Data](_)
//    )
//  }

  def bench[T](duration: Int)
              (f1: T => Seq[Data], f2: Seq[Data] => T, checkEqual: Boolean = true)
              (implicit name: sourcecode.Name) = {
    val stringified = f2(benchmarkSampleData)
    val r1 = f1(stringified)
    val equal = benchmarkSampleData == r1

    if (checkEqual) {
      assert(equal)
      val rewritten = f2(f1(stringified))
      (stringified, rewritten) match {
        case (lhs: Array[_], rhs: Array[_]) => assert(lhs.toSeq == rhs.toSeq)
        case _ => assert(stringified == rewritten)
      }
    }
//    bench0[T, Seq[Data]](duration, stringified)(f1, f2)
  }

  def bench0[T, V](duration: Int, stringified: T)
                  (f1: T => V, f2: V => T)
                  (implicit name: sourcecode.Name)= {
    {
      var n = 0
      val start = System.currentTimeMillis()
      while(System.currentTimeMillis() < start + duration){
        f1(stringified)
        n += 1
      }
      println(name.value + " Read " + n)
    }

    val parsed = f1(stringified)

    {
      var n = 0
      val start = System.currentTimeMillis()
      while(System.currentTimeMillis() < start + duration){
        f2(parsed)
        n += 1
      }
      println(name.value + " Write " + n)
    }
  }
}
