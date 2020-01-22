package com.rallyhealth.weepickle.v1

import com.rallyhealth.weejson.v1.IndexedValue
import com.rallyhealth.weepickle.v1.core._

import scala.language.experimental.macros
import scala.language.higherKinds
import scala.reflect.ClassTag

/**
 * An instance of the com.rallyhealth.weepickle.v1 API. There's a default instance at
 * `com.rallyhealth.weepickle.v1.WeePickle`, but you can also implement it yourself to customize
 * its behavior. Override the `annotate` methods to control how a sealed
 * trait instance is tagged during reading and writing.
 */
trait Api
    extends com.rallyhealth.weepickle.v1.core.Types
    with implicits.Readers
    with implicits.Writers
    with WebJson
    with Api.NoOpMappers
    with JsReaderWriters
    with MsgReaderWriters{

  /**
    * Somewhat internal version of [[WeePickle.ToScala]] for use by custom API bundles.
    */
  def toScala[Out](implicit pickleOut: Reader[Out]): Visitor[_, Out] = pickleOut

  /**
    * Somewhat internal version of [[WeePickle.FromScala]] for use by custom API bundles.
    */
  def fromScala[In](scala: In)(implicit pickleIn: Writer[In]): Transformable = new Transformable {
    override def transform[T](into: Visitor[_, T]): T = pickleIn.transform(scala, into)
  }


  def reader[T: Reader]: Reader[T] = implicitly[Reader[T]]

  def writer[T: Writer]: Writer[T] = implicitly[Writer[T]]

  def readerWriter[T: ReaderWriter]: ReaderWriter[T] = implicitly[ReaderWriter[T]]

  // End Api
}
object Api{
  trait NoOpMappers{

    /**
      * Transforms object keys.
      * e.g. {"food_group": "vegetable"} => {"FoodGroup": "vegetable"}
      *
      * @see http://www.lihaoyi.com/upickle/#CustomConfiguration
      */
    def objectAttributeKeyReadMap(s: CharSequence): CharSequence = s
    def objectAttributeKeyWriteMap(s: CharSequence): CharSequence = s

    /**
      * Transforms sealed trait/class $type discriminator values.
      * e.g. {{{
      *    {"$type": "com.rallyhealth.Bee"} => {"$type": "com-rallyhealth-bee"}
      * }}}
      *
      * @see http://www.lihaoyi.com/upickle/#CustomConfiguration
      */
    def objectTypeKeyReadMap(s: CharSequence): CharSequence = s
    def objectTypeKeyWriteMap(s: CharSequence): CharSequence = s
  }

}

/**
 * A `com.rallyhealth.weepickle.v1.Api` that follows the default sealed-trait-instance-tagging
 * behavior of using an attribute, but allow you to control what the name
 * of the attribute is.
 */
trait AttributeTagged extends Api{

  /**
    * Default discriminator field name.
    * Overridable here globally, or for a specific class hierarcy using the
    * [[com.rallyhealth.weepickle.v1.implicits.discriminator]] annotation.
    */
  def tagName: String = "$type"

  def annotate[V](rw: CaseR[V], tagName: String, tag: String) = {
    new TaggedReader.Leaf[V](tagName, tag, rw)
  }

  def annotate[V](rw: CaseW[V], tagName: String, tag: String)(implicit c: ClassTag[V]) = {
    new TaggedWriter.Leaf[V](c, tagName, tag, rw)
  }

  def taggedExpectedMsg = "expected dictionary"
  override def taggedObjectContext[T](taggedReader: TaggedReader[T], index: Int): ObjVisitor[Any, T] = {
    new ObjVisitor[Any, T]{
      private[this] var fastPath = false
      private[this] var context: ObjVisitor[Any, _] = null
      def subVisitor: Visitor[_, _] =
        if (context == null) com.rallyhealth.weepickle.v1.core.StringVisitor
        else context.subVisitor

      def visitKey(index: Int) = {
        if (context != null) context.visitKey(index)
        else com.rallyhealth.weepickle.v1.core.StringVisitor
      }
      def visitKeyValue(s: Any): Unit = {
        if (context != null) context.visitKeyValue(s)
        else {
          if (s.toString == taggedReader.tagName) () //do nothing
          else {
            // otherwise, go slow path
            val slowCtx = IndexedValue.Builder.visitObject(-1, index).narrow
            val keyVisitor = slowCtx.visitKey(index)
            val xxx = keyVisitor.visitString(s.toString, index)
            slowCtx.visitKeyValue(xxx)
            context = slowCtx
          }
        }
      }

      def visitValue(v: Any, index: Int): Unit = {
        if (context != null) context.visitValue(v, index)
        else {
          val typeName = objectTypeKeyReadMap(v.toString).toString
          val facade0 = taggedReader.findReader(typeName)
          if (facade0 == null) {
            throw new Abort("invalid tag for tagged object: " + typeName)
          }
          val fastCtx = facade0.visitObject(-1, index)
          context = fastCtx
          fastPath = true
        }
      }
      def visitEnd(index: Int) = {
        if (context == null) throw new Abort("expected tagged dictionary")
        else if (fastPath) context.visitEnd(index).asInstanceOf[T]
        else{
          val x = context.visitEnd(index).asInstanceOf[IndexedValue.Obj]
          val tagInfo = x.value0.find(_._1.toString == taggedReader.tagName).getOrElse(throw Abort(s"missing tag key: ${taggedReader.tagName}"))
          val keyAttr = tagInfo._2
          val key = keyAttr.asInstanceOf[IndexedValue.Str].value0.toString
          val delegate = taggedReader.findReader(key)
          if (delegate == null){
            throw new AbortException("invalid tag for tagged object: " + key, keyAttr.index, -1, -1, null)
          }
          val ctx2 = delegate.visitObject(-1, -1)
          for (p <- x.value0) {
            val (k0, v) = p
            val k = k0.toString
            if (k != taggedReader.tagName){
              val keyVisitor = ctx2.visitKey(-1)

              ctx2.visitKeyValue(keyVisitor.visitString(k, -1))
              ctx2.visitValue(IndexedValue.transform(v, ctx2.subVisitor), -1)
            }
          }
          ctx2.visitEnd(index)
        }
      }

    }
  }
  def taggedWrite[T, R](w: CaseW[T], tagName: String, tag: String, out: Visitor[_,  R], v: T): R = {
    val ctx = out.asInstanceOf[Visitor[Any, R]].visitObject(w.length(v) + 1, -1)
    val keyVisitor = ctx.visitKey(-1)

    ctx.visitKeyValue(keyVisitor.visitString(tagName, -1))
    ctx.visitValue(ctx.subVisitor.visitString(objectTypeKeyWriteMap(tag), -1), -1)
    w.writeToObject(ctx, v)
    val res = ctx.visitEnd(-1)
    res
  }
}
