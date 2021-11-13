package com.rallyhealth.weepack.v1

import com.rallyhealth.weejson.v1.Value
import com.rallyhealth.weepickle.v1.core.{Abort, TestUtil}
import utest._

import java.io.{ByteArrayInputStream, ByteArrayOutputStream}

object UnitTests extends TestSuite{
  val tests = Tests {
    test("trivial") {
      val msg = Arr(Str("a"))
      val written = msg.transform(ToMsgPack.bytes)
      FromMsgPack(written).transform(Msg) ==> msg
    }
    test("array"){
      val msg = Arr(
        Str("a"), Str("bb"), Str("ccc"), Str("dddd"), Str("eeeee"), Str("ffffff"),
        Str("g"), Str("hh"), Str("iii"), Str("jjjj"), Str("kkkkk"), Str("llllll"),
        Str("m"), Str("nn"), Str("ooo"), Str("pppp"), Str("qqqqq"), Str("rrrrrr")
      )
      val written = msg.transform(ToMsgPack.bytes)
      val reader = new InputStreamMsgPackParser(new ByteArrayInputStream(written), 2, 2)
      val read = reader.parse(Msg)

      read ==> msg
    }
    test("map"){
      val msg = Obj(
        Str("a") -> Int32(123), Str("c") -> Int32(456), Str("d") -> Int32(789),
        Str("e") -> Int32(123), Str("f") -> Int32(456), Str("g") -> Int32(789),
        Str("h") -> Int32(123), Str("i") -> Int32(456), Str("j") -> Int32(789),
        Str("k") -> Int32(123), Str("l") -> Int32(456), Str("m") -> Int32(789),
        Str("n") -> Int32(123), Str("o") -> Int32(456), Str("p") -> Int32(789),
        Str("q") -> Int32(123), Str("r") -> Int32(456), Str("s") -> Int32(789)
      )
      val written = msg.transform(ToMsgPack.bytes)
      val reader = new InputStreamMsgPackParser(new ByteArrayInputStream(written), 2, 2)
      val read = reader.parse(Msg)

      read ==> msg
    }
    test("compositeKeys") {
      val msg = Obj(Arr(Int32(1), Int32(2)) -> Int32(1))
      val written = WeePack.write(msg)
      val writtenStr = TestUtil.bytesToString(written)
      writtenStr ==> "81-92-01-02-01"

      WeePack.read(written) ==> msg

      intercept[Abort] {
        WeePack.transform(written, Value)
      }
      intercept[Abort] {
        WeePack.transform(msg, Value)
      }
    }
    def roundtrip(msg1: Msg) = {
      val bytes1 = msg1.transform(ToMsgPack.bytes)
      val msg2 = FromMsgPack(bytes1).transform(Msg)
      // msg2 ==> msg1 fails because Int32(0) != Int64(0L), etc.
      val bytes2 = msg2.transform(ToMsgPack.bytes)
      assert(bytes1.sameElements(bytes2))
    }
    test("writeBytesTo")(roundtrip(Obj(Arr(Int32(1), Int32(2)) -> Int32(1))))
    test("extInList")(roundtrip(Arr(Ext(33, new Array[Byte](4)), False)))
    test("extInMap")(roundtrip(Obj(Str("foo") -> Ext(33, new Array[Byte](12)), Str("bar") -> Null)))
  }
}
