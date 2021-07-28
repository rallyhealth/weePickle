package com.rallyhealth.weepickle.v1.implicits

import compiletime.{summonInline}
import deriving.Mirror
import scala.reflect.ClassTag
import com.rallyhealth.weepickle.v1.core.{Annotator, ObjVisitor, Types, Visitor}

trait CaseClassFromPiece extends MacrosCommon:
  this: Types with Annotator =>

  class CaseClassFrom[V](
    // parallel arrays in field definition order
    fieldNames: Array[String],
    defaultValues: Array[Option[AnyRef]],
    froms: Array[From[_]],
    dropDefaults: Array[Boolean],
    dropAllDefaults: Boolean
  ) extends CaseW[V]:

    def length(v: V): Int =
      if mightDropDefaults then
        var sum = 0
        val product = v.asInstanceOf[Product]
        var i = 0
        val arity = product.productArity
        while (i < arity) do
          val value = product.productElement(i)
          val writer = froms(i)
          if shouldWriteValue(value, i) then sum += 1
          i += 1
        sum
      else
        // fast path
        froms.length
    end length

    def writeToObject[R](ctx: ObjVisitor[_, R], v: V): Unit =
      val product = v.asInstanceOf[Product]
      var i = 0
      val arity = product.productArity
      while (i < arity) do
        val value = product.productElement(i)
        val from = froms(i)
        val fieldName = fieldNames(i)
        if shouldWriteValue(value, i) then
          val keyVisitor = ctx.visitKey()
          ctx.visitKeyValue(
            keyVisitor.visitString(
              objectAttributeKeyWriteMap(fieldName)
            )
          )
          ctx.narrow.visitValue(from.narrow.transform(value, ctx.subVisitor))
        end if
        i += 1
      end while
    end writeToObject

    /**
     * Optimization to allow short-circuiting length checks.
     */
    private val mightDropDefaults = !serializeDefaults && (dropAllDefaults || dropDefaults.exists(_ == true)) && defaultValues.exists(_.isDefined)

    private def shouldWriteValue(value: Any, i: Int): Boolean = serializeDefaults || !(dropAllDefaults || dropDefaults(i)) || !defaultValues(i).contains(value)

  end CaseClassFrom

  inline def macroFrom[T: ClassTag](using m: Mirror.Of[T]): From[T] = inline m match {
    case m: Mirror.ProductOf[T] =>
      val (fullClassName, dropAllDefaults) = macros.fullClassName[T]

      // parallel arrays in field definition order
      val labels: List[(String, Boolean)] = macros.fieldLabels[T]
      val fieldNames = labels.map(_._1).toArray
      val dropDefaults = labels.map(_._2).toArray
      val defaultValues = fieldNames.map(macros.getDefaultParams[T].get)
      val froms: List[From[_]] =
        macros.summonList[Tuple.Map[m.MirroredElemTypes, From]].asInstanceOf[List[From[_]]]

      val fromCaseClass = CaseClassFrom[T](
        fieldNames,
        defaultValues,
        froms.toArray,
        dropDefaults,
        dropAllDefaults,
      )

      val (isSealed, discriminator) = macros.isMemberOfSealedHierarchy[T]
      if isSealed then annotate(fromCaseClass, discriminator.getOrElse("$type"), fullClassName)
      else fromCaseClass
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
