package com.rallyhealth.weepickle.v1.core

import java.util.UUID

object Util {

  def parseIntegralNum(s: CharSequence, decIndex: Int, expIndex: Int): Long = {
    val expMul =
      if (expIndex == -1) 1
      else {
        var mult = 1
        val e = parseLong(s, expIndex + 1, s.length())
        var i = 0
        while (i < e) {
          if (mult >= Long.MaxValue / 10) throw new Abort("expected integer")
          mult = mult * 10
          i += 1
        }
        mult
      }

    val intPortion = {
      val end =
        if (decIndex != -1) decIndex
        else if (expIndex != -1) expIndex
        else s.length

      parseLong(s, 0, end) * expMul
    }

    val decPortion =
      if (decIndex == -1) 0
      else {
        val end = if (expIndex != -1) expIndex else s.length
        var value = parseLong(s, decIndex + 1, end) * expMul
        var i = end - (decIndex + 1)
        while (i > 0) {
          value = value / 10
          i -= 1
        }
        if (s.charAt(0) == '-') -value else value
      }

    intPortion + decPortion
  }
  def parseLong(cs: CharSequence, start: Int, len: Int): Long = {

    // we store the inverse of the positive sum, to ensure we don't
    // incorrectly overflow on Long.MinValue. for positive numbers
    // this inverse sum will be inverted before being returned.
    var inverseSum: Long = 0L
    var inverseSign: Long = -1L
    var i: Int = start

    if (cs.charAt(start) == '-') {
      inverseSign = 1L
      i = 1
    }

    val size = len - i
    if (i >= len) throw new NumberFormatException(cs.toString)
    if (size > 19) throw new NumberFormatException(cs.toString)

    while (i < len) {
      val digit = cs.charAt(i).toInt - 48
      if (digit < 0 || 9 < digit) new NumberFormatException(cs.toString)
      inverseSum = inverseSum * 10L - digit
      i += 1
    }

    // detect and throw on overflow
    if (size == 19 && (inverseSum >= 0 || (inverseSum == Long.MinValue && inverseSign < 0))) {
      throw new NumberFormatException(cs.toString)
    }

    inverseSum * inverseSign
  }

  def parseUUID(name: CharSequence): UUID = {
    val ns = nibbles
    var msb, lsb = 0L
    if (name.length == 36 && {
      val ch1: Long = name.charAt(8)
      val ch2: Long = name.charAt(13)
      val ch3: Long = name.charAt(18)
      val ch4: Long = name.charAt(23)
      (ch1 << 48 | ch2 << 32 | ch3 << 16 | ch4) == 0x2D002D002D002DL
    } && {
      val msb1 = parse4Nibbles(name, ns, 0)
      val msb2 = parse4Nibbles(name, ns, 4)
      val msb3 = parse4Nibbles(name, ns, 9)
      val msb4 = parse4Nibbles(name, ns, 14)
      msb = msb1 << 48 | msb2 << 32 | msb3 << 16 | msb4
      (msb1 | msb2 | msb3 | msb4) >= 0
    } && {
      val lsb1 = parse4Nibbles(name, ns, 19)
      val lsb2 = parse4Nibbles(name, ns, 24)
      val lsb3 = parse4Nibbles(name, ns, 28)
      val lsb4 = parse4Nibbles(name, ns, 32)
      lsb = lsb1 << 48 | lsb2 << 32 | lsb3 << 16 | lsb4
      (lsb1 | lsb2 | lsb3 | lsb4) >= 0
    }) new UUID(msb, lsb)
    else UUID.fromString(name.toString)
  }

  private[this] def parse4Nibbles(name: CharSequence, ns: Array[Byte], pos: Int): Long = {
    val ch1 = name.charAt(pos)
    val ch2 = name.charAt(pos + 1)
    val ch3 = name.charAt(pos + 2)
    val ch4 = name.charAt(pos + 3)
    if ((ch1 | ch2 | ch3 | ch4) > 0xFF) -1
    else ns(ch1) << 12 | ns(ch2) << 8 | ns(ch3) << 4 | ns(ch4)
  }

  private[this] val nibbles: Array[Byte] = {
    val ns = new Array[Byte](256)
    java.util.Arrays.fill(ns, -1: Byte)
    ns('0') = 0
    ns('1') = 1
    ns('2') = 2
    ns('3') = 3
    ns('4') = 4
    ns('5') = 5
    ns('6') = 6
    ns('7') = 7
    ns('8') = 8
    ns('9') = 9
    ns('A') = 10
    ns('B') = 11
    ns('C') = 12
    ns('D') = 13
    ns('E') = 14
    ns('F') = 15
    ns('a') = 10
    ns('b') = 11
    ns('c') = 12
    ns('d') = 13
    ns('e') = 14
    ns('f') = 15
    ns
  }
}
