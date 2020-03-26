package com.rallyhealth.weepickle.v1.core

import scala.language.experimental.macros
import scala.reflect.ClassTag

import VisitorImplicits._

/**
  * Basic functionality to be able to read and write objects. Kept as a trait so
  * other internal files can use it, while also mixing it into the `com.rallyhealth.weepickle.v1`
  * package to form the public API.
  */
trait Types { types =>

  /**
    * A combined [[To]] and [[From]], along with some utility methods.
    */
  trait FromTo[T] extends From[T] with To[T] {
    override def narrow[K]: FromTo[K] = this.asInstanceOf[FromTo[K]]
    def bimap[In](f: In => T, g: T => In): FromTo[In] = {
      new Visitor.MapTo[Any, T, In](FromTo.this) with FromTo[In] {
        def transform0[Out](in: In, out: Visitor[_, Out]): Out = {
          FromTo.this.transform(f(in.asInstanceOf[In]), out)
        }

        override def mapNonNullsFunction(t: T): In = g(t)
      }
    }
    // Map the keys, coming and going
    def bimapKeys(fromMapper: CharSequence => CharSequence, toMapper: CharSequence => CharSequence): FromTo[T] =
      FromTo.join(this.mapKeysTo(toMapper), this.comapKeysFrom(fromMapper))
  }

  object FromTo {

    def merge[T](rws: FromTo[_ <: T]*): TaggedFromTo[T] = {
      new TaggedFromTo.Node(rws.asInstanceOf[Seq[TaggedFromTo[T]]]: _*)
    }

    implicit def join[T](implicit r0: To[T], w0: From[T]): FromTo[T] = (r0, w0) match {
      // Make sure we preserve the tagged-ness of the Tos/Froms being
      // pulled in; we need to do this because the macros that generate tagged
      // Tos/Froms do not know until post-typechecking whether or not the
      // To/From needs to be tagged, and thus cannot communicate that
      // fact in the returned type of the macro call. Thus we are forced to
      // wait until runtime before inspecting it and seeing if the tags exist

      case (r1: TaggedTo[T], w1: TaggedFrom[T]) =>
        new TaggedFromTo[T] {
          override val tagName: String = findTagName(Seq(r1, w1))
          def findTo(s: String): To[T] = r1.findTo(s)
          def findFrom(v: Any): (String, CaseW[T]) = w1.findFrom(v)
        }

      case _ =>
        new Visitor.Delegate[Any, T](r0) with FromTo[T] {
          def transform0[V](v: T, out: Visitor[_, V]): V = w0.transform(v, out)
        }
    }
  }

  /**
    * A To that throws an error for all the visit methods which it does not define,
    * letting you only define the handlers you care about.
    */
  trait SimpleTo[T] extends To[T] with com.rallyhealth.weepickle.v1.core.SimpleVisitor[Any, T]

  /**
    * Represents the ability to read a value of type [[T]].
    *
    * A thin wrapper around [[Visitor]], but needs to be it's own class in order
    * to make type inference automatically pick up it's implicit values.
    */
  trait To[T] extends com.rallyhealth.weepickle.v1.core.Visitor[Any, T] {

    override def map[Z](f: T => Z): To[Z] = new To.MapTo[T, T, Z](To.this) {
      def mapNonNullsFunction(v: T): Z = f(v)
    }
    override def mapNulls[Z](f: T => Z): To[Z] = new To.MapTo[T, T, Z](To.this) {
      override def mapFunction(v: T): Z = f(v)
      def mapNonNullsFunction(v: T): Z = f(v)
    }
    def mapKeysTo(f: CharSequence => CharSequence): To[T] =
      new To.Delegate[T, T](To.this.mapKeys(f))

    def narrow[K <: T]: To[K] = this.asInstanceOf[To[K]]
  }

