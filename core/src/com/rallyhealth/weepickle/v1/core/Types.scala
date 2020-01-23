package com.rallyhealth.weepickle.v1.core

import scala.language.experimental.macros
import scala.reflect.ClassTag

/**
* Basic functionality to be able to read and write objects. Kept as a trait so
* other internal files can use it, while also mixing it into the `com.rallyhealth.weepickle.v1`
* package to form the public API.
*/
trait Types{ types =>

  /**
    * A combined [[Receiver]] and [[Transmitter]], along with some utility methods.
    */
  trait Transceiver[T] extends Receiver[T] with Transmitter[T]{
    override def narrow[K]: Transceiver[K] = this.asInstanceOf[Transceiver[K]]
    def bimap[In](f: In => T, g: T => In): Transceiver[In] = {
      new Visitor.MapReceiver[Any, T, In](Transceiver.this) with Transceiver[In]{
        def transmit0[Out](in: In, out: Visitor[_, Out]): Out = {
          Transceiver.this.transmit(f(in.asInstanceOf[In]), out)
        }

        override def mapNonNullsFunction(t: T): In = g(t)
      }
    }
  }

  object Transceiver{

    def merge[T](rws: Transceiver[_ <: T]*): TaggedTransceiver[T] = {
      new TaggedTransceiver.Node(rws.asInstanceOf[Seq[TaggedTransceiver[T]]]:_*)
    }

    implicit def join[T](implicit r0: Receiver[T], w0: Transmitter[T]): Transceiver[T] = (r0, w0) match{
      // Make sure we preserve the tagged-ness of the Receivers/Transmitters being
      // pulled in; we need to do this because the macros that generate tagged
      // Receivers/Transmitters do not know until post-typechecking whether or not the
      // Receiver/Transmitter needs to be tagged, and thus cannot communicate that
      // fact in the returned type of the macro call. Thus we are forced to
      // wait until runtime before inspecting it and seeing if the tags exist

      case (r1: TaggedReceiver[T], w1: TaggedTransmitter[T]) =>
        new TaggedTransceiver[T] {
          override val tagName: String = findTagName(Seq(r1, w1))
          def findReceiver(s: String): Receiver[T] = r1.findReceiver(s)
          def findTransmitter(v: Any): (String, CaseW[T]) = w1.findTransmitter(v)
        }

      case _ =>
        new Visitor.Delegate[Any, T](r0) with Transceiver[T]{
          def transmit0[V](v: T, out: Visitor[_, V]): V = w0.transmit(v, out)
        }
    }
  }

  /**
    * A Receiver that throws an error for all the visit methods which it does not define,
    * letting you only define the handlers you care about.
    */
  trait SimpleReceiver[T] extends Receiver[T] with com.rallyhealth.weepickle.v1.core.SimpleVisitor[Any, T]

  /**
    * Represents the ability to read a value of type [[T]].
    *
    * A thin wrapper around [[Visitor]], but needs to be it's own class in order
    * to make type inference automatically pick up it's implicit values.
    */
  trait Receiver[T] extends com.rallyhealth.weepickle.v1.core.Visitor[Any, T]{

    override def map[Z](f: T => Z): Receiver[Z] = new Receiver.MapReceiver[T, T, Z](Receiver.this){
      def mapNonNullsFunction(v: T): Z = f(v)
    }
    override def mapNulls[Z](f: T => Z): Receiver[Z] = new Receiver.MapReceiver[T, T, Z](Receiver.this){
      override def mapFunction(v: T): Z = f(v)
      def mapNonNullsFunction(v: T): Z = f(v)
    }

    def narrow[K <: T]: Receiver[K] = this.asInstanceOf[Receiver[K]]
  }

  object Receiver{
    class Delegate[T, J](delegatedReceiver: Visitor[T, J])
      extends Visitor.Delegate[T, J](delegatedReceiver) with Receiver[J]{
      override def visitObject(length: Int): ObjVisitor[Any, J] = super.visitObject(length).asInstanceOf[ObjVisitor[Any, J]]
      override def visitArray(length: Int): ArrVisitor[Any, J] = super.visitArray(length).asInstanceOf[ArrVisitor[Any, J]]
    }

    abstract class MapReceiver[-T, V, Z](delegatedReceiver: Visitor[T, V])
      extends Visitor.MapReceiver[T, V, Z](delegatedReceiver) with Receiver[Z] {

      def mapNonNullsFunction(t: V): Z

      override def visitObject(length: Int): ObjVisitor[Any, Z] = super.visitObject(length).asInstanceOf[ObjVisitor[Any, Z]]
      override def visitArray(length: Int): ArrVisitor[Any, Z] = super.visitArray(length).asInstanceOf[ArrVisitor[Any, Z]]
    }
    def merge[T](readers0: Receiver[_ <: T]*) = {
      new TaggedReceiver.Node(readers0.asInstanceOf[Seq[TaggedReceiver[T]]]:_*)
    }
  }

