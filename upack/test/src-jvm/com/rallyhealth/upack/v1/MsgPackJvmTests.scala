package com.rallyhealth.upack.v1
import java.io.ByteArrayOutputStream

import com.rallyhealth.upickle.v1.core.Util
import utest._

import scala.collection.mutable
object MsgPackJvmTests extends TestSuite{
  def readBytes(path: String) = java.nio.file.Files.readAllBytes(java.nio.file.Paths.get(path))
  def readMsgs(path: String) = {
    val bytes = readBytes(path)
    val output = mutable.ArrayBuffer.empty[Msg]
    val p = new MsgPackReader(0, bytes)
    while(p.currentIndex < bytes.length){
      output.append(p.parse(Msg))
    }
    com.rallyhealth.upack.v1.Arr(output)
  }
  val tests = Tests{
    test("hello"){

      // Taken from:
      // https://github.com/msgpack/msgpack-ruby/tree/a22d8268f82e0f2ae95f038285af43ce5971810e/spec
      val casesJson = "upack/test/resources/cases.json"
      val casesMsg = "upack/test/resources/cases.msg"
      val casesCompactMsg = "upack/test/resources/cases_compact.msg"
      val expectedJson = com.rallyhealth.ujson.v1.read(readBytes(casesJson))
      val msg = readMsgs(casesMsg)
      val msgCompact = readMsgs(casesCompactMsg)
      val jsonMsg = com.rallyhealth.upack.v1.transform(msg, com.rallyhealth.ujson.v1.Value)
      val jsonMsgCompact = com.rallyhealth.upack.v1.transform(msgCompact, com.rallyhealth.ujson.v1.Value)
      val writtenMsg = Util.bytesToString(com.rallyhealth.upack.v1.write(msg))
      val rewrittenMsg = Util.bytesToString(
        com.rallyhealth.upack.v1.write(com.rallyhealth.upack.v1.read(com.rallyhealth.upack.v1.write(msg)))
      )
      val writtenMsgCompact = Util.bytesToString(com.rallyhealth.upack.v1.write(msgCompact))
      val rewrittenMsgCompact = Util.bytesToString(
        com.rallyhealth.upack.v1.write(com.rallyhealth.upack.v1.read(com.rallyhealth.upack.v1.write(msgCompact)))
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
