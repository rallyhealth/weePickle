package com.rallyhealth.weepickle.v1.implicits.macros

import scala.quoted.{ given, _ }
import deriving._, compiletime._

inline def getDefaultParams[T]: String => Option[() => AnyRef] = ${ getDefaultParmasImpl[T] }
def getDefaultParmasImpl[T](using Quotes, Type[T]): Expr[String => Option[() => AnyRef]] =
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
    val comp = if (sym.isClassDef) sym.companionClass else sym
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
      assumeDefaultNoneNames.map(_ => '{ () => None })
    )

    '{ $namesExpr.zip($identsExpr).toMap.get }
  } else {
    '{ Map.empty.get }
  }
end getDefaultParmasImpl

inline def summonList[T <: Tuple]: List[_] =
  inline erasedValue[T] match
    case _: EmptyTuple => Nil
    case _: (t *: ts) => summonInline[t] :: summonList[ts]
end summonList

def extractKey[A](using Quotes)(sym: quotes.reflect.Symbol): Option[String] =
  import quotes.reflect._
  sym
    .annotations
    .find(_.tpe =:= TypeRepr.of[com.rallyhealth.weepickle.v1.implicits.key])
    .map{case Apply(_, Literal(StringConstant(s)) :: Nil) => s}
end extractKey

/*
 * Returns the custom discriminator we should use instead of "$type", if there is one.
 */
def extractDiscriminator[A](using Quotes)(sym: quotes.reflect.Symbol): Option[String] =
  import quotes.reflect._
  sym
    .annotations
    .find(_.tpe =:= TypeRepr.of[com.rallyhealth.weepickle.v1.implicits.discriminator])
    .map{case Apply(_, Literal(StringConstant(s)) :: Nil) => s}
end extractDiscriminator

/*
 * Returns if dropDefault is present. Could be defined at the class or field level.
 */
def extractDropDefault[A](using Quotes)(sym: quotes.reflect.Symbol): Boolean =
  import quotes.reflect._
  sym
    .annotations
    .exists(_.tpe =:= TypeRepr.of[com.rallyhealth.weepickle.v1.implicits.dropDefault])
end extractDropDefault

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