  /**
    * Represents the ability to write a value of type [[In]].
    *
    * Generally nothing more than a way of applying the [[In]] to
    * a [[Visitor]], along with some utility methods
    */
  trait Transmitter[In] {
    def narrow[K] = this.asInstanceOf[Transmitter[K]]
    def transmit[Out](in: In, out: Visitor[_, Out]): Out = {
      if (in == null) out.visitNull()
      else transmit0(in, out)
    }
    def transmit0[Out](in: In, out: Visitor[_, Out]): Out
    def comapNulls[U](f: U => In) = new Transmitter.MapTransmitterNulls[U, In](this, f)
    def comap[U](f: U => In) = new Transmitter.MapTransmitter[U, In](this, f)
  }
  object Transmitter {

    class MapTransmitterNulls[U, T](src: Transmitter[T], f: U => T) extends Transmitter[U] {
      override def transmit[R](u: U, out: Visitor[_, R]): R = src.transmit(f(u), out)
      def transmit0[R](v: U, out: Visitor[_, R]): R = src.transmit(f(v), out)
    }
    class MapTransmitter[U, T](src: Transmitter[T], f: U => T) extends Transmitter[U] {
      def transmit0[R](v: U, out: Visitor[_, R]): R = src.transmit(f(v), out)
    }
    def merge[T](writers: Transmitter[_ <: T]*) = {
      new TaggedTransmitter.Node(writers.asInstanceOf[Seq[TaggedTransmitter[T]]]:_*)
    }
  }

  private def findTagName(ts: Seq[Tagged]): String = {
    val tagName = ts.head.tagName
    for (t <- ts.iterator.drop(1)) {
      // Enforce consistent tag names.
      if (t.tagName != tagName) throw new IllegalArgumentException(s"Inconsistent tag names: [$tagName, ${t.tagName}]")
    }
    tagName
  }

  class TupleNTransmitter[In](val writers: Array[Transmitter[_]], val f: In => Array[Any]) extends Transmitter[In]{
    def transmit0[Out](in: In, out: Visitor[_, Out]): Out = {
      if (in == null) out.visitNull()
      else{
        val ctx = out.visitArray(writers.length)
        val vs = f(in)
        var i = 0
        while(i < writers.length){
          ctx.visitValue(writers(i).asInstanceOf[Transmitter[Any]].transmit(vs(i), ctx.subVisitor.asInstanceOf[Visitor[Any, Nothing]]))
          i += 1
        }
        ctx.visitEnd()
      }
    }
  }

  class TupleNReceiver[V](val readers: Array[Receiver[_]], val f: Array[Any] => V) extends SimpleReceiver[V]{

    override def expectedMsg = "expected sequence"
    override def visitArray(length: Int): ArrVisitor[Any, V] = new ArrVisitor[Any, V] {
      val b = new Array[Any](readers.length)
      var facadesIndex = 0

      var start = facadesIndex
      def visitValue(v: Any): Unit = {
        b(facadesIndex % readers.length) = v
        facadesIndex = facadesIndex + 1
      }

      def visitEnd(): V = {
        val lengthSoFar = facadesIndex - start
        if (lengthSoFar != readers.length) {
          throw new Abort("expected " + readers.length + " items in sequence, found " + lengthSoFar)
        }
        start = facadesIndex

        f(b)

      }

      def subVisitor: Visitor[_, _] = {
        readers(facadesIndex % readers.length)
      }
    }
  }

  abstract class CaseR[V] extends SimpleReceiver[V]{
    override def expectedMsg = "expected dictionary"
    trait CaseObjectContext extends ObjVisitor[Any, V]{

      /**
        * Stores a value for a specific field index. Implemented by macroR.
        */
      def storeAggregatedValue(currentIndex: Int, v: Any): Unit

      /**
        * Bitset supporting up to 64 fields.
        */
      var found = 0L

      /**
        * Set by [[visitKeyValue]]
        */
      var currentIndex = -1
      def visitValue(v: Any): Unit = {
        if (currentIndex != -1 && ((found & (1L << currentIndex)) == 0)) {
          storeAggregatedValue(currentIndex, v)
          found |= (1L << currentIndex)
        }
      }
    }
  }
  trait CaseW[In] extends Transmitter[In]{
    def length(v: In): Int
    def writeToObject[R](ctx: ObjVisitor[_, R], v: In): Unit
    def transmit0[Out](in: In, out: Visitor[_, Out]): Out = {
      if (in == null) out.visitNull()
      else{
        val ctx = out.visitObject(length(in))
        writeToObject(ctx, in)
        ctx.visitEnd()
      }
    }
  }
  class SingletonR[T](t: T) extends CaseR[T]{
    override def expectedMsg = "expected dictionary"
    override def visitObject(length: Int): ObjVisitor[Any, T] = new ObjVisitor[Any, T] {
      def subVisitor: Visitor[_, _] = NoOpVisitor

      def visitKey(): Visitor[_, _] = NoOpVisitor
      def visitKeyValue(s: Any) = ()

      def visitValue(v: Any): Unit = ()

      def visitEnd(): T = t
    }
  }
  class SingletonW[T](f: T) extends CaseW[T] {
    def length(v: T) = 0
    def writeToObject[R](ctx: ObjVisitor[_, R], v: T): Unit = () // do nothing
  }


