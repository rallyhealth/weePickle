package com.rallyhealth.weepickle.v1.implicits

import compiletime.{summonInline}
import deriving.Mirror
import scala.reflect.ClassTag
import com.rallyhealth.weepickle.v1.core.{Annotator, ObjVisitor, Types, Visitor}

trait CaseClassFromPiece extends MacrosCommon:
  this: Types with Annotator =>
  class CaseClassFrom[V](
    elemsInfo: V => List[(String, Boolean, From[_], Option[AnyRef], Any)]
  ) extends CaseW[V]:

    def length(v: V): Int =
      var n = 0
      for
        (_, dropDefault, _, defaultValue, value) <- elemsInfo(v)
        if !dropDefault || serializeDefaults || !defaultValue.find(_ == value)
      do
        n += 1
      n
    end length

    def writeToObject[R](ctx: ObjVisitor[_, R], v: V): Unit =
      for
        (name, dropDefault, writer, defaultValue, value) <- elemsInfo(v)
        if !dropDefault || serializeDefaults || !defaultValue.map(_ == value)
      do
        val keyVisitor = ctx.visitKey()
        ctx.visitKeyValue(
          keyVisitor.visitString(
            objectAttributeKeyWriteMap(name)
          )
        )
        ctx.narrow.visitValue(
          writer.narrow.transform(value, ctx.subVisitor))
    end writeToObject
  end CaseClassFrom

  inline def macroFrom[T: ClassTag](using m: Mirror.Of[T]): From[T] = inline m match {
    case m: Mirror.ProductOf[T] =>
      val (fullClassName, dropAllDefaults) = macros.fullClassName[T]
      val labels: List[(String, Boolean)] = macros.fieldLabels[T]
      val writers: List[From[_]] =
        macros.summonList[Tuple.Map[m.MirroredElemTypes, From]]
          .asInstanceOf[List[From[_]]]
      val defaults = macros.getDefaultParams[T]
      val staticElemsInfo: List[(String, Boolean, From[_], Option[AnyRef])] = for ((l, dd), w) <- labels.zip(writers)
        yield (l, dd || dropAllDefaults, w, defaults.get(l))

      def elemsInfo(v: T): List[(String, Boolean, From[_], Option[AnyRef], Any)] =
        for ((l, dd, w, d), v) <- staticElemsInfo.zip(v.asInstanceOf[Product].productIterator)
        yield (l, dd, w, d, v)
      end elemsInfo
      val writer = CaseClassFrom[T](elemsInfo)

      val (isSealed, discriminator) = macros.isMemberOfSealedHierarchy[T]
      if isSealed then annotate(writer, discriminator.getOrElse("$type"), fullClassName)
      else writer
    case m: Mirror.SumOf[T] =>
      val writers: List[From[_ <: T]] = macros.summonList[Tuple.Map[m.MirroredElemTypes, From]]
        .asInstanceOf[List[From[_ <: T]]]
      From.merge[T](writers:_*)
  }

  inline given [T <: Singleton: Mirror.Of: ClassTag]: From[T] = macroFrom[T]

  // see comment in MacroImplicits as to why Dotty's extension methods aren't used here
  implicit class FromExtension(r: From.type):
    inline def derived[T](using Mirror.Of[T], ClassTag[T]): From[T] = macroFrom[T]
  end FromExtension

end CaseClassFromPiece
