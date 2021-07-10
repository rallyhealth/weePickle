package com.rallyhealth.weepickle.v1.implicits

import compiletime.{summonInline}
import deriving.Mirror
import com.rallyhealth.weepickle.v1.core.{ Visitor, ObjVisitor, Annotator }

trait CaseClassToPiece extends MacrosCommon:
  this: com.rallyhealth.weepickle.v1.core.Types with Tos with Annotator =>
  trait CaseClassTo[T] extends CaseR[T]:
    def make(bldr: Map[String, Any]): T

    def visitorForKey(currentKey: String): Visitor[_, _]

    private val builder = collection.mutable.Map.empty[String, Any]

    override def visitObject(length: Int) = new ObjVisitor[Any, T] {
      var currentKey: String = null

      def subVisitor: Visitor[_, _] = visitorForKey(currentKey)

      def visitKey(): Visitor[_, _] = ToString

      def visitKeyValue(v: Any): Unit =
        currentKey = objectAttributeKeyReadMap(v.asInstanceOf[String]).toString

      def visitValue(v: Any): Unit =
        builder(currentKey) = v

      def visitEnd(): T =
        make(builder.toMap)
    }
  end CaseClassTo

  inline def macroTo[T](using m: Mirror.Of[T]): To[T] = inline m match {
    case m: Mirror.ProductOf[T] =>
      val labels: List[String] = macros.fieldLabels[T]
      val visitors: List[Visitor[_, _]] =
        macros.summonList[Tuple.Map[m.MirroredElemTypes, To]]
          .asInstanceOf[List[com.rallyhealth.weepickle.v1.core.Visitor[_, _]]]
      val defaultParams: Map[String, AnyRef] = macros.getDefaultParams[T]

      val reader = new CaseClassTo[T] {
        override def visitorForKey(key: String) =
          labels.zip(visitors).toMap.get(key) match {
            case None => com.rallyhealth.weepickle.v1.core.NoOpVisitor
            case Some(v) => v
          }

        override def make(params: Map[String, Any]): T =
          val values = collection.mutable.ListBuffer.empty[AnyRef]
          val missingKeys = collection.mutable.ListBuffer.empty[String]

          labels.zip(visitors).map { case (fieldName, _) =>
            params.get(fieldName) match {
              case Some(value) => values += value.asInstanceOf[AnyRef]
              case None =>
                defaultParams.get(fieldName) match {
                  case Some(fallback) => values += fallback.asInstanceOf[AnyRef]
                  case None => missingKeys += fieldName
                }
            }
          }

          if (!missingKeys.isEmpty) {
            throw new com.rallyhealth.weepickle.v1.core.Abort("missing keys in dictionary: " + missingKeys.mkString(", "))
          }

          val valuesArray = values.toArray
          m.fromProduct(new Product {
            def canEqual(that: Any): Boolean = true
            def productArity: Int = valuesArray.length
            def productElement(i: Int): Any = valuesArray(i)
          })
        end make
      }

      //TODO adapt new code to the WeePickle way of annotating (with a tag name)
      if macros.isMemberOfSealedHierarchy[T] then annotate(reader, "$type", macros.fullClassName[T])
      else reader

    case m: Mirror.SumOf[T] =>
      val readers: List[To[_ <: T]] = macros.summonList[Tuple.Map[m.MirroredElemTypes, To]]
        .asInstanceOf[List[To[_ <: T]]]
      To.merge[T](readers:_*)
  }

  inline given [T <: Singleton: Mirror.Of]: To[T] = macroTo[T]

  // see comment in MacroImplicits as to why Dotty's extension methods aren't used here
  implicit class ToExtension(r: To.type):
    inline def derived[T](using Mirror.Of[T]): To[T] = macroTo[T]
  end ToExtension

end CaseClassToPiece
