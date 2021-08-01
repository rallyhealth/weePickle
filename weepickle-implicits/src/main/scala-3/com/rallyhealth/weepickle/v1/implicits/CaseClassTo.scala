package com.rallyhealth.weepickle.v1.implicits

import compiletime.{summonInline}
import deriving.Mirror
import scala.util.control.NonFatal
import com.rallyhealth.weepickle.v1.core.{Abort, Annotator, ObjVisitor, NoOpVisitor, Types, Visitor}

trait CaseClassToPiece extends MacrosCommon :
  this: Types with Annotator =>

  // must duplicate ToString definition here to avoid circular reference with DefaultTos
  implicit val stringVisitor: To[String] = new SimpleTo[String] {
    override def expectedMsg = "expected string"
    override def visitString(cs: CharSequence): String = cs.toString
  }

  class CaseClassTo[T](
    mirror: Mirror.ProductOf[T],
    // parallel arrays in field definition order
    fieldNames: Array[String],
    defaultValues: Array[Option[Unit => AnyRef]],
    createVisitors: => Array[Visitor[_, _]]
  ) extends CaseR[T] :

    lazy val visitors = createVisitors
    val outOfBounds = -1

    val fieldIndex = fieldNames.zipWithIndex.toMap.withDefaultValue(outOfBounds)
    val arity: Int = fieldNames.size

    // by default, no extra level of fallback for missing fields
    def processMissing(index: Int): Either[String, AnyRef] = Left(fieldNames(index))

    def makeWithDefaults(elements: Array[AnyRef], visited: Array[Boolean], missing: Int): T =
      val missingKeys = collection.mutable.ListBuffer.empty[String]

      var i = 0
      var stillMissing = missing
      while (i < arity && stillMissing > 0) do
        if (!visited(i)) {
          defaultValues(i) match {
            case Some(default) =>
              elements(i) = default.apply(())
              stillMissing -= 1
            case None =>
              processMissing(i) match {
                case Right(extraFallbackValue) =>
                  elements(i) = extraFallbackValue
                  stillMissing -= 1
                case Left(missingFieldName) =>
                  missingKeys += missingFieldName
              }
          }
        }
        i += 1

      if (!missingKeys.isEmpty) {
        throw new Abort("missing keys in dictionary: " + missingKeys.mkString(", "))
      }

      makeWithoutDefaults(elements)
    end makeWithDefaults

    def makeWithoutDefaults(elements: Array[AnyRef]): T =
      mirror.fromProduct(new Product {
        def canEqual(that: Any): Boolean = true
        def productArity: Int = arity
        def productElement(i: Int): Any = elements(i)
      })
    end makeWithoutDefaults

    override def visitObject(length: Int) = new ObjVisitor[Any, T] {
      private val elements = new Array[AnyRef](arity)
      private val visited = Array.fill[Boolean](arity)(false)
      private var missing = arity // count down missing fields
      private var currentKey: Int = outOfBounds

      def subVisitor: Visitor[_, _] =
        if (currentKey == outOfBounds) NoOpVisitor
        else visitors(currentKey)

      def visitKey(): Visitor[_, _] = stringVisitor

      def visitKeyValue(v: Any): Unit =
        currentKey = fieldIndex(objectAttributeKeyReadMap(v.asInstanceOf[CharSequence]).toString)

      def visitValue(v: Any): Unit = if (currentKey != outOfBounds) {
        elements(currentKey) = v.asInstanceOf[AnyRef]
        if (!visited(currentKey)) { // may get the same key more than once
          visited(currentKey) = true
          missing -= 1
        }
      }

      def visitEnd(): T =
        if (missing == 0) makeWithoutDefaults(elements)
        else makeWithDefaults(elements, visited, missing)
    }
  end CaseClassTo

  inline def macroTo[T](using m: Mirror.Of[T]): To[T] = inline m match {
    case m: Mirror.ProductOf[T] =>
      // parallel arrays in field definition order
      val fieldNames = macros.fieldLabels[T].map(_._1).toArray
      val defaultValues = fieldNames.map(macros.getDefaultParams[T])

      def createVisitors: Array[Visitor[_, _]] =
        macros.summonList[Tuple.Map[m.MirroredElemTypes, To]].asInstanceOf[List[Visitor[_, _]]].toArray

      val reader = new CaseClassTo[T](
        mirror = m,
        fieldNames,
        defaultValues,
        createVisitors
      )

      val (isSealed, discriminator) = macros.isMemberOfSealedHierarchy[T]
      if isSealed then annotate(reader, discriminator.getOrElse("$type"), macros.fullClassName[T]._1)
      else reader

    case m: Mirror.SumOf[T] =>
      val readers: List[To[_ <: T]] = macros.summonList[Tuple.Map[m.MirroredElemTypes, To]]
        .asInstanceOf[List[To[_ <: T]]]
      To.merge[T](readers: _*)
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
   */
  inline def macroNullableTo[T](using m: Mirror.Of[T]): To[T] = inline m match {
    case m: Mirror.ProductOf[T] =>
      // parallel arrays in field definition order
      val fieldNames = macros.fieldLabels[T].map(_._1).toArray
      val defaultValues = fieldNames.map(macros.getDefaultParams[T])

      def createVisitors: Array[Visitor[_, _]] =
        macros.summonList[Tuple.Map[m.MirroredElemTypes, To]].asInstanceOf[List[Visitor[_, _]]].toArray

      val reader = new CaseClassTo[T](
        mirror = m,
        fieldNames,
        defaultValues,
        createVisitors
      ) {

        // extra level of fallback for missing fields
        override def processMissing(index: Int): Either[String, AnyRef] =
          try {
            Right(visitors(index).visitNull().asInstanceOf[AnyRef])
          } catch {
            case NonFatal(_) => Left(fieldNames(index))
          }
      }

      val (isSealed, discriminator) = macros.isMemberOfSealedHierarchy[T]
      if isSealed then annotate(reader, discriminator.getOrElse("$type"), macros.fullClassName[T]._1)
      else reader

    case m: Mirror.SumOf[T] =>
      val readers: List[To[_ <: T]] = macros.summonList[Tuple.Map[m.MirroredElemTypes, To]]
        .asInstanceOf[List[To[_ <: T]]]
      To.merge[T](readers: _*)
  }

  inline given[T <: Singleton : Mirror.Of]: To[T] = macroTo[T]

  // see comment in MacroImplicits as to why Dotty's extension methods aren't used here
  implicit class ToExtension(r: To.type):
    inline def derived[T](using Mirror.Of[T]): To[T] = macroTo[T]
  end ToExtension

end CaseClassToPiece
