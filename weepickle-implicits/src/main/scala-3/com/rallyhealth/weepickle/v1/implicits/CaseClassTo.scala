package com.rallyhealth.weepickle.v1.implicits

import compiletime.{summonInline}
import deriving.Mirror
import scala.util.control.NonFatal
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
      val labels: List[String] = macros.fieldLabels[T].map(_._1) // we don't need dropDefault in To
      val visitors: List[Visitor[_, _]] =
        macros.summonList[Tuple.Map[m.MirroredElemTypes, To]]
          .asInstanceOf[List[com.rallyhealth.weepickle.v1.core.Visitor[_, _]]]
      val defaultParams: Map[String, AnyRef] = macros.getDefaultParams[T]
      val labelToVisitor = labels.zip(visitors).toMap // cache
      val indexedLabels = labels.zipWithIndex // cache
      val valueCount = labels.size // cache

      val reader = new CaseClassTo[T] {
        override def visitorForKey(key: String) =
          labelToVisitor.get(key) match {
            case None =>
              // println(s"WARNING: CaseClassTo.visitorForKey($key): not found, returning NoOpVisitor")
              com.rallyhealth.weepickle.v1.core.NoOpVisitor
            case Some(v) =>
              // println(s"CaseClassTo.visitorForKey($key): found, returning $v")
              v
          }

        override def make(params: Map[String, Any]): T =
          // println(s"in make: params=$params, labels=$labels")
          val valuesArray = new Array[AnyRef](valueCount)
          val missingKeys = collection.mutable.ListBuffer.empty[String]

          indexedLabels.map { case (fieldName, index) =>
            params.get(fieldName) match {
              case Some(value) => valuesArray(index) = value.asInstanceOf[AnyRef]
              case None =>
                defaultParams.get(fieldName) match {
                  case Some(fallback) => valuesArray(index) = fallback.asInstanceOf[AnyRef]
                  case None => missingKeys += fieldName
                }
            }
          }

          if (!missingKeys.isEmpty) {
            throw new com.rallyhealth.weepickle.v1.core.Abort("missing keys in dictionary: " + missingKeys.mkString(", "))
          }

          m.fromProduct(new Product {
            def canEqual(that: Any): Boolean = true
            def productArity: Int = valueCount
            def productElement(i: Int): Any = valuesArray(i)
          })
        end make
      }

      val (isSealed, discriminator) = macros.isMemberOfSealedHierarchy[T]
      if isSealed then annotate(reader, discriminator.getOrElse("$type"), macros.fullClassName[T]._1)
      else reader

    case m: Mirror.SumOf[T] =>
      val readers: List[To[_ <: T]] = macros.summonList[Tuple.Map[m.MirroredElemTypes, To]]
        .asInstanceOf[List[To[_ <: T]]]
      To.merge[T](readers:_*)
  }

  /*
   * Forces a default for missing "container" types, i.e., a Seq, List, Array, or Map.
   *
   * If a field is missing for one of these "container" types, then call visitNull
   * so the correct null attribute will be set given the type, e.g., Nil for a Seq.
   *
   * This is subtly different than actually assuming a default value. Default values
   * affect both reading (to) and writing (from), e.g., the application of dropDefault
   * as part of writing. We assume None as the default for Option types that don't have
   * and explicit default defined, which are dropped as part of dropDefault writing.
   * But these nullable container types are only considered as part of reading, so
   * writing one, even with dropDefault defined, will still result in a value written.
   * The rationale for this difference in treatment is that it may be surprising to
   * have the output of an empty sequence suppressed as part of writing (e.g.,
   * serializing to a JSON string) where a empty sequence was not explicitly defined
   * as the default to be suppressed.
   *
   * As a first attempt, side-steped the need to differentiate container from non-container
   * types by just always calling visitNull on missing fields. At first it seemed like it
   * would be a good PoC, but then... it started feeling more like the right thing to do
   * for the long term. That way, anything that does something useful in visitNull (including
   * any custom To) would be invoked, which seems to be more in keeping with the name "Nullable".
   * Only the following default implicit Tos implement visitNull: ToMap, ToImmutableMap, and
   * ToMutableMap (all via MapTo0), as well as ToArray, ToSeqLike, and ToOption. However
   * the "Nullable" actions for ToOption are really accomidated by having a None default by
   * default, so only the other default Tos (as well as anything custom) should be leveraged
   * by this "Nullable" logic.
   *
   * TODO: Consolidate duplicate code here and in macroTo (right now it is 90% cut and paste).
   * (And maybe write a shorter, less stream-of-consciousness comment.)
   */
  inline def macroNullableTo[T](using m: Mirror.Of[T]): To[T] = inline m match {
    case m: Mirror.ProductOf[T] =>
      val labels: List[String] = macros.fieldLabels[T].map(_._1) // we don't need dropDefault in To
      val visitors: List[Visitor[_, _]] =
        macros.summonList[Tuple.Map[m.MirroredElemTypes, To]]
          .asInstanceOf[List[com.rallyhealth.weepickle.v1.core.Visitor[_, _]]]
      val defaultParams: Map[String, AnyRef] = macros.getDefaultParams[T]
      val labelToVisitor = labels.zip(visitors).toMap // cache
      val indexedLabels = labels.zipWithIndex // cache
      val valueCount = labels.size // cache

      val reader = new CaseClassTo[T] {
        override def visitorForKey(key: String) =
          labelToVisitor.get(key) match {
            case None =>
              // println(s"WARNING: CaseClassTo.visitorForKey($key): not found, returning NoOpVisitor")
              com.rallyhealth.weepickle.v1.core.NoOpVisitor
            case Some(v) =>
              // println(s"CaseClassTo.visitorForKey($key): found, returning $v")
              v
          }

        override def make(params: Map[String, Any]): T =
          // println(s"in make: params=$params, labels=$labels")
          val valuesArray = new Array[AnyRef](valueCount)
          val missingKeys = collection.mutable.ListBuffer.empty[String]

          indexedLabels.map { case (fieldName, index) =>
            params.get(fieldName) match {
              case Some(value) => valuesArray(index) = value.asInstanceOf[AnyRef]
              case None =>
                defaultParams.get(fieldName) match {
                  case Some(fallback) => valuesArray(index) = fallback.asInstanceOf[AnyRef]
                  case None =>
                    labelToVisitor.get(fieldName) match {
                      case None =>
                        // shouldn't happen - treat as missing
                        missingKeys += fieldName
                      case Some(v) =>
                        try {
                          valuesArray(index) = v.visitNull().asInstanceOf[AnyRef]
                        } catch {
                          case NonFatal(_) => missingKeys += fieldName
                        }
                    }
                }
            }
          }

          if (!missingKeys.isEmpty) {
            throw new com.rallyhealth.weepickle.v1.core.Abort("missing keys in dictionary: " + missingKeys.mkString(", "))
          }

          m.fromProduct(new Product {
            def canEqual(that: Any): Boolean = true
            def productArity: Int = valueCount
            def productElement(i: Int): Any = valuesArray(i)
          })
        end make
      }

      val (isSealed, discriminator) = macros.isMemberOfSealedHierarchy[T]
      if isSealed then annotate(reader, discriminator.getOrElse("$type"), macros.fullClassName[T]._1)
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
