package com.rallyhealth.weejson.v1

import com.rallyhealth.weejson.v1.jackson.FromJson
import com.rallyhealth.weepickle.v1.core._

import scala.collection.compat._

trait AstTransformer[I] extends Transformer[I] with JsVisitor[I, I] {
  def apply(s: String): I = FromJson(s).transform(this)

  def transformArray[T](f: Visitor[_, T], items: Iterable[I]): T = {
    val ctx = f.visitArray(items.size).narrow
    for (item <- items) ctx.visitValue(transform(item, ctx.subVisitor))
    ctx.visitEnd()
  }
  def transformObject[T](f: Visitor[_, T], items: Iterable[(String, I)]): T = {
    val ctx = f.visitObject(items.size).narrow
    for (kv <- items) {
      val keyVisitor = ctx.visitKey()
      ctx.visitKeyValue(keyVisitor.visitString(kv._1))
      ctx.visitValue(transform(kv._2, ctx.subVisitor))
    }
    ctx.visitEnd()
  }

  class AstObjVisitor[T](build: T => I)(implicit factory: Factory[(String, I), T]) extends ObjVisitor[I, I] {

    private[this] var key: String = null
    private[this] val vs = factory.newBuilder
    override def subVisitor: Visitor[_, _] = AstTransformer.this
    override def visitKey(): Visitor[_, _] = com.rallyhealth.weepickle.v1.core.StringVisitor
    override def visitKeyValue(s: Any): Unit = key = s.toString

    override def visitValue(v: I): Unit = vs += (key -> v)

    override def visitEnd(): I = build(vs.result)
  }
  class AstArrVisitor[T[_]](build: T[I] => I)(implicit factory: Factory[I, T[I]]) extends ArrVisitor[I, I] {
    override def subVisitor: Visitor[_, _] = AstTransformer.this
    private[this] val vs = factory.newBuilder
    override def visitValue(v: I): Unit = vs += v

    override def visitEnd(): I = build(vs.result())
  }
}
