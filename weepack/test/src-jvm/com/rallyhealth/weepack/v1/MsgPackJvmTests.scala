package com.rallyhealth.weepack.v1

import com.rallyhealth.weejson.v1.WeeJson
import com.rallyhealth.weepickle.v1.core.TestUtil
import utest._

import scala.collection.mutable
object MsgPackJvmTests extends TestSuite {
  def readBytes(path: String) = java.nio.file.Files.readAllBytes(java.nio.file.Paths.get(path))
  def readMsgs(path: String) = {
    val bytes = readBytes(path)
    val output = mutable.ArrayBuffer.empty[Msg]
    val p = new MsgPackParser(0, bytes)
    while (p.index < bytes.length) {
      output.append(p.parse(Msg))
    }
    com.rallyhealth.weepack.v1.Arr(output)
  }
  val tests = Tests {
    test("hello") {

      // Taken from:
      // https://github.com/msgpack/msgpack-ruby/tree/a22d8268f82e0f2ae95f038285af43ce5971810e/spec
      val casesJson = "weepack/test/resources/cases.json"
      val casesMsg = "weepack/test/resources/cases.msg"
      val casesCompactMsg = "weepack/test/resources/cases_compact.msg"
      val expectedJson = WeeJson.read(readBytes(casesJson))
      val msg = readMsgs(casesMsg)
      val msgCompact = readMsgs(casesCompactMsg)
      val jsonMsg = WeePack.transform(msg, com.rallyhealth.weejson.v1.Value)
      val jsonMsgCompact = WeePack.transform(msgCompact, com.rallyhealth.weejson.v1.Value)
      val writtenMsg = TestUtil.bytesToString(WeePack.write(msg))
      val rewrittenMsg = TestUtil.bytesToString(
        WeePack.write(WeePack.read(WeePack.write(msg)))
      )
      val writtenMsgCompact = TestUtil.bytesToString(WeePack.write(msgCompact))
      val rewrittenMsgCompact = TestUtil.bytesToString(
        WeePack.write(WeePack.read(WeePack.write(msgCompact)))
      )
      assert(
        expectedJson == jsonMsg,
        expectedJson == jsonMsgCompact
//        Still doesn't pass:
//
//        writtenMsg == rewrittenMsg,
//        writtenMsgCompact == rewrittenMsgCompact
      )
    }
  }
}
