package com.rallyhealth.weejson.v1

import java.time.Instant

import com.rallyhealth.weepickle.v1.core.{ArrVisitor, JsVisitor, ObjVisitor, Visitor}

import scala.collection.mutable

/**
  * A version of [[com.rallyhealth.weejson.v1.Value]] used to buffer data in raw form.
  *
  * This is used by the case class macros to buffer data for polymorphic types
  * when the discriminator is not the first element, e.g. `{"foo": 1, "\$type": "discriminator"}`.
  * It is important that all types be immutable.
  */
sealed trait BufferedValue

object BufferedValue extends Transformer[BufferedValue] {

  case class Str(value0: String) extends BufferedValue
  case class Obj(value0: (String, BufferedValue)*) extends BufferedValue
  case class Arr(value: BufferedValue*) extends BufferedValue
  case class Num(s: String, decIndex: Int, expIndex: Int) extends BufferedValue
  case class NumLong(l: Long) extends BufferedValue
  case class NumDouble(d: Double) extends BufferedValue
  case class Binary(b: Array[Byte]) extends BufferedValue
  case class Ext(tag: Byte, b: Array[Byte]) extends BufferedValue
  case class Timestamp(i: Instant) extends BufferedValue
  case object False extends BufferedValue {
    def value = false
  }
  case object True extends BufferedValue {
    def value = true
  }
  case object Null extends BufferedValue {
    def value = null
  }

  def transform[T](i: BufferedValue, to: Visitor[_, T]): T = {
    i match {
      case BufferedValue.Null         => to.visitNull()
      case BufferedValue.True         => to.visitTrue()
      case BufferedValue.False        => to.visitFalse()
      case BufferedValue.Str(s)       => to.visitString(s)
      case BufferedValue.Num(s, d, e) => to.visitFloat64StringParts(s, d, e)
      case BufferedValue.NumLong(l)   => to.visitInt64(l)
      case BufferedValue.NumDouble(d) => to.visitFloat64(d)
      case BufferedValue.Binary(b)    => to.visitBinary(b, 0, b.length)
      case BufferedValue.Ext(tag, b)  => to.visitExt(tag, b, 0, b.length)
      case BufferedValue.Timestamp(i) => to.visitTimestamp(i)
      case BufferedValue.Arr(items @ _*) =>
        val ctx = to.visitArray(-1).narrow
        for (item <- items) ctx.visitValue(transform(item, ctx.subVisitor))
        ctx.visitEnd()
      case BufferedValue.Obj(items @ _*) =>
        val ctx = to.visitObject(-1).narrow
        for ((k, item) <- items) {
          val keyVisitor = ctx.visitKey()

          ctx.visitKeyValue(keyVisitor.visitString(k))
          ctx.visitValue(transform(item, ctx.subVisitor))
        }
        ctx.visitEnd()
    }
  }

  object Builder extends JsVisitor[BufferedValue, BufferedValue] {
    def visitArray(length: Int): ArrVisitor[BufferedValue, BufferedValue] =
      new ArrVisitor[BufferedValue, BufferedValue.Arr] {
        val out = mutable.Buffer.empty[BufferedValue]
        def subVisitor = Builder
        def visitValue(v: BufferedValue): Unit = {
          out.append(v)
        }
        def visitEnd(): BufferedValue.Arr = BufferedValue.Arr(out.toSeq: _*)
      }

    def visitObject(length: Int): ObjVisitor[BufferedValue, BufferedValue] =
      new ObjVisitor[BufferedValue, BufferedValue.Obj] {
        val out = mutable.Buffer.empty[(String, BufferedValue)]
        var currentKey: String = _
        def subVisitor = Builder
        def visitKey(): Visitor[_, _] = BufferedValue.Builder
        def visitKeyValue(s: Any): Unit = currentKey = s.asInstanceOf[BufferedValue.Str].value0.toString
        def visitValue(v: BufferedValue): Unit = {
          out.append((currentKey, v))
        }
        def visitEnd(): BufferedValue.Obj = BufferedValue.Obj(out.toSeq: _*)
      }

    def visitNull(): BufferedValue = BufferedValue.Null

    def visitFalse(): BufferedValue = BufferedValue.False

    def visitTrue(): BufferedValue = BufferedValue.True

    override def visitFloat64StringParts(cs: CharSequence, decIndex: Int, expIndex: Int): BufferedValue =
      BufferedValue.Num(cs.toString, decIndex, expIndex)
    override def visitFloat64(d: Double): BufferedValue = BufferedValue.NumDouble(d)

    override def visitInt64(l: Long): BufferedValue = NumLong(l)

    def visitString(cs: CharSequence): BufferedValue = BufferedValue.Str(cs.toString)

    override def visitTimestamp(instant: Instant): BufferedValue = Timestamp(instant)

    override def visitBinary(bytes: Array[Byte], offset: Int, len: Int): BufferedValue = {
      BufferedValue.Binary(bytes.slice(offset, len))
    }

    override def visitExt(
      tag: Byte,
      bytes: Array[Byte],
      offset: Int,
      len: Int
    ): BufferedValue = BufferedValue.Ext(tag, bytes.slice(offset, len))
  }
}
