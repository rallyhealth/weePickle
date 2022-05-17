package com.rallyhealth.weepickle.v1.implicits.macros

import scala.quoted.{ given, _ }
import deriving._, compiletime._

inline def getDefaultParams[T]: String => Option[() => AnyRef] = ${ getDefaultParamsImpl[T] }
def getDefaultParamsImpl[T](using Quotes, Type[T]): Expr[String => Option[() => AnyRef]] =
  import quotes.reflect._

  /*
   * Classes with parameterized types do not have direct symbol translations. For these we
   * get the type symbol from the type instead.
   */
  val sym =
    if (TypeTree.of[T].symbol.isNoSymbol)
      TypeTree.of[T].tpe.typeSymbol
    else
      TypeTree.of[T].symbol
  /*
   * TBD if there is a better way. Also can the s.tree call ever fail for a case class? From
   * https://docs.scala-lang.org/scala3/guides/macros/best-practices.html -- Avoid "Symbol.tree":
   *
   *   ... Be careful when using this method as the tree for a symbol might not be defined. When the code
   *   associated to the symbol is defined in a different moment than this access, if the -Yretain-trees
   *   compilation option is not used, then the tree of the symbol will not be available. Symbols originated
   *   from Java code do not have an associated tree.
   *
   * (I wish I knew what a "different moment than this access" was... assuming they meant a different compile
   * operation... so I think maybe we are okay?)
   */
  def isOption(s:Symbol): Boolean = s.tree match {
    case ValDef(_, tree, _) => tree.tpe.classSymbol == TypeRepr.of[scala.Option].classSymbol
    case _ => false
  }


  if (sym.isClassDef) {
    val comp = sym.companionClass
    val names =
      for p <- sym.caseFields if p.flags.is(Flags.HasDefault)
      yield p.name
    // println(s"getDefaultParams for $sym names with defaults = $names")

    val body = comp.tree.asInstanceOf[ClassDef].body
    val idents: List[Ref] =
      for case deff @ DefDef(name, _, _, _) <- body if name.startsWith("$lessinit$greater$default")
      yield Ref(deff.symbol)
    // println(s"getDefaultParams for $sym idents = $idents")

    // synthesize default None for any Option types where a default is not already specified
    val assumeDefaultNoneNames =
      for p <- sym.caseFields if !names.contains(p.name) && isOption(p)
      yield p.name
    // println(s"getDefaultParams for $sym assumeDefaultNoneNames = $assumeDefaultNoneNames")

    val namesExpr: Expr[List[String]] = Expr.ofList((names ++ assumeDefaultNoneNames).map(Expr(_)))
    val identsExpr: Expr[List[() => AnyRef]] = Expr.ofList(
      idents.map(_.asExpr).map(i => '{ () => $i.asInstanceOf[AnyRef] }) ++
      assumeDefaultNoneNames.map(_ => '{ () => scala.None })
    )

    '{ $namesExpr.zip($identsExpr).toMap.get }
  } else {
    '{ Map.empty.get }
  }
end getDefaultParamsImpl

inline def summonList[T <: Tuple]: List[_] =
  inline erasedValue[T] match
    case _: EmptyTuple => Nil
    case _: (t *: ts) => summonInline[t] :: summonList[ts]
end summonList

private def annotationString[T: Type](using Quotes)(sym: quotes.reflect.Symbol): Option[String] =
  import quotes.reflect._
  sym
    .annotations
    .find(_.tpe =:= TypeRepr.of[T])
    .collect { case Apply(_, Literal(StringConstant(s)) :: Nil) => s }
end annotationString

private def annotationExists[T: Type](using Quotes)(sym: quotes.reflect.Symbol): Boolean =
  import quotes.reflect._
  sym
    .annotations
    .exists(_.tpe =:= TypeRepr.of[T])
end annotationExists

def extractKey[A](using Quotes)(sym: quotes.reflect.Symbol): Option[String] =
  annotationString[com.rallyhealth.weepickle.v1.implicits.key](sym)

/*
 * Returns the custom discriminator we should use instead of "$type", if there is one.
 */
def extractDiscriminator[A](using Quotes)(sym: quotes.reflect.Symbol): Option[String] =
  annotationString[com.rallyhealth.weepickle.v1.implicits.discriminator](sym)

/*
 * Returns if dropDefault is present. Could be defined at the class or field level.
 */
def extractDropDefault[A](using Quotes)(sym: quotes.reflect.Symbol): Boolean =
  annotationExists[com.rallyhealth.weepickle.v1.implicits.dropDefault](sym)

/*
 * Return associated field labels and an indication of if individual field defaults should be dropped.
 */
inline def fieldLabels[T]: List[(String, Boolean)] = ${fieldLabelsImpl[T]}
def fieldLabelsImpl[T](using Quotes, Type[T]): Expr[List[(String, Boolean)]] =
  import quotes.reflect._

  /*
   * Classes with parameterized types do not have direct symbol translations. For these we
   * get the type symbol from the type instead. Using .caseFields here prevents the type
   * parameters themselves from being returned as part of the field list, but since
   * .caseFields won't work on case objects, we keep the .primaryConstructor logic for
   * everything else.
   */
  val fields: List[Symbol] =
    if (TypeTree.of[T].symbol.isNoSymbol)
      TypeTree.of[T].tpe.typeSymbol
        .caseFields
    else
      TypeTree.of[T].symbol
        .primaryConstructor
        .paramSymss
        .flatten

  val names = fields.map{ sym =>
    extractKey(sym) match {
      case Some(name) => name
      case None => sym.name
    }
  }
  val dropDefaults = fields.map{ sym =>
    extractDropDefault(sym)
  }
  // println(s"fieldLabels field names = $names, dropDefaults = $dropDefaults")

  Expr.ofList(names.zip(dropDefaults).map(Expr(_)))
end fieldLabelsImpl

/*
 * Return if T is in a sealed hierarchy and, if so, any non-default discriminator
 */
inline def isMemberOfSealedHierarchy[T]: (Boolean, Option[String]) = ${ isMemberOfSealedHierarchyImpl[T] }
def isMemberOfSealedHierarchyImpl[T](using Quotes, Type[T]): Expr[(Boolean, Option[String])] =
  import quotes.reflect._

  val parents = TypeRepr.of[T].baseClasses
  val sealedParents = parents.filter(_.flags.is(Flags.Sealed))
  val isSealed: Boolean = sealedParents.nonEmpty
  val discriminator = sealedParents.flatMap(extractDiscriminator).headOption.filter(_ => isSealed)
  // println(s"sealedParents = $sealedParents, discriminator = $discriminator")
  Expr((isSealed, discriminator))
end isMemberOfSealedHierarchyImpl

/*
 * Finds the valueOf method (a DefDef) in the companion of the sealed trait for this enum which
 * is used to decode a string as an enum value.
 */
inline def enumValueOf[T]: String => T = ${ enumValueOfImpl[T] }
def enumValueOfImpl[T](using Quotes, Type[T]): Expr[String => T] =
  import quotes.reflect._

  // println(s"enumValueOf isNoSymbol = ${TypeTree.of[T].symbol.isNoSymbol}")
  if (TypeTree.of[T].symbol.isNoSymbol) throw Exception("Enumeration default derivation not supported: type is not a symbol")

  val sym = TypeTree.of[T].symbol
  // println(s"enumValueOf sym = $sym, isClassDef = ${sym.isClassDef}, isNoSymbol = ${TypeTree.of[T].symbol.isNoSymbol}")
  if (!sym.isClassDef) throw Exception("Enumeration default derivation not supported: type is not a class definition")

  val companion = sym.companionClass.tree.asInstanceOf[ClassDef]
  // println(s"enumValueOf companion.symbol = ${companion.symbol}")

  val valueOfMethods: List[DefDef] = companion.body.collect{
    case dd @ DefDef("valueOf", _, _, _) => dd
  }
  // println(s"enumValueOf valueOfMethods = valueOfMethods")
  if (valueOfMethods.size != 1) throw Exception("Enumeration default derivation not supported: companion valueOf method not found")

  val methodSymbol = valueOfMethods.head.symbol
  // println(s"enumValueOf methodSymbol = $methodSymbol, methodSymbol.owner = ${methodSymbol.owner}")
  Ref(methodSymbol).etaExpand(methodSymbol.owner).asExpr.asInstanceOf[Expr[String => T]]
end enumValueOfImpl

/*
 * Return class name and an indication of if all field defaults should be dropped.
 */
inline def fullClassName[T]: (String, Boolean) = ${ fullClassNameImpl[T] }
def fullClassNameImpl[T](using Quotes, Type[T]): Expr[(String, Boolean)] =
  import quotes.reflect._

  /*
   * Classes with parameterized types do not have direct symbol translations. For these we
   * get the type symbol from the type instead. We can pass all tests if we just use .tpe.typeSymbol
   * for everything, but keeping this logic consistent with getDefaultParams above (just in case).
   */
  val sym =
    if (TypeTree.of[T].symbol.isNoSymbol)
      TypeTree.of[T].tpe.typeSymbol
    else
      TypeTree.of[T].symbol
  // println(s"fullClassName symbol = $sym")

  val key = extractKey(sym) match {
    case Some(name) =>
      // println(s"fullClassName key found = $name")
      name
    case None =>
      // println(s"fullClassName key not found, using = ${sym.fullName.replace("$", "")}")
      sym.fullName.replace("$", "")
  }

  val dropDefault = extractDropDefault(sym)

  // println(s"fullClassName dropDefault = $dropDefault")
  Expr((key, dropDefault))
end fullClassNameImpl