  object To {
    class Delegate[T, J](delegatedTo: Visitor[T, J])
        extends Visitor.Delegate[T, J](delegatedTo)
        with To[J] {
      override def visitObject(length: Int): ObjVisitor[Any, J] =
        super.visitObject(length).asInstanceOf[ObjVisitor[Any, J]]
      override def visitArray(length: Int): ArrVisitor[Any, J] =
        super.visitArray(length).asInstanceOf[ArrVisitor[Any, J]]
    }

    abstract class MapTo[-T, V, Z](delegatedTo: Visitor[T, V])
        extends Visitor.MapTo[T, V, Z](delegatedTo)
        with To[Z] {

      def mapNonNullsFunction(t: V): Z

      override def visitObject(length: Int): ObjVisitor[Any, Z] =
        super.visitObject(length).asInstanceOf[ObjVisitor[Any, Z]]
      override def visitArray(length: Int): ArrVisitor[Any, Z] =
        super.visitArray(length).asInstanceOf[ArrVisitor[Any, Z]]
    }
    def merge[T](readers0: To[_ <: T]*) = {
      new TaggedTo.Node(readers0.asInstanceOf[Seq[TaggedTo[T]]]: _*)
    }
  }

  /**
    * Represents the ability to write a value of type [[In]].
    *
    * Generally nothing more than a way of applying the [[In]] to
    * a [[Visitor]], along with some utility methods
    */
  trait From[In] {
    def narrow[K] = this.asInstanceOf[From[K]]
    def transform[Out](in: In, out: Visitor[_, Out]): Out = {
      if (in == null) out.visitNull()
      else transform0(in, out)
    }
    def transform0[Out](in: In, out: Visitor[_, Out]): Out
    def comapNulls[U](f: U => In) = new From.MapFromNulls[U, In](this, f)
    def comap[U](f: U => In) = new From.MapFrom[U, In](this, f)
    def comapKeysFrom(f: CharSequence => CharSequence): From[In] = new From.MapKeys[In](this, f)
  }
  object From {

