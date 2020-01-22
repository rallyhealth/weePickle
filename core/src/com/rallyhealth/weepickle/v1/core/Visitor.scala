package com.rallyhealth.weepickle.v1.core

import java.time.Instant

/**
  * Standard set of hooks weepickle uses to traverse over a structured data.
  * A superset of the JSON, MessagePack, and Scala object  hierarchies, since
  * it needs to support efficiently processing all of them.
  *
  * Note that some parameters are un-set (-1) when not available; e.g.
  * `visitArray`'s `length` is not set when parsing JSON input (since it cannot
  * be known up front) and the various `index` parameters are not set when
  * traversing Scala object hierarchies.
  *
  * When expecting to deal with a subset of the methods; it is common to
  * forward the ones you don't care about to the ones you do; e.g. JSON visitors
  * would forward all `visitFloat32`/`visitInt`/etc. methods to `visitFloat64`
  *
  * @see [[http://www.lihaoyi.com/post/ZeroOverheadTreeProcessingwiththeVisitorPattern.html]]
  * @tparam T the result of [[ObjArrVisitor.subVisitor]] which is passed back into
  *           a [[ArrVisitor]] and [[ObjVisitor]] via [[ObjArrVisitor.visitValue()]].
  *           For example, this might be a weejson.Str that gets passed into an
  *           [[ObjVisitor]] that's building up a weejson.Obj to be returned on [[ObjVisitor.visitEnd()]].
  *           Often [[T]] will be the same type as [[J]] for visitors that return things,
  *           or else [[Any]] by visitors that do their work by side-effecting instead of returning [[J]].
  * @tparam J the result of visiting elements (e.g. a json AST or side-effecting writer)
  */
trait Visitor[-T, +J] extends AutoCloseable {
  /**
    * @return a [[Visitor]] used for visiting the elements of the array
    */
  def visitArray(length: Int): ArrVisitor[T, J]

  /**
    * @return a [[ObjVisitor]] used for visiting the keys/values of the object
    */
  def visitObject(length: Int): ObjVisitor[T, J]

  def visitNull(): J

  def visitFalse(): J

  def visitTrue(): J

  /**
    * Visit the number in its text representation.
    *
    * @param cs       unparsed text representation of the number.
    * @param decIndex index of the `.`, relative to the start of the CharSequence, or -1 if omitted
    * @param expIndex index of `e` or `E` relative to the start of the CharSequence, or -1 if omitted
    */
  def visitFloat64StringParts(cs: CharSequence, decIndex: Int, expIndex: Int): J

  /**
    * Optional handler for raw double values; can be overriden for performance
    * in cases where you're translating directly between numbers to avoid the
    * overhead of stringifying and re-parsing your numbers (e.g. the WebJson
    * transformer gets raw doubles from the underlying Json.parse).
    *
    * Delegates to `visitFloat64StringParts` if not overriden
    *
    * @param d     the input number
    */
  def visitFloat64(d: Double): J
  def visitFloat32(d: Float): J

  def visitInt32(i: Int): J
  def visitInt64(l: Long): J
  def visitUInt64(ul: Long): J

  /**
    * Convenience methods to help you compute the decimal-point-index and
    * exponent-index of an arbitrary numeric string
    *
    * @param s     the text string being visited
    */
  def visitFloat64String(s: String): J

  /**
    * @param cs     the text string being visited
    */
  def visitString(cs: CharSequence): J
  def visitChar(c: Char): J

  /**
    * Raw bytes.
    */
  def visitBinary(bytes: Array[Byte], offset: Int, len: Int): J

  /**
    * MsgPack extension type.
    */
  def visitExt(tag: Byte, bytes: Array[Byte], offset: Int, len: Int): J

  def visitTimestamp(instant: Instant): J

  def map[Z](f: J => Z): Visitor[T, Z] = new Visitor.MapReader[T, J, Z](Visitor.this){
    def mapNonNullsFunction(v: J): Z = f(v)
  }
  def mapNulls[Z](f: J => Z): Visitor[T, Z] = new Visitor.MapReader[T, J, Z](Visitor.this){
    override def mapFunction(v: J): Z = f(v)
    def mapNonNullsFunction(v: J): Z = f(v)
  }

  /**
    * ==Responsibility==
    * Generally, whoever creates the visitor should be responsible for closing it,
    * i.e. not intermediate `transform(v: Visitor)` methods themselves.
    *
    * ==Self Closing==
    * Given that common usage is most often single-valued (e.g. "{}"),
    * rather than multi-valued (e.g. "{} {} {}"), Visitors may self-close
    * (e.g. `visitor.map{v => Try(v.close); v)`} after a single value to
    * prevent resource leaks, but are encouraged to expose both forms
    * (i.e. single/multiple), if supportable.
    *
    * ==Multiple close() calls/Idempotency==
    * Visitors are encouraged to respond gracefully if close() is called multiple times.
    * If an underlying resource would throw if already closed, this may mean adding a
    * `private var isClosed: Boolean` field to prevent multiple calls.
    */
  override def close(): Unit = ()
}

