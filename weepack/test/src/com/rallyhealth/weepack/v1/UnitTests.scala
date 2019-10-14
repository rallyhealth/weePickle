package com.rallyhealth.weepack.v0
import com.rallyhealth.weepickle.v0.core.Abort
import utest._

object UnitTests extends TestSuite{
  val tests = Tests {
    test("compositeKeys"){
      val msg = Obj(Arr(Int32(1), Int32(2)) -> Int32(1))
      val written = WeePack.write(msg)
      val writtenStr = com.rallyhealth.weepickle.v0.core.Util.bytesToString(written)
      writtenStr ==> "81-92-01-02-01"

      WeePack.read(written) ==> msg


      intercept[Abort]{
        WeePack.transform(written, com.rallyhealth.weejson.v0.Value)
      }
      intercept[Abort] {
        WeePack.transform(msg, com.rallyhealth.weejson.v0.Value)
      }
    }
  }
}
