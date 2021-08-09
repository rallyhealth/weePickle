package com.rallyhealth.weejson.v1

import java.time.Instant
import com.rallyhealth.weepickle.v1.core.{ArrVisitor, FromInput, JsVisitor, ObjVisitor, Visitor}

import scala.collection.mutable

/**
  * A version of [[com.rallyhealth.weejson.v1.Value]] used to buffer data in raw form.
  *
  * This is used by the case class macros to buffer data for polymorphic types
  * when the discriminator is not the first element, e.g. `{"foo": 1, "\$type": "discriminator"}`.
  * It is important that all types be immutable.
  */
sealed trait BufferedValue extends FromInput {
  import BufferedValue._

  /**
   * Returns the `String` value of this [[BufferedValue]], fails if it is not
   * a [[Str]]
   */
  def str: String = strOpt.getOrElse(throw BufferedValue.InvalidData(this, "Expected Str"))

  /**
   * Returns an Optional `String` value of this [[BufferedValue]] in case this [[BufferedValue]] is a 'String'.
   */
  def strOpt: Option[String] = this match {
    case Str(value) => Some(value)
    case _          => None
  }

  /**
   * Returns the key/value map of this [[BufferedValue]], fails if it is not
   * a [[Obj]]
   */
  def obj: Seq[(String, BufferedValue)] = objOpt.getOrElse(throw BufferedValue.InvalidData(this, "Expected Obj"))

  /**
   * Returns an Optional key/value map of this [[BufferedValue]] in case this [[BufferedValue]] is a 'Obj'.
   */
  def objOpt: Option[Seq[(String, BufferedValue)]] = this match {
    case Obj(items @ _*) => Some(items)
    case _               => None
  }

  /**
   * Returns the elements of this [[BufferedValue]], fails if it is not
   * a [[Arr]]
   */
  def arr: Seq[BufferedValue] = arrOpt.getOrElse(throw BufferedValue.InvalidData(this, "Expected Arr"))

  /**
   * Returns The optional elements of this [[BufferedValue]] in case this [[BufferedValue]] is a 'Arr'.
   */
  def arrOpt: Option[Seq[BufferedValue]] = this match {
    case Arr(items @ _*) => Some(items)
    case _               => None
  }

  /**
   * Returns the `BigDecimal` value of this [[BufferedValue]], fails if it is not
   * a [[Num]]
   */
  def num: BigDecimal = numOpt.getOrElse(throw BufferedValue.InvalidData(this, "Expected Num*"))

  /**
   * Returns an Option[BigDecimal] in case this [[BufferedValue]] is a 'Num'.
   */
  def numOpt: Option[BigDecimal] = this match {
    case Num(s, _, _)  => Some(BigDecimal(s))
    case NumLong(l)    => Some(BigDecimal(l))
    case NumDouble(d)  => Some(BigDecimal(d))
    case _             => None
  }
  /**
   * Returns the `BigDecimal` value of this [[BufferedValue]], fails if it is not
   * a [[Num]]
   */
  def timestamp: Instant = timestampOpt.getOrElse(throw BufferedValue.InvalidData(this, "Expected Timestamp"))

  /**
   * Returns an Option[Instant] in case this [[BufferedValue]] is a 'Timestamp'.
   */
  def timestampOpt: Option[Instant] = this match {
    case Timestamp(i) => Some(i)
    case _            => None
  }

  /**
   * Returns the `Boolean` value of this [[BufferedValue]], fails if it is not
   * a [[Bool]]
   */
  def bool: Boolean = boolOpt.getOrElse(throw BufferedValue.InvalidData(this, "Expected Bool"))

  /**
   * Returns an Optional `Boolean` value of this [[BufferedValue]] in case this [[BufferedValue]] is a 'Bool'.
   */
  def boolOpt: Option[Boolean] = this match {
    case True  => Some(true)
    case False => Some(false)
    case _     => None
  }

  /**
   * Returns true if the value of this [[BufferedValue]] is Null, false otherwise
   */
  def isNull: Boolean = this match {
    case Null => true
    case _    => false
  }

  def apply(s: BufferedValue.Selector): BufferedValue = s(this)
  def transform[T](to: Visitor[_, T]): T = BufferedValue.transform(this, to)
}

object BufferedValue extends Transformer[BufferedValue] {
  sealed trait Selector {
    def apply(x: BufferedValue): BufferedValue
  }
  object Selector {
    implicit class IntSelector(i: Int) extends Selector {
      def apply(x: BufferedValue): BufferedValue = x.arr(i)
    }
    implicit class StringSelector(i: String) extends Selector {
      def apply(x: BufferedValue): BufferedValue =
        x.obj.find(_._1 == i).map(_._2).getOrElse(throw BufferedValue.InvalidData(x, s"No value for $i"))
    }
  }

  case class Str(value0: String) extends BufferedValue
  case class Obj(value0: (String, BufferedValue)*) extends BufferedValue
  object Obj {
    def from(items: Iterable[(String, BufferedValue)]): Obj =
      BufferedValue.Obj(items.toSeq: _*)
  }

  case class Arr(value: BufferedValue*) extends BufferedValue
  object Arr {
    def from(items: Iterable[BufferedValue]): Arr =
      Arr(items.toSeq: _*)
  }

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
  object Bool {
    def apply(value: Boolean): BufferedValue = if (value) True else False
  }
  case object Null extends BufferedValue {
    def value = null
  }

  /**
   * Thrown when weepickle tries to convert a JSON blob into a given data
   * structure but fails because part the blob is invalid
   *
   * @param data The section of the JSON blob that weepickle tried to convert.
   *             This could be the entire blob, or it could be some subtree.
   * @param msg Human-readable text saying what went wrong
   */
  case class InvalidData(data: BufferedValue, msg: String) extends Exception(s"$msg (data: $data)")

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
