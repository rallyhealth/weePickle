package com.rallyhealth.weejson.v1

import com.rallyhealth.weejson.v1.jackson.ToJson
import com.rallyhealth.weepickle.v1.core.{ArrVisitor, FromInput, ObjVisitor, Visitor}

import scala.collection.JavaConverters._
import scala.collection.compat._
import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

sealed trait Value extends FromInput {
  def value: Any

  /**
    * Returns the `String` value of this [[Value]], fails if it is not
    * a [[Str]]
    */
  def str: String = this match {
    case Str(value) => value
    case _          => throw Value.InvalidData(this, "Expected Str")
  }

  /**
    * Returns an Optional `String` value of this [[Value]] in case this [[Value]] is a 'String'.
    */
  def strOpt: Option[String] = this match {
    case Str(value) => Some(value)
    case _          => None
  }

  /**
    * Returns the key/value map of this [[Value]], fails if it is not
    * a [[Obj]]
    */
  def obj: mutable.Map[String, Value] = this match {
    case Obj(value) => value
    case _          => throw Value.InvalidData(this, "Expected Obj")
  }

  /**
    * Returns an Optional key/value map of this [[Value]] in case this [[Value]] is a 'Obj'.
    */
  def objOpt: Option[mutable.Map[String, Value]] = this match {
    case Obj(value) => Some(value)
    case _          => None
  }

  /**
    * Returns the elements of this [[Value]], fails if it is not
    * a [[Arr]]
    */
  def arr: ArrayBuffer[Value] = this match {
    case Arr(value) => value
    case _          => throw Value.InvalidData(this, "Expected Arr")
  }

  /**
    * Returns The optional elements of this [[Value]] in case this [[Value]] is a 'Arr'.
    */
  def arrOpt: Option[ArrayBuffer[Value]] = this match {
    case Arr(value) => Some(value)
    case _          => None
  }

  /**
    * Returns the `BigDecimal` value of this [[Value]], fails if it is not
    * a [[Num]]
    */
  def num: BigDecimal = this match {
    case Num(value) => value
    case _          => throw Value.InvalidData(this, "Expected Num")
  }

  /**
    * Returns an Option[BigDecimal] in case this [[Value]] is a 'Num'.
    */
  def numOpt: Option[BigDecimal] = this match {
    case Num(value) => Some(value)
    case _          => None
  }

  /**
    * Returns the `Boolean` value of this [[Value]], fails if it is not
    * a [[Bool]]
    */
  def bool = this match {
    case Bool(value) => value
    case _           => throw Value.InvalidData(this, "Expected Bool")
  }

  /**
    * Returns an Optional `Boolean` value of this [[Value]] in case this [[Value]] is a 'Bool'.
    */
  def boolOpt: Option[Boolean] = this match {
    case Bool(value) => Some(value)
    case _           => None
  }

  /**
    * Returns true if the value of this [[Value]] is Null, false otherwise
    */
  def isNull: Boolean = this match {
    case Null => true
    case _    => false
  }

  def apply(s: Value.Selector): Value = s(this)
  def update(s: Value.Selector, v: Value): Unit = s(this) = v

  /**
    * Update a value in-place. Takes an `Int` or a `String`, through the
    * implicitly-constructe [[Value.Selector]] type.
    *
    * We cannot just overload `update` on `s: Int` and `s: String` because
    * of type inference problems in Scala 2.11.
    */
  def update[V](s: Value.Selector, f: Value => V)(implicit v: V => Value): Unit = s(this) = v(f(s(this)))

  def transform[T](to: Visitor[_, T]): T = Value.transform(this, to)

  override def toString = transform(ToJson.string)
}

/**
  * A very small, very simple JSON AST that weepickle uses as part of its
  * serialization process. A common standard between the Jawn AST (which
  * we don't use so we don't pull in the bulk of Spire) and the Javascript
  * JSON AST.
  */
object Value extends AstTransformer[Value] {
  type Value = com.rallyhealth.weejson.v1.Value
  sealed trait Selector {
    def apply(x: Value): Value
    def update(x: Value, y: Value): Unit
  }
  object Selector {
    implicit class IntSelector(i: Int) extends Selector {
      def apply(x: Value): Value = x.arr(i)
      def update(x: Value, y: Value) = x.arr(i) = y
    }
    implicit class StringSelector(i: String) extends Selector {
      def apply(x: Value): Value = x.obj(i)
      def update(x: Value, y: Value) = x.obj(i) = y
    }
  }

