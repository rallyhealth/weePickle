package com.rallyhealth.weepack.v1

import java.io.ByteArrayOutputStream
import java.time.Instant

import com.rallyhealth.weepickle.v1.core.{ArrVisitor, ObjVisitor, FromData, Visitor}

import scala.collection.compat._
import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

/**
  * In-memory representation of the MessagePack data model
  *
  test - https://msgpack.org/index.html
  *
  * Note that we do not model all the fine details of the MessagePack format
  * in this type; fixed and variable length strings/maps/arrays are all
  * modelled using the same [[Str]]/[[Obj]]/[[Arr]] types, and the various
  * sized integers are all collapsed into [[Int32]]/[[Int64]]/[[UInt64]]. The
  * appropriately sized versions are written out when the message is serialized
  * to bytes.
  */
sealed trait Msg extends FromData {
  def transform[T](to: Visitor[_, T]) = Msg.transform(this, to)

  /**
    * Returns the `String` value of this [[Msg]], fails if it is not
    * a [[com.rallyhealth.weepack.v1.Str]]
    */
  def binary = this match {
    case Binary(value) => value
    case _             => throw Msg.InvalidData(this, "Expected com.rallyhealth.weejson.v1.Str")
  }

  /**
    * Returns the `String` value of this [[Msg]], fails if it is not
    * a [[com.rallyhealth.weepack.v1.Str]]
    */
  def str = this match {
    case Str(value) => value
    case _          => throw Msg.InvalidData(this, "Expected com.rallyhealth.weejson.v1.Str")
  }

  /**
    * Returns the key/value map of this [[Msg]], fails if it is not
    * a [[com.rallyhealth.weepack.v1.Obj]]
    */
  def obj = this match {
    case Obj(value) => value
    case _          => throw Msg.InvalidData(this, "Expected com.rallyhealth.weejson.v1.Obj")
  }

  /**
    * Returns the elements of this [[Msg]], fails if it is not
    * a [[com.rallyhealth.weepack.v1.Arr]]
    */
  def arr = this match {
    case Arr(value) => value
    case _          => throw Msg.InvalidData(this, "Expected com.rallyhealth.weejson.v1.Arr")
  }

  /**
    * Returns the `Double` value of this [[Msg]], fails if it is not
    * a [[com.rallyhealth.weepack.v1.Int32]], [[com.rallyhealth.weepack.v1.Int64]] or [[com.rallyhealth.weepack.v1.UInt64]]
    */
  def int32 = this match {
    case Int32(value)  => value
    case Int64(value)  => value.toInt
    case UInt64(value) => value.toInt
    case _             => throw Msg.InvalidData(this, "Expected com.rallyhealth.weejson.v1.Num")
  }

  /**
    * Returns the `Double` value of this [[Msg]], fails if it is not
    * a [[com.rallyhealth.weepack.v1.Int32]], [[com.rallyhealth.weepack.v1.Int64]] or [[com.rallyhealth.weepack.v1.UInt64]]
    */
  def int64 = this match {
    case Int32(value)  => value.toLong
    case Int64(value)  => value
    case UInt64(value) => value
    case _             => throw Msg.InvalidData(this, "Expected com.rallyhealth.weejson.v1.Num")
  }

  /**
    * Returns the `Boolean` value of this [[Msg]], fails if it is not
    * a [[com.rallyhealth.weepack.v1.Bool]]
    */
  def bool = this match {
    case Bool(value) => value
    case _           => throw Msg.InvalidData(this, "Expected com.rallyhealth.weejson.v1.Bool")
  }

  /**
    * Returns true if the value of this [[Msg]] is com.rallyhealth.weejson.v1.Null, false otherwise
    */
  def isNull = this match {
    case Null => true
    case _    => false
  }

  def writeBytesTo(out: java.io.OutputStream): Unit = {
    this.transform(new MsgPackRenderer(out))
  }
}
case object Null extends Msg
case object True extends Bool {
  def value = true
}
case object False extends Bool {
  def value = false
}
case class Int32(value: Int) extends Msg
case class Int64(value: Long) extends Msg
case class UInt64(value: Long) extends Msg
case class Float32(value: Float) extends Msg
case class Float64(value: Double) extends Msg
case class Str(value: String) extends Msg
case class Binary(value: Array[Byte]) extends Msg
case class Arr(value: mutable.ArrayBuffer[Msg]) extends Msg
object Arr {
  def apply(items: Msg*): Arr = Arr(items.to(mutable.ArrayBuffer))
}
case class Obj(value: mutable.LinkedHashMap[Msg, Msg]) extends Msg
object Obj {
  def apply(item: (Msg, Msg), items: (Msg, Msg)*): Obj = {
    val map = new mutable.LinkedHashMap[Msg, Msg]()
    map.put(item._1, item._2)
    for (i <- items) map.put(i._1, i._2)
    Obj(map)
  }

