package com.rallyhealth.weepickle.v1

import com.rallyhealth.weejson.v1.BufferedValue
import com.rallyhealth.weepickle.v1.core._

import scala.language.experimental.macros
import scala.reflect.ClassTag

/**
  * An instance of the com.rallyhealth.weepickle.v1 API. There's a default instance at
  * `com.rallyhealth.weepickle.v1.WeePickle`, but you can also implement it yourself to customize
  * its behavior. Override the `annotate` methods to control how a sealed
  * trait instance is tagged during reading and writing.
  */
trait Api extends implicits.Tos with implicits.Froms with Api.NoOpMappers with FromToValue {
  this: Types with Annotator =>

  /**
    * Somewhat internal version of [[WeePickle.ToScala]] for use by custom API bundles.
    */
  def toScala[Out](implicit to: To[Out]): Visitor[_, Out] = to

  /**
    * Somewhat internal version of [[WeePickle.FromScala]] for use by custom API bundles.
    */
  def fromScala[In](scala: In)(implicit from: From[In]): FromInput = new FromInput {
    override def transform[T](to: Visitor[_, T]): T = from.transform(scala, to)
  }

  /**
    * Summons a Visitor that produces output value(s) of type T.
    */
  def to[T: To]: To[T] = implicitly[To[T]]

  /**
    * Summons a type class that can write from data into visitor.
    */
  def from[T: From]: From[T] = implicitly[From[T]]

  def fromTo[T: FromTo]: FromTo[T] = implicitly[FromTo[T]]

  // End Api
}
object Api {
  trait NoOpMappers {

    /**
      * Transforms object keys.
      * e.g. {"food_group": "vegetable"} => {"FoodGroup": "vegetable"}
      *
      * @see https://com-lihaoyi.github.io/upickle/#CustomConfiguration
      */
    def objectAttributeKeyReadMap(s: CharSequence): CharSequence = s
    def objectAttributeKeyWriteMap(s: CharSequence): CharSequence = s

    /**
      * Transforms sealed trait/class \$type discriminator values.
      * e.g. {{{
      *    {"\$type": "com.rallyhealth.Bee"} => {"\$type": "com-rallyhealth-bee"}
      * }}}
      *
      * @see https://com-lihaoyi.github.io/upickle/#CustomConfiguration
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
trait AttributeTagged extends Api with Annotator {

  /**
    * Default discriminator field name.
    * Overridable here globally, or for a specific class hierarchy using the
    * `discriminator` annotation.
    */
  def tagName: String = "$type"

  def annotate[V](rw: CaseR[V], tagName: String, tag: String) = {
    new TaggedTo.Leaf[V](tagName, tag, rw)
  }

  def annotate[V](rw: CaseW[V], tagName: String, tag: String)(implicit c: ClassTag[V]) = {
    new TaggedFrom.Leaf[V](c, tagName, tag, rw)
  }

  def taggedExpectedMsg = "expected dictionary"
  override def taggedObjectContext[T](taggedTo: TaggedTo[T]): ObjVisitor[Any, T] = {
    new ObjVisitor[Any, T] {
      private[this] var fastPath = false
      private[this] var context: ObjVisitor[Any, _] = null
      def subVisitor: Visitor[_, _] =
        if (context == null) com.rallyhealth.weepickle.v1.core.StringVisitor
        else context.subVisitor

      def visitKey(): Visitor[_, _] = {
        if (context != null) context.visitKey()
        else com.rallyhealth.weepickle.v1.core.StringVisitor
      }
      def visitKeyValue(s: Any): Unit = {
        if (context != null) context.visitKeyValue(s)
        else {
          if (s.toString == taggedTo.tagName) () //do nothing
          else {
            // otherwise, go slow path
            val slowCtx = BufferedValue.Builder.visitObject(-1).narrow
            val keyVisitor = slowCtx.visitKey()
            val xxx = keyVisitor.visitString(s.toString)
            slowCtx.visitKeyValue(xxx)
            context = slowCtx
          }
        }
      }

      def visitValue(v: Any): Unit = {
        if (context != null) context.visitValue(v)
        else {
          val typeName = objectTypeKeyReadMap(v.toString).toString
          val facade0 = taggedTo.findTo(typeName)
          if (facade0 == null) {
            throw new Abort("invalid tag for tagged object: " + typeName)
          }
          val fastCtx = facade0.visitObject(-1)
          context = fastCtx
          fastPath = true
        }
      }
      def visitEnd(): T = {
        if (context == null) throw new Abort("expected tagged dictionary")
        else if (fastPath) context.visitEnd().asInstanceOf[T]
        else {
          val x = context.visitEnd().asInstanceOf[BufferedValue.Obj]
          val tagInfo = x.value0
            .find(_._1.toString == taggedTo.tagName)
            .getOrElse(throw new Abort(s"missing tag key: ${taggedTo.tagName}"))
          val keyAttr = tagInfo._2
          val key = keyAttr.asInstanceOf[BufferedValue.Str].value0.toString
          val reader = taggedTo.findTo(key)
          if (reader == null) {
            throw new Abort("invalid tag for tagged object: " + key)
          }
          // Replaying buffered content requires new path tracking for exceptions thrown by the reader.
          val delegate = JsonPointerVisitor(reader)
          val ctx2 = delegate.visitObject(-1)
          for (p <- x.value0) {
            val (k0, v) = p
            val k = k0.toString
            if (k != taggedTo.tagName) {
              val keyVisitor = ctx2.visitKey()

              ctx2.visitKeyValue(keyVisitor.visitString(k))
              ctx2.visitValue(BufferedValue.transform(v, ctx2.subVisitor))
            }
          }
          ctx2.visitEnd()
        }
      }

    }
  }
  def taggedWrite[T, R](w: CaseW[T], tagName: String, tag: String, out: Visitor[_, R], v: T): R = {
    val ctx = out.asInstanceOf[Visitor[Any, R]].visitObject(w.length(v) + 1)
    val keyVisitor = ctx.visitKey()

    ctx.visitKeyValue(keyVisitor.visitString(tagName))
    ctx.visitValue(ctx.subVisitor.visitString(objectTypeKeyWriteMap(tag)))
    w.writeToObject(ctx, v)
    val res = ctx.visitEnd()
    res
  }
}
