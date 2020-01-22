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
    * A combined [[Reader]] and [[Writer]], along with some utility methods.
    */
  trait ReaderWriter[T] extends Reader[T] with Writer[T]{
    override def narrow[K]: ReaderWriter[K] = this.asInstanceOf[ReaderWriter[K]]
    def bimap[V](f: V => T, g: T => V): ReaderWriter[V] = {
      new Visitor.MapReader[Any, T, V](ReaderWriter.this) with ReaderWriter[V]{
        def write0[Z](out: Visitor[_, Z], v: V): Z = {
          ReaderWriter.this.write(out, f(v.asInstanceOf[V]))
        }

        override def mapNonNullsFunction(t: T): V = g(t)
      }
    }
  }

  object ReaderWriter{

    def merge[T](rws: ReaderWriter[_ <: T]*): TaggedReaderWriter[T] = {
      new TaggedReaderWriter.Node(rws.asInstanceOf[Seq[TaggedReaderWriter[T]]]:_*)
    }

    implicit def join[T](implicit r0: Reader[T], w0: Writer[T]): ReaderWriter[T] = (r0, w0) match{
      // Make sure we preserve the tagged-ness of the Readers/Writers being
      // pulled in; we need to do this because the macros that generate tagged
      // Readers/Writers do not know until post-typechecking whether or not the
      // Reader/Writer needs to be tagged, and thus cannot communicate that
      // fact in the returned type of the macro call. Thus we are forced to
      // wait until runtime before inspecting it and seeing if the tags exist

      case (r1: TaggedReader[T], w1: TaggedWriter[T]) =>
        new TaggedReaderWriter[T] {
          override val tagName: String = findTagName(Seq(r1, w1))
          def findReader(s: String): Reader[T] = r1.findReader(s)
          def findWriter(v: Any): (String, CaseW[T]) = w1.findWriter(v)
        }

      case _ =>
        new Visitor.Delegate[Any, T](r0) with ReaderWriter[T]{
          def write0[V](out: Visitor[_, V], v: T): V = w0.write(out, v)
        }
    }
  }

  /**
    * A Reader that throws an error for all the visit methods which it does not define,
    * letting you only define the handlers you care about.
    */
  trait SimpleReader[T] extends Reader[T] with com.rallyhealth.weepickle.v1.core.SimpleVisitor[Any, T]

  /**
    * Represents the ability to read a value of type [[T]].
    *
    * A thin wrapper around [[Visitor]], but needs to be it's own class in order
    * to make type inference automatically pick up it's implicit values.
    */
  trait Reader[T] extends com.rallyhealth.weepickle.v1.core.Visitor[Any, T]{

    override def map[Z](f: T => Z): Reader[Z] = new Reader.MapReader[T, T, Z](Reader.this){
      def mapNonNullsFunction(v: T): Z = f(v)
    }
    override def mapNulls[Z](f: T => Z): Reader[Z] = new Reader.MapReader[T, T, Z](Reader.this){
      override def mapFunction(v: T): Z = f(v)
      def mapNonNullsFunction(v: T): Z = f(v)
    }

    def narrow[K <: T]: Reader[K] = this.asInstanceOf[Reader[K]]
  }

  object Reader{
    class Delegate[T, J](delegatedReader: Visitor[T, J])
      extends Visitor.Delegate[T, J](delegatedReader) with Reader[J]{
      override def visitObject(length: Int, index: Int): ObjVisitor[Any, J] = super.visitObject(length, index).asInstanceOf[ObjVisitor[Any, J]]
      override def visitArray(length: Int, index: Int): ArrVisitor[Any, J] = super.visitArray(length, index).asInstanceOf[ArrVisitor[Any, J]]
    }

    abstract class MapReader[-T, V, Z](delegatedReader: Visitor[T, V])
      extends Visitor.MapReader[T, V, Z](delegatedReader) with Reader[Z] {

      def mapNonNullsFunction(t: V): Z

      override def visitObject(length: Int, index: Int) = super.visitObject(length, index).asInstanceOf[ObjVisitor[Any, Z]]
      override def visitArray(length: Int, index: Int) = super.visitArray(length, index).asInstanceOf[ArrVisitor[Any, Z]]
    }
    def merge[T](readers0: Reader[_ <: T]*) = {
      new TaggedReader.Node(readers0.asInstanceOf[Seq[TaggedReader[T]]]:_*)
    }
  }

  /**
    * Represents the ability to write a value of type [[T]].
    *
    * Generally nothing more than a way of applying the [[T]] to
    * a [[Visitor]], along with some utility methods
    */
  trait Writer[T] {
    def narrow[K] = this.asInstanceOf[Writer[K]]
    def transform[V](v: T, out: Visitor[_, V]) = write(out, v)
    def write0[V](out: Visitor[_, V], v: T): V
    def write[V](out: Visitor[_, V], v: T): V = {
      if (v == null) out.visitNull(-1)
      else write0(out, v)
    }
    def comapNulls[U](f: U => T) = new Writer.MapWriterNulls[U, T](this, f)
    def comap[U](f: U => T) = new Writer.MapWriter[U, T](this, f)
  }
  object Writer {

    class MapWriterNulls[U, T](src: Writer[T], f: U => T) extends Writer[U] {
      override def write[R](out: Visitor[_, R], v: U): R = src.write(out, f(v))
      def write0[R](out: Visitor[_, R], v: U): R = src.write(out, f(v))
    }
    class MapWriter[U, T](src: Writer[T], f: U => T) extends Writer[U] {
      def write0[R](out: Visitor[_, R], v: U): R = src.write(out, f(v))
    }
    def merge[T](writers: Writer[_ <: T]*) = {
      new TaggedWriter.Node(writers.asInstanceOf[Seq[TaggedWriter[T]]]:_*)
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

  class TupleNWriter[V](val writers: Array[Writer[_]], val f: V => Array[Any]) extends Writer[V]{
    def write0[R](out: Visitor[_, R], v: V): R = {
      if (v == null) out.visitNull(-1)
      else{
        val ctx = out.visitArray(writers.length, -1)
        val vs = f(v)
        var i = 0
        while(i < writers.length){
          ctx.visitValue(
            writers(i).asInstanceOf[Writer[Any]].write(
              ctx.subVisitor.asInstanceOf[Visitor[Any, Nothing]],
              vs(i)
            ),
            -1
          )
          i += 1
        }
        ctx.visitEnd(-1)
      }
    }
  }

  class TupleNReader[V](val readers: Array[Reader[_]], val f: Array[Any] => V) extends SimpleReader[V]{

    override def expectedMsg = "expected sequence"
    override def visitArray(length: Int, index: Int) = new ArrVisitor[Any, V] {
      val b = new Array[Any](readers.length)
      var facadesIndex = 0

      var start = facadesIndex
      def visitValue(v: Any, index: Int): Unit = {
        b(facadesIndex % readers.length) = v
        facadesIndex = facadesIndex + 1
      }

      def visitEnd(index: Int) = {
        val lengthSoFar = facadesIndex - start
        if (lengthSoFar != readers.length) {
          throw new Abort(
            "expected " + readers.length + " items in sequence, found " + lengthSoFar, index
          )
        }
        start = facadesIndex

        f(b)

      }

      def subVisitor: Visitor[_, _] = {
        readers(facadesIndex % readers.length)
      }
    }
  }

  abstract class CaseR[V] extends SimpleReader[V]{
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
      def visitValue(v: Any, index: Int): Unit = {
        if (currentIndex != -1 && ((found & (1L << currentIndex)) == 0)) {
          storeAggregatedValue(currentIndex, v)
          found |= (1L << currentIndex)
        }
      }
    }
  }
  trait CaseW[V] extends Writer[V]{
    def length(v: V): Int
    def writeToObject[R](ctx: ObjVisitor[_, R], v: V): Unit
    def write0[R](out: Visitor[_, R], v: V): R = {
      if (v == null) out.visitNull(-1)
      else{
        val ctx = out.visitObject(length(v), -1)
        writeToObject(ctx, v)
        ctx.visitEnd(-1)
      }
    }
  }
  class SingletonR[T](t: T) extends CaseR[T]{
    override def expectedMsg = "expected dictionary"
    override def visitObject(length: Int, index: Int): ObjVisitor[Any, T] = new ObjVisitor[Any, T] {
      def subVisitor: Visitor[_, _] = NoOpVisitor

      def visitKey(index: Int): Visitor[_, _] = NoOpVisitor
      def visitKeyValue(s: Any) = ()

      def visitValue(v: Any, index: Int): Unit = ()

      def visitEnd(index: Int) = t
    }
  }
  class SingletonW[T](f: T) extends CaseW[T] {
    def length(v: T) = 0
    def writeToObject[R](ctx: ObjVisitor[_, R], v: T): Unit = () // do nothing
  }


  def taggedExpectedMsg: String
  def taggedArrayContext[T](taggedReader: TaggedReader[T], index: Int): ArrVisitor[Any, T] = throw new Abort(taggedExpectedMsg, index)
  def taggedObjectContext[T](taggedReader: TaggedReader[T], index: Int): ObjVisitor[Any, T] = throw new Abort(taggedExpectedMsg, index)
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
      * Readers will fast path if this is the first field of the object.
      * Otherwise, Readers will have to buffer the content and find the tag later.
      * While naming, consider that some implementations (e.g. vpack) may sort object keys,
      * so symbol prefixes work well for ensuring the tag is the first property.
      */
    def tagName: String
  }
  trait TaggedReader[T] extends SimpleReader[T] with Tagged {
    def findReader(s: String): Reader[T]

    override def expectedMsg = taggedExpectedMsg
    override def visitArray(length: Int, index: Int) = taggedArrayContext(this, index)
    override def visitObject(length: Int, index: Int) = taggedObjectContext(this, index)
  }
  object TaggedReader{
    class Leaf[T](override val tagName: String, tag: String, r: Reader[T]) extends TaggedReader[T]{
      def findReader(s: String) = if (s == tag) r else null
    }
    class Node[T](rs: TaggedReader[_ <: T]*) extends TaggedReader[T]{
      override val tagName: String = findTagName(rs)
      def findReader(s: String) = scanChildren(rs)(_.findReader(s)).asInstanceOf[Reader[T]]
    }
  }

  trait TaggedWriter[T] extends Writer[T] with Tagged {
    def findWriter(v: Any): (String, CaseW[T])
    def write0[R](out: Visitor[_, R], v: T): R = {
      val (tag, w) = findWriter(v)
      taggedWrite(w, tagName, tag, out, v)

    }
  }
  object TaggedWriter{
    class Leaf[T](c: ClassTag[_], override val tagName: String, tag: String, r: CaseW[T]) extends TaggedWriter[T]{
      def findWriter(v: Any) = {
        if (c.runtimeClass.isInstance(v)) tag -> r
        else null
      }
    }
    class Node[T](rs: TaggedWriter[_ <: T]*) extends TaggedWriter[T]{
      override val tagName: String = findTagName(rs)
      def findWriter(v: Any) = scanChildren(rs)(_.findWriter(v)).asInstanceOf[(String, CaseW[T])]
    }
  }

  trait TaggedReaderWriter[T] extends ReaderWriter[T] with TaggedReader[T] with TaggedWriter[T] with SimpleReader[T]{
    override def visitArray(length: Int, index: Int) = taggedArrayContext(this, index)
    override def visitObject(length: Int, index: Int) = taggedObjectContext(this, index)

  }
  object TaggedReaderWriter{
    class Leaf[T](c: ClassTag[_], override val tagName: String, tag: String, r: CaseW[T] with Reader[T]) extends TaggedReaderWriter[T]{
      def findReader(s: String) = if (s == tag) r else null
      def findWriter(v: Any) = {
        if (c.runtimeClass.isInstance(v)) (tag -> r)
        else null
      }
    }
    class Node[T](rs: TaggedReaderWriter[_ <: T]*) extends TaggedReaderWriter[T]{
      override val tagName: String = findTagName(rs)
      def findReader(s: String) = scanChildren(rs)(_.findReader(s)).asInstanceOf[Reader[T]]
      def findWriter(v: Any) = scanChildren(rs)(_.findWriter(v)).asInstanceOf[(String, CaseW[T])]
    }
  }

}
