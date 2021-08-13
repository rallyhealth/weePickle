package com.rallyhealth.weejson.v1

import java.time.Instant
import com.rallyhealth.weepickle.v1.core.{ArrVisitor, FromInput, JsVisitor, ObjVisitor, Visitor}

import scala.collection.mutable
import scala.util.Try

/**
  * A version of [[com.rallyhealth.weejson.v1.Value]] used to buffer data in raw form.
  *
  * This is used by the case class macros to buffer data for polymorphic types
  * when the discriminator is not the first element, e.g. `{"foo": 1, "\$type": "discriminator"}`.
  * It is important that all types be immutable.
  *
  * May be superior to use in client code as well under some circumstances, e.g.,
  * when representing data from and to non-JSON types. For example, when piped through a
  * `Value`, `java.time.Instant` will come out the other side as `visitString` rather than
  * `visitTimestamp`, leading to potentially wasteful parsing/formatting. Other advantages
  * are: immutable, more efficient number representation (BigDecimal has a lot of performance
  * overhead when applied to smaller numeric types), and direct representation of binary
  * and extension types (without assumed JSON byte array encoding).
  *
  * Most Visitor methods are represented by their own case classes. Exceptions are:
  * - Float64String and UInt64 - along with Float64StringParts, represented as Num
  * - Int32 - along with Int64, represented as NumLong
  * - Float32 - along with Float64, represented as NumDouble
  * - Char - along with String, represented as Str
  *
  * Therefore, when transforming from a [[BufferedValue]], the specific visit methods for these
  * will never be called (e.g., for integers, visitInt32 will never be called, only visitInt64).
  */
sealed trait BufferedValue

object BufferedValueOps {
  implicit class ValueLike(bv: BufferedValue) extends FromInput {

    import BufferedValue._

    /**
     * Returns the `String` value of this [[BufferedValue]], fails if it is not
     * a [[Str]]
     */
    def str: String = strOpt.getOrElse(throw BufferedValue.InvalidData(bv, "Expected Str"))

    /**
     * Returns an Optional `String` value of this [[BufferedValue]] in case this [[BufferedValue]] is a 'String'.
     */
    def strOpt: Option[String] = bv match {
      case Str(value) => Some(value)
      case _ => None
    }

    /**
     * Returns the key/value map of this [[BufferedValue]], fails if it is not
     * a [[Obj]]
     */
    def obj: Seq[(String, BufferedValue)] = objOpt.getOrElse(throw BufferedValue.InvalidData(bv, "Expected Obj"))

    /**
     * Returns an Optional key/value map of this [[BufferedValue]] in case this [[BufferedValue]] is a 'Obj'.
     */
    def objOpt: Option[Seq[(String, BufferedValue)]] = bv match {
      case Obj(items@_*) => Some(items)
      case _ => None
    }

    /**
     * Returns the elements of this [[BufferedValue]], fails if it is not
     * a [[Arr]]
     */
    def arr: Seq[BufferedValue] = arrOpt.getOrElse(throw BufferedValue.InvalidData(bv, "Expected Arr"))

    /**
     * Returns The optional elements of this [[BufferedValue]] in case this [[BufferedValue]] is a 'Arr'.
     */
    def arrOpt: Option[Seq[BufferedValue]] = bv match {
      case Arr(items@_*) => Some(items)
      case _ => None
    }

    /**
     * Returns the `BigDecimal` value of this [[BufferedValue]], fails if it is not
     * a [[Num]]
     */
    def num: BigDecimal = numOpt.getOrElse(throw BufferedValue.InvalidData(bv, "Expected Num*"))

    /**
     * Returns an Option[BigDecimal] in case this [[BufferedValue]] is a 'Num'.
     */
    def numOpt: Option[BigDecimal] = bv match {
      case a: AnyNum => Some(a.value)
      case _ => None
    }

    /**
     * Returns the `BigDecimal` value of this [[BufferedValue]], fails if it is not
     * a [[Num]]
     */
    def timestamp: Instant = timestampOpt.getOrElse(throw BufferedValue.InvalidData(bv, "Expected Timestamp"))

    /**
     * Returns an Option[Instant] in case this [[BufferedValue]] is a 'Timestamp'.
     * (or a string that can be parsed as a timestamp.)
     */
    def timestampOpt: Option[Instant] = bv match {
      case Timestamp(i) => Some(i)
      case Str(s) => Try(Instant.parse(s)).toOption // so it won't blow up if you round-trip through JSON
      case _ => None
    }

    /**
     * Returns the `Boolean` value of this [[BufferedValue]], fails if it is not
     * a [[Bool]]
     */
    def bool: Boolean = boolOpt.getOrElse(throw BufferedValue.InvalidData(bv, "Expected Bool"))

    /**
     * Returns an Optional `Boolean` value of this [[BufferedValue]] in case this [[BufferedValue]] is a 'Bool'.
     */
    def boolOpt: Option[Boolean] = bv match {
      case b: Bool => Some(b.value)
      case _ => None
    }

    /**
     * Returns true if the value of this [[BufferedValue]] is Null, false otherwise
     */
    def isNull: Boolean = bv match {
      case Null => true
      case _ => false
    }

    def apply(s: BufferedValue.Selector): BufferedValue = s(bv)

    override def transform[T](to: Visitor[_, T]): T = BufferedValue.transform(bv, to)
  }
}

