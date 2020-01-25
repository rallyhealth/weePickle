package com.rallyhealth.weepickle.v1
import utest._
import acyclic.file
import com.rallyhealth.weejson.v1.jackson.{FromJson, ToJson}
import com.rallyhealth.weepack.v1.{FromMsgPack, ToMsgPack}

/**
  * Created by haoyi on 4/22/14.
  */
object TestUtil extends TestUtil[com.rallyhealth.weepickle.v1.WeePickle.type](com.rallyhealth.weepickle.v1.WeePickle)
class TestUtil[Api <: com.rallyhealth.weepickle.v1.Api](val api: Api) {

  def rw[T: api.To: api.From](t: T, s: String*) = {
    rwk[T, T](t, s: _*)(x => x)
  }
  def rwNoBinaryJson[T: api.To: api.From](t: T, s: String*) = {
    rwk[T, T](t, s: _*)(x => x, checkBinaryJson = false)
  }
  def rwEscape[T: api.To: api.From](t: T, s: String*) = {
    rwk[T, T](t, s: _*)(x => x, escapeUnicode = true)
  }
  def rwk[T: api.To: api.From, V](
    t: T,
    sIn: String*
  )(normalize: T => V, escapeUnicode: Boolean = false, checkBinaryJson: Boolean = true) = {
    val writtenT = api.from[T].transform(t, ToJson.string)

    // Test JSON round tripping
    val strings = sIn.map(_.trim)

    for (s <- strings) {
      val readS = FromJson(s).transform(api.to[T])
      val normalizedReadString = normalize(readS)
      val normalizedValue = normalize(t)
      assert(normalizedReadString == normalizedValue)
    }

    val normalizedReadWrittenT = normalize(FromJson(writtenT).transform(api.to[T]))
    val normalizedT = normalize(t)
    assert(normalizedReadWrittenT == normalizedT)

    // Test binary round tripping
    val writtenBinary = api.from[T].transform(t, ToMsgPack.bytes)
    // println(com.rallyhealth.weepickle.v1.core.Util.bytesToString(writtenBinary))
    val roundTrippedBinary = FromMsgPack(writtenBinary).transform(api.to[T])
    (roundTrippedBinary, t) match {
      case (lhs: Array[_], rhs: Array[_]) => assert(lhs.toSeq == rhs.toSeq)
      case _                              => assert(roundTrippedBinary == t)
    }

    // Test binary-JSON equivalence
    if (checkBinaryJson) {
      val rewrittenBinary = api.from[T].transform(roundTrippedBinary, ToMsgPack.bytes)

      val writtenBinaryStr = com.rallyhealth.weepickle.v1.core.TestUtil.bytesToString(writtenBinary)
      val rewrittenBinaryStr = com.rallyhealth.weepickle.v1.core.TestUtil.bytesToString(rewrittenBinary)
      assert(writtenBinaryStr == rewrittenBinaryStr)
    }
  }

  def assertNumberFormatException[T: api.To](s: String) = {
    val e = intercept[Exception] {
      FromJson(s.trim).transform(api.to[T])
    }
    e.getCause.getClass ==> classOf[NumberFormatException]
  }

  def parses[T: api.To](s: String*) = {
    val strings = s.map(_.trim)

    for (s <- strings) {
      FromJson(s).transform(api.to[T])
    }
  }
}
