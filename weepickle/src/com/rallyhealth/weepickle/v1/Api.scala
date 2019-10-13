package com.rallyhealth.weepickle.v0

import java.io.ByteArrayOutputStream

import language.experimental.macros
import language.higherKinds
import com.rallyhealth.weepickle.v0.core._
import scala.reflect.ClassTag
import com.rallyhealth.weejson.v0.IndexedValue
/**
 * An instance of the com.rallyhealth.weepickle.v0 API. There's a default instance at
 * `com.rallyhealth.weepickle.v0.default`, but you can also implement it yourself to customize
 * its behavior. Override the `annotate` methods to control how a sealed
 * trait instance is tagged during reading and writing.
 */
trait Api
    extends com.rallyhealth.weepickle.v0.core.Types
    with implicits.Readers
    with implicits.Writers
    with WebJson
    with Api.NoOpMappers
    with JsReadWriters
    with MsgReadWriters{
  /**
    * Reads the given MessagePack input into a Scala value
    */
  def readMsgPack[T: Reader](s: com.rallyhealth.weepack.v0.Readable): T = s.transform(reader[T])

  /**
    * Reads the given JSON input into a Scala value
    */
  def read[T: Reader](s: com.rallyhealth.weejson.v0.Readable): T = s.transform(reader[T])

  def reader[T: Reader]: Reader[T] = implicitly[Reader[T]]

  /**
    * Write the given Scala value as a JSON string
    */
  def write[T: Writer](t: T,
                       indent: Int = -1,
                       escapeUnicode: Boolean = false): String = {
    transform(t).to(com.rallyhealth.weejson.v0.StringRenderer(indent, escapeUnicode)).toString
  }
  /**
    * Write the given Scala value as a MessagePack binary
    */
  def writeMsgPack[T: Writer](t: T): Array[Byte] = {
    transform(t).to(new com.rallyhealth.weepack.v0.MsgPackWriter(new ByteArrayOutputStream())).toByteArray
  }

  /**
    * Write the given Scala value as a JSON struct
    */
  def writeJs[T: Writer](t: T): com.rallyhealth.weejson.v0.Value = transform(t).to[com.rallyhealth.weejson.v0.Value]

  /**
    * Write the given Scala value as a MessagePack struct
    */
  def writeMsgAst[T: Writer](t: T): com.rallyhealth.weepack.v0.Msg = transform(t).to[com.rallyhealth.weepack.v0.Msg]

  /**
    * Write the given Scala value as a JSON string to the given Writer
    */
  def writeTo[T: Writer](t: T,
                         out: java.io.Writer,
                         indent: Int = -1,
                         escapeUnicode: Boolean = false): Unit = {
    transform(t).to(new com.rallyhealth.weejson.v0.Renderer(out, indent = indent, escapeUnicode))
  }
  /**
    * Write the given Scala value as a MessagePack binary to the given OutputStream
    */
  def writeMsgPackTo[T: Writer](t: T, out: java.io.OutputStream): Unit = {
    transform(t).to(new com.rallyhealth.weepack.v0.MsgPackWriter(out))
  }

  def writer[T: Writer]: Writer[T] = implicitly[Writer[T]]

  def readwriter[T: ReadWriter]: ReadWriter[T] = implicitly[ReadWriter[T]]

  case class transform[T: Writer](t: T) extends com.rallyhealth.weepack.v0.Readable with com.rallyhealth.weejson.v0.Readable {
    def transform[V](f: Visitor[_, V]): V = writer[T].transform(t, f)
    def to[V](f: Visitor[_, V]): V = transform(f)
    def to[V](implicit f: Reader[V]): V = transform(f)
  }
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
      * e.g. {"$type": "com.rallyhealth.Bee"} => {"$type": "com-rallyhealth-bee"}
      *
      * @see http://www.lihaoyi.com/upickle/#CustomConfiguration
      */
    def objectTypeKeyReadMap(s: CharSequence): CharSequence = s
    def objectTypeKeyWriteMap(s: CharSequence): CharSequence = s
  }

}
/**
 * The default way of accessing com.rallyhealth.weepickle.v0
 */
object default extends AttributeTagged{

}

/**
 * A `com.rallyhealth.weepickle.v0.Api` that follows the default sealed-trait-instance-tagging
 * behavior of using an attribute, but allow you to control what the name
 * of the attribute is.
 */
trait AttributeTagged extends Api{

  /**
    * Default discriminator field name.
    * Overridable here globally, or for a specific class hierarcy using the
    * [[com.rallyhealth.weepickle.v0.implicits.discriminator]] annotation.
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
        if (context == null) com.rallyhealth.weepickle.v0.core.StringVisitor
        else context.subVisitor

      def visitKey(index: Int) = {
        if (context != null) context.visitKey(index)
        else com.rallyhealth.weepickle.v0.core.StringVisitor
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
            throw new AbortException("invalid tag for tagged object: " + key, keyAttr.index, -1, -1, Nil, null)
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
    ctx.visitValue(out.visitString(objectTypeKeyWriteMap(tag), -1), -1)
    w.writeToObject(ctx, v)
    val res = ctx.visitEnd(-1)
    res
  }
}
