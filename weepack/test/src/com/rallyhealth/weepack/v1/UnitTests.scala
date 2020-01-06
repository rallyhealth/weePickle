package com.rallyhealth.weepack.v0
import java.io.ByteArrayOutputStream

import com.rallyhealth.weejson.v0.Value
import com.rallyhealth.weepickle.v0.core.{Abort, Util}
import com.rallyhealth.weepickle.v0.geny.ReadableAsBytes
import utest._

object UnitTests extends TestSuite{
  val tests = Tests {

    test("trivial"){
      val msg = Arr(Str("a"))
      val written = WeePack.write(msg)
      WeePack.read(written: ReadableAsBytes) ==> msg
    }
    test("compositeKeys"){
      val msg = Obj(Arr(Int32(1), Int32(2)) -> Int32(1))
      val written = WeePack.write(msg)
      val writtenStr = Util.bytesToString(written)
      writtenStr ==> "81-92-01-02-01"

      WeePack.read(written) ==> msg


      intercept[Abort]{
        WeePack.transform(written, Value)
      }
      intercept[Abort] {
        WeePack.transform(msg, Value)
      }
    }
    test("writeBytesTo"){
      val msg = Obj(Arr(Int32(1), Int32(2)) -> Int32(1))
      val out = new ByteArrayOutputStream()
      msg.writeBytesTo(out)
      val bytes = out.toByteArray
      val parsed = WeePack.read(bytes)
      assert(msg == parsed)
    }
  }
}