/**
  * Base class for visiting elements of json arrays and objects.
  *
  * @tparam T input result of visiting a child of this object or array.
  *           object or array builders will typically insert this value into their internal Map or Seq.
  * @tparam J output result of visiting elements (e.g. a json AST or side-effecting writer)
  */
sealed trait ObjArrVisitor[-T, +J] {

  /**
    * Called on descent into elements.
    *
    * The returned [[Visitor]] will be used to visit this branch of the json.
    */
  def subVisitor: Visitor[_, _]

  /**
    * Called on completion of visiting an array element or object field value, with the produced result, [[T]].
    *
    * @param v     result of visiting a value in this object or arary
    *              (not the input value, this would have been passed to [[subVisitor]])
    */
  def visitValue(v: T): Unit

  /**
    * Called on end of the object or array.
    *
    * @return the result of visiting this array or object
    */
  def visitEnd(): J

  /**
    * @return true if this is a json object
    *         false if this is a json array
    */
  def isObj: Boolean

  /**
    * Casts [[T]] from _ to [[Any]].
    */
  def narrow: ObjArrVisitor[Any, J] = this.asInstanceOf[ObjArrVisitor[Any, J]]
}
object Visitor{
  class Delegate[T, J](delegatedReader: Visitor[T, J]) extends Visitor[T, J]{

    override def visitNull(): J = delegatedReader.visitNull()
    override def visitTrue(): J = delegatedReader.visitTrue()
    override def visitFalse(): J = delegatedReader.visitFalse()

    override def visitString(cs: CharSequence): J = delegatedReader.visitString(cs)
    override def visitFloat64StringParts(cs: CharSequence, decIndex: Int, expIndex: Int): J = {
      delegatedReader.visitFloat64StringParts(cs, decIndex, expIndex)
    }

    override def visitFloat64(d: Double): J = {
      delegatedReader.visitFloat64(d)
    }
    override def visitObject(length: Int): ObjVisitor[T, J] = delegatedReader.visitObject(length)
    override def visitArray(length: Int): ArrVisitor[T, J] = delegatedReader.visitArray(length)

    override def visitFloat32(d: Float): J = delegatedReader.visitFloat32(d)
    override def visitInt32(i: Int): J = delegatedReader.visitInt32(i)
    override def visitInt64(l: Long): J = delegatedReader.visitInt64(l)
    override def visitUInt64(ul: Long): J = delegatedReader.visitUInt64(ul)
    override def visitFloat64String(s: String): J = delegatedReader.visitFloat64String(s)
    override def visitChar(c: Char): J = delegatedReader.visitChar(c)
    override def visitBinary(bytes: Array[Byte], offset: Int, len: Int): J = delegatedReader.visitBinary(bytes, offset, len)
    override def visitExt(tag: Byte, bytes: Array[Byte], offset: Int, len: Int): J = delegatedReader.visitExt(tag, bytes, offset, len)
    override def visitTimestamp(instant: Instant): J = delegatedReader.visitTimestamp(instant)

    override def close(): Unit = delegatedReader.close()
  }

  class ArrDelegate[T, J](protected val arrVisitor: ArrVisitor[T, J]) extends ArrVisitor[T, J] {

    override def subVisitor: Visitor[Nothing, Any] = arrVisitor.subVisitor

    override def visitValue(v: T): Unit = arrVisitor.visitValue(v)

    override def visitEnd(): J = arrVisitor.visitEnd()

    override def toString: String = arrVisitor.toString
  }

  class ObjDelegate[T, J](protected val objVisitor: ObjVisitor[T, J]) extends ObjVisitor[T, J] {

    override def visitKey(): Visitor[_, _] = objVisitor.visitKey()

    override def visitKeyValue(s: Any): Unit = objVisitor.visitKeyValue(s)

    override def subVisitor: Visitor[Nothing, Any] = objVisitor.subVisitor

    override def visitValue(v: T): Unit = objVisitor.visitValue(v)

    override def visitEnd(): J = objVisitor.visitEnd()

    override def toString: String = objVisitor.toString
  }


  abstract class MapReader[-T, V, Z](delegatedReader: Visitor[T, V]) extends Visitor[T, Z] {

    def mapNonNullsFunction(t: V) : Z
    def mapFunction(v: V): Z =
      if(v == null) null.asInstanceOf[Z]
      else mapNonNullsFunction(v)