    class MapFromNulls[U, T](src: From[T], f: U => T) extends From[U] {
      override def transform[R](u: U, out: Visitor[_, R]): R = src.transform(f(u), out)
      def transform0[R](v: U, out: Visitor[_, R]): R = src.transform(f(v), out)
    }
    class MapFrom[U, T](src: From[T], f: U => T) extends From[U] {
      def transform0[R](v: U, out: Visitor[_, R]): R = src.transform(f(v), out)
    }
    class MapKeys[T](src: From[T], f: CharSequence => CharSequence) extends From[T] {
      def transform0[R](v: T, out: Visitor[_, R]): R = src.transform(v, out.mapKeys(f))
    }
    def merge[T](writers: From[_ <: T]*) = {
      new TaggedFrom.Node(writers.asInstanceOf[Seq[TaggedFrom[T]]]: _*)
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

  class TupleNFrom[In](val writers: Array[From[_]], val f: In => Array[Any]) extends From[In] {
    def transform0[Out](in: In, out: Visitor[_, Out]): Out = {
      if (in == null) out.visitNull()
      else {
        val ctx = out.visitArray(writers.length)
        val vs = f(in)
        var i = 0
        while (i < writers.length) {
          ctx.visitValue(
            writers(i)
              .asInstanceOf[From[Any]]
              .transform(vs(i), ctx.subVisitor.asInstanceOf[Visitor[Any, Nothing]])
          )
          i += 1
        }
        ctx.visitEnd()
      }
    }
  }

  class TupleNTo[V](val readers: Array[To[_]], val f: Array[Any] => V) extends SimpleTo[V] {

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

  abstract class CaseR[V] extends SimpleTo[V] {
    override def expectedMsg = "expected dictionary"
    trait CaseObjectContext extends ObjVisitor[Any, V] {

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
  trait CaseW[In] extends From[In] {
    def length(v: In): Int
    def writeToObject[R](ctx: ObjVisitor[_, R], v: In): Unit
    def transform0[Out](in: In, out: Visitor[_, Out]): Out = {
      if (in == null) out.visitNull()
      else {
        val ctx = out.visitObject(length(in))
        writeToObject(ctx, in)
        ctx.visitEnd()
      }
    }
  }
  class SingletonR[T](t: T) extends CaseR[T] {
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
  def taggedArrayContext[T](taggedTo: TaggedTo[T]): ArrVisitor[Any, T] = throw new Abort(taggedExpectedMsg)
  def taggedObjectContext[T](taggedTo: TaggedTo[T]): ObjVisitor[Any, T] = throw new Abort(taggedExpectedMsg)
  def taggedWrite[T, R](w: CaseW[T], tagName: String, tag: String, out: Visitor[_, R], v: T): R

  private[this] def scanChildren[T, V](xs: Seq[T])(f: T => V) = {
    var x: V = null.asInstanceOf[V]
    val i = xs.iterator
    while (x == null && i.hasNext) {
      val t = f(i.next())
      if (t != null) x = t
    }
    x
  }
  trait Tagged {

    /**
      * Name of the object key used to identify the subclass tag.
      * Tos will fast path if this is the first field of the object.
      * Otherwise, Tos will have to buffer the content and find the tag later.
      * While naming, consider that some implementations (e.g. vpack) may sort object keys,
      * so symbol prefixes work well for ensuring the tag is the first property.
      */
    def tagName: String
  }
  trait TaggedTo[T] extends SimpleTo[T] with Tagged {
    def findTo(s: String): To[T]

    override def expectedMsg = taggedExpectedMsg
    override def visitArray(length: Int) = taggedArrayContext(this)
    override def visitObject(length: Int) = taggedObjectContext(this)
  }
  object TaggedTo {
    class Leaf[T](override val tagName: String, tag: String, r: To[T]) extends TaggedTo[T] {
      def findTo(s: String) = if (s == tag) r else null
    }
    class Node[T](rs: TaggedTo[_ <: T]*) extends TaggedTo[T] {
      override val tagName: String = findTagName(rs)
      def findTo(s: String) = scanChildren(rs)(_.findTo(s)).asInstanceOf[To[T]]
    }
  }

  trait TaggedFrom[In] extends From[In] with Tagged {
    def findFrom(v: Any): (String, CaseW[In])
    override def transform0[Out](in: In, out: Visitor[_, Out]): Out = {
      val (tag, w) = findFrom(in)
      taggedWrite(w, tagName, tag, out, in)

    }
  }
  object TaggedFrom {
    class Leaf[T](c: ClassTag[_], override val tagName: String, tag: String, r: CaseW[T]) extends TaggedFrom[T] {
      def findFrom(v: Any) = {
        if (c.runtimeClass.isInstance(v)) tag -> r
        else null
      }
    }
    class Node[T](rs: TaggedFrom[_ <: T]*) extends TaggedFrom[T] {
      override val tagName: String = findTagName(rs)
      def findFrom(v: Any) = scanChildren(rs)(_.findFrom(v)).asInstanceOf[(String, CaseW[T])]
    }
  }

  trait TaggedFromTo[T]
      extends FromTo[T]
      with TaggedTo[T]
      with TaggedFrom[T]
      with SimpleTo[T] {
    override def visitArray(length: Int) = taggedArrayContext(this)
    override def visitObject(length: Int) = taggedObjectContext(this)

  }
  object TaggedFromTo {
    class Leaf[T](c: ClassTag[_], override val tagName: String, tag: String, r: CaseW[T] with To[T])
        extends TaggedFromTo[T] {
      def findTo(s: String) = if (s == tag) r else null
      def findFrom(v: Any) = {
        if (c.runtimeClass.isInstance(v)) (tag -> r)
        else null
      }
    }
    class Node[T](rs: TaggedFromTo[_ <: T]*) extends TaggedFromTo[T] {
      override val tagName: String = findTagName(rs)
      def findTo(s: String) = scanChildren(rs)(_.findTo(s)).asInstanceOf[To[T]]
      def findFrom(v: Any) = scanChildren(rs)(_.findFrom(v)).asInstanceOf[(String, CaseW[T])]
    }
  }

}