  def apply(): Obj = Obj(new mutable.LinkedHashMap[Msg, Msg]())
}
case class Ext(tag: Byte, data: Array[Byte]) extends Msg

sealed abstract class Bool extends Msg {
  def value: Boolean
}
object Bool {
  def apply(value: Boolean): Bool = if (value) True else False
  def unapply(bool: Bool): Option[Boolean] = Some(bool.value)
}

object Msg extends MsgVisitor[Msg, Msg] {

  /**
    * Thrown when weepickle tries to convert a JSON blob into a given data
    * structure but fails because part the blob is invalid
    *
    * @param data The section of the JSON blob that weepickle tried to convert.
    *             This could be the entire blob, or it could be some subtree.
    * @param msg Human-readable text saying what went wrong
    */
  case class InvalidData(data: Msg, msg: String) extends Exception(s"$msg (data: $data)")

  sealed trait Selector {
    def apply(x: Msg): Msg
    def update(x: Msg, y: Msg): Unit
  }
  object Selector {
    implicit class IntSelector(i: Int) extends Selector {
      def apply(x: Msg): Msg = x.arr(i)
      def update(x: Msg, y: Msg) = x.arr(i) = y
    }
    implicit class StringSelector(i: String) extends Selector {
      def apply(x: Msg): Msg = x.obj(Str(i))
      def update(x: Msg, y: Msg) = x.obj(Str(i)) = y
    }
    implicit class MsgSelector(i: Msg) extends Selector {
      def apply(x: Msg): Msg = x.obj(i)
      def update(x: Msg, y: Msg) = x.obj(i) = y
    }
  }
  def transform[T](j: Msg, f: Visitor[_, T]): T = {
    j match {
      case Null  => f.visitNull()
      case True  => f.visitTrue()
      case False => f.visitFalse()

      case Int32(value) => f.visitInt32(value)
      case Int64(value) => f.visitInt64(value)

      case UInt64(value) => f.visitUInt64(value)

      case Float32(value) => f.visitFloat32(value)
      case Float64(value) => f.visitFloat64(value)

      case Str(value)    => f.visitString(value)
      case Binary(value) => f.visitBinary(value, 0, value.length)

      case Arr(items) =>
        val arr = f.visitArray(items.length)
        for (i <- items) {
          arr.narrow.visitValue(transform(i, arr.subVisitor))
        }
        arr.visitEnd()

      case Obj(items) =>
        val obj = f.visitObject(items.size)
        for ((k, v) <- items) {
          val keyVisitor = obj.visitKey()
          obj.visitKeyValue(k.transform(keyVisitor))
          obj.narrow.visitValue(transform(v, obj.subVisitor))
        }
        obj.visitEnd()
      case Ext(tag, data) => f.visitExt(tag, data, 0, data.length)

    }
  }
  def visitArray(length: Int): ArrVisitor[Msg, Msg] = new ArrVisitor[Msg, Msg] {
    val arr = ArrayBuffer[Msg]()
    def subVisitor = Msg
    def visitValue(v: Msg): Unit = arr.append(v)
    def visitEnd(): Msg = Arr(arr)
  }

  def visitObject(length: Int): ObjVisitor[Msg, Msg] = new ObjVisitor[Msg, Msg] {
    val map = mutable.LinkedHashMap[Msg, Msg]()
    var lastKey: Msg = null
    def subVisitor = Msg
    def visitValue(v: Msg): Unit = map(lastKey) = v
    def visitEnd(): Msg = Obj(map)
    def visitKey(): Visitor[_, _] = Msg
    def visitKeyValue(s: Any): Unit = lastKey = s.asInstanceOf[Msg]
  }

  def visitNull(): Msg = Null

  def visitFalse(): Msg = False

  def visitTrue(): Msg = True

  def visitFloat64(d: Double): Msg = Float64(d)

  def visitFloat32(d: Float): Msg = Float32(d)

  def visitInt32(i: Int): Msg = Int32(i)

  def visitInt64(l: Long): Msg = Int64(l)

  def visitUInt64(ul: Long): Msg = UInt64(ul)

  def visitString(cs: CharSequence): Msg = Str(cs.toString)

  def visitBinary(bytes: Array[Byte], offset: Int, len: Int): Msg =
    Binary(bytes.slice(offset, offset + len))

  def visitExt(tag: Byte, bytes: Array[Byte], offset: Int, len: Int): Msg =
    Ext(tag, bytes.slice(offset, offset + len))

  def visitChar(c: Char): Msg = Int32(c)

  override def visitTimestamp(instant: Instant): Msg = {
    // DRY, but unoptimized.
    val writer = new MsgPackRenderer(new ByteArrayOutputStream(15))
    val baos = writer.visitTimestamp(instant)
    val arr = baos.toByteArray
    arr(1) match {
      case -1 => visitExt(-1, arr, 2, baos.size() - 2)
      case 12 => visitExt(-1, arr, 3, 12)
    }
  }
}