object BufferedValue extends Transformer[BufferedValue] {
  import BufferedValueOps._
  sealed trait Selector {
    def apply(x: BufferedValue): BufferedValue
  }
  /*
   * Note that, because objects are represented internally using a sequence of attributes rather than
   * a dictionary-style map, access time may suffer for objects with a large number of attributes. In
   * these cases, it may be best to fetch the sequence and convert it into a map locally (using `.obj.toMap`).
   */
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

  case class Arr(value: BufferedValue*) extends BufferedValue

  sealed trait AnyNum extends BufferedValue {
    def value: BigDecimal
  }
  case class Num(s: String, decIndex: Int, expIndex: Int) extends AnyNum {
    override def value: BigDecimal = BigDecimal(s)
  }
  case class NumLong(l: Long) extends AnyNum {
    override def value: BigDecimal = BigDecimal(l)
  }
  case class NumDouble(d: Double) extends AnyNum {
    override def value: BigDecimal = BigDecimal(d)
  }
  object AnyNum {
    def apply(d: BigDecimal): AnyNum = // precision sensitive
      if (d.isValidLong) NumLong(d.longValue)
      else if (d.isDecimalDouble) NumDouble(d.doubleValue)
      else {
        val s = d.toString
        Num(
          s,
          s.indexOf('.'),
          s.indexOf('E') match {
            case -1 => s.indexOf('e')
            case n => n
          })
      }
  }

  case class Binary(b: Array[Byte]) extends BufferedValue

  case class Ext(tag: Byte, b: Array[Byte]) extends BufferedValue

  case class Timestamp(i: Instant) extends BufferedValue

  sealed trait Bool extends BufferedValue {
    def value: Boolean
  }
  case object False extends Bool {
    override def value = false
  }
  case object True extends Bool {
    override def value = true
  }
  object Bool {
    def apply(value: Boolean): BufferedValue = if (value) True else False
  }

  case object Null extends BufferedValue {
    def value = null
  }

  def fromAttributes(items: Iterable[(String, BufferedValue)]): Obj = Obj(items.toSeq: _*)

  def fromElements(items: Iterable[BufferedValue]): Arr = Arr(items.toSeq: _*)


  /**
   * Thrown when weepickle tries to convert a JSON blob into a given data
   * structure but fails because part the blob is invalid
   *
   * @param data The section of the JSON blob that weepickle tried to convert.
   *             This could be the entire blob, or it could be some subtree.
   * @param msg Human-readable text saying what went wrong
   */
  case class InvalidData(data: BufferedValue, msg: String) extends Exception(s"$msg (data: $data)")

  implicit def BufferableSeq[T](items: Iterable[T])(implicit f: T => BufferedValue): Arr = fromElements(items.map(f))
  implicit def BufferableDict[T](items: Iterable[(String, T)])(implicit f: T => BufferedValue): Obj =
    fromAttributes(items.map(x => (x._1, f(x._2))))
  implicit def BufferableBoolean(i: Boolean): Bool = if (i) True else False
  implicit def BufferableByte(i: Byte): NumLong = NumLong(i.longValue)
  implicit def BufferableShort(i: Short): NumLong = NumLong(i.longValue)
  implicit def BufferableInt(i: Int): NumLong = NumLong(i.longValue)
  implicit def BufferableLong(i: Long): NumLong = NumLong(i)
  implicit def BufferableFloat(i: Float): NumDouble = NumDouble(i.doubleValue)
  implicit def BufferableDouble(i: Double): NumDouble = NumDouble(i)
  implicit def BufferableBigDecimal(i: BigDecimal): AnyNum = AnyNum(i)
  implicit def BufferableNull(i: Null): Null.type = Null
  implicit def BufferableString(s: CharSequence): Str = Str(s.toString)
  implicit def BufferableInstant(dt: Instant): Timestamp = Timestamp(dt)

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
        val ctx = to.visitArray(items.size).narrow
        for (item <- items) ctx.visitValue(transform(item, ctx.subVisitor))
        ctx.visitEnd()
      case BufferedValue.Obj(items @ _*) =>
        val ctx = to.visitObject(items.length).narrow
        for ((k, item) <- items) {
          val keyVisitor = ctx.visitKey()

          ctx.visitKeyValue(keyVisitor.visitString(k))
          ctx.visitValue(transform(item, ctx.subVisitor))
        }
        ctx.visitEnd()
    }
  }

  /**
   * Extending JsVisitor here for bin compat reasons only. Overrides all methods,
   * essentially extending Visitor without hidden JSON "baggage".
   */
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

    /*
     * Not represented by their own case cases. Restates what is in `JsVisitor` for clarity
     * (would prefer not to extend JsVisitor at all).
     */
    override def visitFloat64String(s: String): BufferedValue = BufferedValue.Num(
      s = s,
      decIndex = s.indexOf('.'),
      expIndex = s.indexOf('E') match {
        case -1 => s.indexOf('e')
        case n  => n
      }
    )

    override def visitUInt64(ul: Long): BufferedValue = BufferedValue.Num(
      s = java.lang.Long.toUnsignedString(ul),
      decIndex = -1,
      expIndex = -1
    )

    override def visitInt32(i: Int): BufferedValue = BufferedValue.NumLong(i.longValue)

    override def visitFloat32(f: Float): BufferedValue = BufferedValue.NumDouble(f.doubleValue)

    override def visitChar(c: Char): BufferedValue = BufferedValue.Str(c.toString)

  }
}
