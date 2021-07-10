package com.rallyhealth.weepickle.v1.implicits

import compiletime.{summonInline}
import deriving.Mirror
import scala.reflect.ClassTag
import com.rallyhealth.weepickle.v1.core.{ Visitor, ObjVisitor, Annotator }

trait CaseClassFromPiece extends MacrosCommon:
  this: com.rallyhealth.weepickle.v1.core.Types with Froms with Annotator =>
  class CaseClassFrom[V](
    elemsInfo: V => List[(String, From[_], Any)],
    defaultParams: Map[String, AnyRef]) extends CaseW[V]:
    def length(v: V): Int =
      var n = 0
      for
        (name, _, value) <- elemsInfo(v)
        defaultValue <- defaultParams.get(name)
        if defaultValue != value || serializeDefaults
      do n += 1
      n
    end length

    def writeToObject[R](ctx: ObjVisitor[_, R], v: V): Unit =
      for
        (name, writer, value) <- elemsInfo(v)
        defaultValue = defaultParams.get(name)
        if serializeDefaults || defaultValue.isEmpty || defaultValue.get != value
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
      def elemsInfo(v: T): List[(String, From[_], Any)] =
        val labels: List[String] = macros.fieldLabels[T]
        val writers: List[From[_]] =
          macros.summonList[Tuple.Map[m.MirroredElemTypes, From]]
            .asInstanceOf[List[From[_]]]
        val values: List[Any] = v.asInstanceOf[Product].productIterator.toList
        for ((l, w), v) <- labels.zip(writers).zip(values)
        yield (l, w, v)
      end elemsInfo
      val writer = CaseClassFrom[T](elemsInfo, macros.getDefaultParams[T])

      //TODO adapt new code to the WeePickle way of annotating (with a tag name)
      if macros.isMemberOfSealedHierarchy[T] then annotate(writer, "$type", macros.fullClassName[T])
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
