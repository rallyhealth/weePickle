package com.rallyhealth.weejson.v1.dijon

import java.time.Instant

import com.github.pathikrit.dijon._
import com.rallyhealth.weejson.v1.AstTransformer
import com.rallyhealth.weepickle.v1.core.{ArrVisitor, ObjVisitor, StringVisitor, Visitor}

import scala.collection.JavaConverters._
import scala.collection.mutable

object Dijon extends AstTransformer[SomeJson] {

  // TODO fast path empty?
  override def visitArray(length: Int): ArrVisitor[SomeJson, SomeJson] = new AstArrVisitor[mutable.ArrayBuffer](a => a)

  // TODO fast path empty?
  // TODO try to compact it?
  override def visitObject(length: Int): ObjVisitor[SomeJson, SomeJson] = new ObjVisitor[SomeJson, SomeJson] {
    // Not using AstObjVisitor since there's no Factory for java.util.LinkedHashMap
    private val obj = new java.util.LinkedHashMap[String, SomeJson](if (length >= 0) length else 16)
    private var key: String = _

    override def visitKey(): Visitor[_, _] = StringVisitor

    override def visitKeyValue(v: Any): Unit = key = v.toString

    override def subVisitor: Visitor[_, _] = Dijon

    override def visitValue(v: SomeJson): Unit = obj.put(key, v)

    override def visitEnd(): SomeJson = obj.asScala
  }

  override def visitNull(): SomeJson = None

  override def visitFalse(): SomeJson = false

  override def visitTrue(): SomeJson = true

  override def visitFloat64(d: Double): SomeJson = d

  override def visitFloat32(d: Float): SomeJson = d.toDouble

  override def visitInt32(i: Int): SomeJson = i

  override def visitInt64(l: Long): SomeJson = {
    val i = l.toInt
    if (i != l) throw new ArithmeticException("integer overflow")
    i
  }

  override def visitFloat64String(s: String): SomeJson = super.visitFloat64String(s)

  override def visitBinary(bytes: Array[Byte], offset: Int, len: Int): SomeJson = super.visitBinary(bytes, offset, len)

  override def visitExt(tag: Byte, bytes: Array[Byte], offset: Int, len: Int): SomeJson = super.visitExt(tag, bytes, offset, len)

  override def visitChar(c: Char): SomeJson = super.visitChar(c)

  override def visitTimestamp(instant: Instant): SomeJson = super.visitTimestamp(instant)

  override def visitFloat64StringParts(cs: CharSequence, decIndex: Int, expIndex: Int): SomeJson = {
    val d = cs.toString.toDouble
    // In Dijon, there's only Int or Double.
    val i = d.toInt
    if (i == d) i else d
  }

  override def visitString(cs: CharSequence): SomeJson = cs.toString

  override def transform[T](i: SomeJson, out: Visitor[_, T]): T = (i.underlying : @unchecked) match {
    case None => out.visitNull()
    case str: String => out.visitString(str)
    case true => out.visitTrue()
    case false => out.visitFalse()
    case i: Int => out.visitInt32(i)
    case d: Double => out.visitFloat64(d)
    case arr: JsonArray =>
      val arrVis = out.visitArray(arr.length).narrow
      arr.foreach(v => arrVis.visitValue(transform(v, arrVis.subVisitor)))
      arrVis.visitEnd()
    case obj: JsonObject =>
      val objVis = out.visitObject(obj.size).narrow
      obj.foreach { case (k, v) =>
        objVis.visitKeyValue(objVis.visitKey().visitString(k))
        objVis.visitValue(transform(v, objVis.subVisitor))
      }
      objVis.visitEnd()
  }
}