    override def visitFalse(): Z = mapFunction(delegatedReader.visitFalse())
    override def visitNull(): Z = mapFunction(delegatedReader.visitNull())
    override def visitFloat64StringParts(cs: CharSequence, decIndex: Int, expIndex: Int): Z = {
      mapFunction(delegatedReader.visitFloat64StringParts(cs, decIndex, expIndex))
    }
    override def visitFloat64(d: Double): Z = {
      mapFunction(delegatedReader.visitFloat64(d))
    }
    override def visitString(cs: CharSequence): Z = {
      mapFunction(delegatedReader.visitString(cs))
    }
    override def visitTrue(): Z = mapFunction(delegatedReader.visitTrue())

    override def visitObject(length: Int): ObjVisitor[T, Z] = {
      new MapObjContext[T, V, Z](delegatedReader.visitObject(length), mapNonNullsFunction)
    }
    override def visitArray(length: Int): ArrVisitor[T, Z] = {
      new MapArrContext[T, V, Z](delegatedReader.visitArray(length), mapNonNullsFunction)
    }

    override def visitFloat32(d: Float): Z = mapFunction(delegatedReader.visitFloat32(d))
    override def visitInt32(i: Int): Z = mapFunction(delegatedReader.visitInt32(i))
    override def visitInt64(l: Long): Z = mapFunction(delegatedReader.visitInt64(l))
    override def visitUInt64(ul: Long): Z = mapFunction(delegatedReader.visitUInt64(ul))
    override def visitFloat64String(s: String): Z = mapFunction(delegatedReader.visitFloat64String(s))
    override def visitChar(c: Char): Z = mapFunction(delegatedReader.visitChar(c))
    override def visitBinary(bytes: Array[Byte], offset: Int, len: Int): Z = mapFunction(delegatedReader.visitBinary(bytes, offset, len))
    override def visitExt(tag: Byte, bytes: Array[Byte], offset: Int, len: Int): Z = mapFunction(delegatedReader.visitExt(tag, bytes, offset, len))
    override def visitTimestamp(instant: Instant): Z = mapFunction(delegatedReader.visitTimestamp(instant))

    override def close(): Unit = delegatedReader.close()
  }


  class MapArrContext[T, V, Z](src: ArrVisitor[T, V], f: V => Z) extends ArrVisitor[T, Z]{
    def subVisitor: Visitor[_, _] = src.subVisitor

    def visitValue(v: T): Unit = src.visitValue(v)

    def visitEnd(): Z = f(src.visitEnd())
  }

  class MapObjContext[T, V, Z](src: ObjVisitor[T, V], f: V => Z) extends ObjVisitor[T, Z]{
    def subVisitor: Visitor[_, _] = src.subVisitor

    def visitKey(): Visitor[_, _] = src.visitKey()
    def visitKeyValue(s: Any) = src.visitKeyValue(s)

    def visitValue(v: T): Unit = src.visitValue(v)

    def visitEnd(): Z = f(src.visitEnd())
  }
}
/**
  * Visits the elements of a json object.
  */
trait ObjVisitor[-T, +J] extends ObjArrVisitor[T, J] {

  /**
    */
  def visitKey(): Visitor[_, _]
  def visitKeyValue(v: Any): Unit
  def isObj = true
  override def narrow: ObjVisitor[Any, J] = this.asInstanceOf[ObjVisitor[Any, J]]
}

/**
  * Visits the elements of a json array.
  */
trait ArrVisitor[-T, +J] extends ObjArrVisitor[T, J]{
  def isObj = false

  override def narrow: ArrVisitor[Any, J] = this.asInstanceOf[ArrVisitor[Any, J]]
}

/**
  * Enriches an exception with parser-level information.
  * This could be a problem with either the parsing
  * or with the Visitor consuming the data.
  *
  * @param shortMsg free-text of what/where went wrong.
  * @param line     line of text (if applicable) (1-indexed)
  * @param col      column of text (if applicable) (1-indexed)
  * @param token    textual representation of the json token (if applicable)
  */
class TransformException(
  val shortMsg: String,
  val jsonPointer: String,
  val index: Option[Long],
  val line: Option[Long],
  val col: Option[Long],
  val token: Option[String],
  cause: Throwable
) extends Exception(
  {
    val sb = new StringBuilder(shortMsg)

    @inline def append(k: String, v: String): Unit = sb.append(' ').append(k).append('=').append(v)

    @inline def appendOpt(k: String, v: Option[Any]): Unit = v.foreach(v => sb.append(' ').append(k).append('=').append(v))

    append("jsonPointer", jsonPointer)
    appendOpt("index", index)
    appendOpt("line", line)
    appendOpt("col", col)
    appendOpt("token", token)
    sb.result()
  },
  cause
) {

  override def fillInStackTrace(): Throwable = {
    // Only include if adds info not already present in the wrapped exception.
    if (cause == null) {
      super.fillInStackTrace()
    }
    this
  }
}

/**
  * Throw this inside a [[Visitor]]'s handler functions to fail the processing
  * of JSON. The Facade just needs to provide the error message, and it is up
  * to the driver to ensure it is wrapped with relevant parser-level information.
  */
class Abort(msg: String) extends Exception(msg)