  def taggedExpectedMsg: String
  def taggedArrayContext[T](taggedReceiver: TaggedReceiver[T]): ArrVisitor[Any, T] = throw new Abort(taggedExpectedMsg)
  def taggedObjectContext[T](taggedReceiver: TaggedReceiver[T]): ObjVisitor[Any, T] = throw new Abort(taggedExpectedMsg)
  def taggedWrite[T, R](w: CaseW[T], tagName: String, tag: String, out: Visitor[_, R], v: T): R

  private[this] def scanChildren[T, V](xs: Seq[T])(f: T => V) = {
    var x: V = null.asInstanceOf[V]
    val i = xs.iterator
    while(x == null && i.hasNext){
      val t = f(i.next())
      if(t != null) x = t
    }
    x
  }
  trait Tagged {

    /**
      * Name of the object key used to identify the subclass tag.
      * Receivers will fast path if this is the first field of the object.
      * Otherwise, Receivers will have to buffer the content and find the tag later.
      * While naming, consider that some implementations (e.g. vpack) may sort object keys,
      * so symbol prefixes work well for ensuring the tag is the first property.
      */
    def tagName: String
  }
  trait TaggedReceiver[T] extends SimpleReceiver[T] with Tagged {
    def findReceiver(s: String): Receiver[T]

    override def expectedMsg = taggedExpectedMsg
    override def visitArray(length: Int) = taggedArrayContext(this)
    override def visitObject(length: Int) = taggedObjectContext(this)
  }
  object TaggedReceiver{
    class Leaf[T](override val tagName: String, tag: String, r: Receiver[T]) extends TaggedReceiver[T]{
      def findReceiver(s: String) = if (s == tag) r else null
    }
    class Node[T](rs: TaggedReceiver[_ <: T]*) extends TaggedReceiver[T]{
      override val tagName: String = findTagName(rs)
      def findReceiver(s: String) = scanChildren(rs)(_.findReceiver(s)).asInstanceOf[Receiver[T]]
    }
  }

  trait TaggedTransmitter[In] extends Transmitter[In] with Tagged {
    def findTransmitter(v: Any): (String, CaseW[In])
    override def transmit0[Out](in: In, out: Visitor[_, Out]): Out = {
      val (tag, w) = findTransmitter(in)
      taggedWrite(w, tagName, tag, out, in)

    }
  }
  object TaggedTransmitter{
    class Leaf[T](c: ClassTag[_], override val tagName: String, tag: String, r: CaseW[T]) extends TaggedTransmitter[T]{
      def findTransmitter(v: Any) = {
        if (c.runtimeClass.isInstance(v)) tag -> r
        else null
      }
    }
    class Node[T](rs: TaggedTransmitter[_ <: T]*) extends TaggedTransmitter[T]{
      override val tagName: String = findTagName(rs)
      def findTransmitter(v: Any) = scanChildren(rs)(_.findTransmitter(v)).asInstanceOf[(String, CaseW[T])]
    }
  }

  trait TaggedTransceiver[T] extends Transceiver[T] with TaggedReceiver[T] with TaggedTransmitter[T] with SimpleReceiver[T]{
    override def visitArray(length: Int) = taggedArrayContext(this)
    override def visitObject(length: Int) = taggedObjectContext(this)

  }
  object TaggedTransceiver{
    class Leaf[T](c: ClassTag[_], override val tagName: String, tag: String, r: CaseW[T] with Receiver[T]) extends TaggedTransceiver[T]{
      def findReceiver(s: String) = if (s == tag) r else null
      def findTransmitter(v: Any) = {
        if (c.runtimeClass.isInstance(v)) (tag -> r)
        else null
      }
    }
    class Node[T](rs: TaggedTransceiver[_ <: T]*) extends TaggedTransceiver[T]{
      override val tagName: String = findTagName(rs)
      def findReceiver(s: String) = scanChildren(rs)(_.findReceiver(s)).asInstanceOf[Receiver[T]]
      def findTransmitter(v: Any) = scanChildren(rs)(_.findTransmitter(v)).asInstanceOf[(String, CaseW[T])]
    }
  }

}