  implicit def JsonableSeq[T](items: TraversableOnce[T])(implicit f: T => Value): Arr = Arr.from(items.map(f))
  implicit def JsonableDict[T](items: TraversableOnce[(String, T)])(implicit f: T => Value): Obj =
    Obj.from(items.map(x => (x._1, f(x._2))))
  implicit def JsonableBoolean(i: Boolean): Bool = if (i) True else False
  implicit def JsonableByte(i: Byte): Num = Num(BigDecimal(i))
  implicit def JsonableShort(i: Short): Num = Num(BigDecimal(i))
  implicit def JsonableInt(i: Int): Num = Num(i)
  implicit def JsonableLong(i: Long): Num = Num(i)
  implicit def JsonableFloat(i: Float): Num = Num(BigDecimal.decimal(i))
  implicit def JsonableDouble(i: Double): Num = Num(i)
  implicit def JsonableBigDecimal(i: BigDecimal): Num = Num(i)
  implicit def JsonableNull(i: Null): Null.type = Null
  implicit def JsonableString(s: CharSequence): Str = Str(s.toString)

  def transform[T](i: Value, to: Visitor[_, T]): T = {
    i match {
      case Null   => to.visitNull()
      case True   => to.visitTrue()
      case False  => to.visitFalse()
      case Str(s) => to.visitString(s)
      case Num(d) =>
        // precision sensitive
        if (d.isValidLong) to.visitInt64(d.longValue)
        else if (d.isDecimalDouble) to.visitFloat64(d.doubleValue)
        else to.visitFloat64String(d.toString)
      case Arr(items) => transformArray(to, items)
      case Obj(items) => transformObject(to, items)
    }
  }

  def visitArray(length: Int): ArrVisitor[Value, Value] = new AstArrVisitor[ArrayBuffer](xs => Arr(xs))

  def visitObject(length: Int): ObjVisitor[Value, Value] =
    new AstObjVisitor[mutable.LinkedHashMap[String, Value]](xs => Obj(xs))

  def visitNull(): Value = Null

  def visitFalse(): Value = False

  def visitTrue(): Value = True

  override def visitFloat64StringParts(cs: CharSequence, decIndex: Int, expIndex: Int): Num = {
    Num(BigDecimal(cs.toString))
  }

  override def visitInt32(i: Int): Value = Num(BigDecimal(i))

  override def visitInt64(l: Long): Value = Num(BigDecimal(l))

  override def visitFloat64(d: Double): Value = Num(BigDecimal(d))

  def visitString(cs: CharSequence): Value = Str(cs.toString)

  /**
    * Thrown when weepickle tries to convert a JSON blob into a given data
    * structure but fails because part the blob is invalid
    *
    * @param data The section of the JSON blob that weepickle tried to convert.
    *             This could be the entire blob, or it could be some subtree.
    * @param msg Human-readable text saying what went wrong
    */
  case class InvalidData(data: Value, msg: String) extends Exception(s"$msg (data: $data)")
}

case class Str(value: String) extends Value
case class Obj(value: mutable.Map[String, Value]) extends Value

object Obj {
  implicit def from(items: TraversableOnce[(String, Value)]): Obj = {
    val initialCapacity = items match {
      case is: mutable.IndexedSeq[_] => is.size
      case _ => 2
    }
    Obj(new java.util.LinkedHashMap[String, Value](initialCapacity).asScala ++= items)
  }
  // Weird telescoped version of `apply(items: (String, Value)*)`, to avoid
  // type inference issues due to overloading the existing `apply` method
  // generated by the case class itself
  // https://github.com/lihaoyi/upickle/issues/230
  def apply[V](item: (String, V), items: (String, Value)*)(implicit viewBound: V => Value): Obj = {
    val map = new java.util.LinkedHashMap[String, Value](items.size).asScala
    map.put(item._1, item._2)
    for (i <- items) map.put(i._1, i._2)
    Obj(map)
  }

  def apply(): Obj = Obj(new java.util.LinkedHashMap[String, Value](2).asScala)
}
case class Arr(value: ArrayBuffer[Value]) extends Value

object Arr {
  implicit def from[T](items: TraversableOnce[T])(implicit viewBound: T => Value): Arr =
    Arr(items.map(x => x: Value).to(mutable.ArrayBuffer))

  def apply(items: Value*): Arr = new Arr(items.to(mutable.ArrayBuffer))
}
case class Num(value: BigDecimal) extends Value
sealed abstract class Bool extends Value {
  def value: Boolean
}
object Bool {
  def apply(value: Boolean): Bool = if (value) True else False
  def unapply(bool: Bool): Option[Boolean] = Some(bool.value)
}
case object False extends Bool {
  def value = false
}
case object True extends Bool {
  def value = true
}
case object Null extends Value {
  def value = null
}
