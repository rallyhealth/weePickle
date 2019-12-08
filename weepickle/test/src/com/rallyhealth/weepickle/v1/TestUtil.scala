package com.rallyhealth.weepickle.v0
import utest._
import acyclic.file
/**
* Created by haoyi on 4/22/14.
*/
object TestUtil extends TestUtil[com.rallyhealth.weepickle.v0.WeePickle.type](com.rallyhealth.weepickle.v0.WeePickle)
class TestUtil[Api <: com.rallyhealth.weepickle.v0.Api](val api: Api){

  def rw[T: api.Reader: api.Writer](t: T, s: String*) = {
    rwk[T, T](t, s:_*)(x => x)
  }
  def rwNoBinaryJson[T: api.Reader: api.Writer](t: T, s: String*) = {
    rwk[T, T](t, s:_*)(x => x, checkBinaryJson = false)
  }
  def rwEscape[T: api.Reader: api.Writer](t: T, s: String*) = {
    rwk[T, T](t, s:_*)(x => x, escapeUnicode = true)
  }
  def rwk[T: api.Reader: api.Writer, V](t: T, sIn: String*)
                                       (normalize: T => V,
                                        escapeUnicode: Boolean = false,
                                        checkBinaryJson: Boolean = true) = {
    val writtenT = api.write(t)

    // Test JSON round tripping
    val strings = sIn.map(_.trim)

    for (s <- strings) {
      val readS = api.read[T](s)
      val normalizedReadString = normalize(readS)
      val normalizedValue = normalize(t)
      assert(normalizedReadString == normalizedValue)
    }

    val normalizedReadWrittenT = normalize(api.read[T](writtenT))
    val normalizedT = normalize(t)
    assert(normalizedReadWrittenT == normalizedT)

    // Test binary round tripping
    val writtenBinary = api.writeMsgPack(t)
    // println(com.rallyhealth.weepickle.v0.core.Util.bytesToString(writtenBinary))
    val roundTrippedBinary = api.readMsgPack[T](writtenBinary)
    (roundTrippedBinary, t) match{
      case (lhs: Array[_], rhs: Array[_]) => assert(lhs.toSeq == rhs.toSeq)
      case _ => assert(roundTrippedBinary == t)
    }


    // Test binary-JSON equivalence
    if (checkBinaryJson){
      val rewrittenBinary = api.writeMsgPack(roundTrippedBinary)

      val writtenBinaryStr = com.rallyhealth.weepickle.v0.core.Util.bytesToString(writtenBinary)
      val rewrittenBinaryStr = com.rallyhealth.weepickle.v0.core.Util.bytesToString(rewrittenBinary)
      assert(writtenBinaryStr == rewrittenBinaryStr)
    }
  }

  def abuseCase[T: api.Reader](s: String*) = {
    val strings = s.map(_.trim)

    for (s <- strings) {
      intercept[NumberFormatException] {
        api.read[T](s)
      }
    }
  }

  def parses[T: api.Reader](s: String*) = {
    val strings = s.map(_.trim)

    for (s <- strings) {
        api.read[T](s)
    }
  }
}
