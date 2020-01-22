package com.rallyhealth.weepickle.v1.core

import java.time.Instant
import java.time.format.DateTimeFormatter

/**
  * A [[Visitor]] specialized to work with JSON types. Forwards the
  * not-JSON-related methods to their JSON equivalents.
  */
trait JsVisitor[-T, +J] extends Visitor[T, J]{
  def visitFloat64(d: Double): J = {
    val i = d.toLong
    if(i == d) visitFloat64StringParts(i.toString, -1, -1)
    else visitFloat64String(d.toString)

  }
  def visitFloat32(d: Float): J = {
    visitFloat64(d)
  }
  def visitInt32(i: Int): J = {
    visitInt64(i)
  }
  def visitInt64(l: Long): J = {
    visitFloat64StringParts(l.toString, -1, -1)
  }
  def visitUInt64(ul: Long): J = {
    visitFloat64StringParts(java.lang.Long.toUnsignedString(ul), -1, -1)
  }

  def visitFloat64String(s: String): J = {
    visitFloat64StringParts(
      cs = s,
      decIndex = s.indexOf('.'),
      expIndex = s.indexOf('E') match {
        case -1 => s.indexOf('e')
        case n => n
      }
    )
  }

  def visitBinary(bytes: Array[Byte], offset: Int, len: Int): J = {
    val arr = visitArray(len)
    var i = 0
    while (i < len){
      arr.visitValue(arr.subVisitor.visitInt32(bytes(offset + i)).asInstanceOf[T])
      i += 1
    }
    arr.visitEnd()
  }

  def visitExt(tag: Byte, bytes: Array[Byte], offset: Int, len: Int): J = {
    val arr = visitArray(-1)
    arr.visitValue(visitFloat64(tag).asInstanceOf[T])
    arr.visitValue(visitBinary(bytes, offset, len).asInstanceOf[T])
    arr.visitEnd()
  }

  def visitChar(c: Char): J = visitString(c.toString)

  def visitTimestamp(instant: Instant): J = {
    visitString(DateTimeFormatter.ISO_INSTANT.format(instant))
  }
}
