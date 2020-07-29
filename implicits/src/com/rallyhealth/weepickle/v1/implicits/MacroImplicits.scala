package com.rallyhealth.weepickle.v1.implicits

import scala.language.experimental.macros
import scala.language.{existentials, higherKinds}

/**
  * Stupid hacks to work around scalac not forwarding macro type params properly
  */
object MacroImplicits {
  def dieIfNothing[T: c.WeakTypeTag](c: scala.reflect.macros.blackbox.Context)(name: String) = {
    if (c.weakTypeOf[T] =:= c.weakTypeOf[Nothing]) {
      c.abort(
        c.enclosingPosition,
        s"weepickle is trying to infer a $name[Nothing]. That probably means you messed up"
      )
    }
  }
  def applyTo[T](c: scala.reflect.macros.blackbox.Context)(implicit e: c.WeakTypeTag[T]): c.Expr[T] = {
    import c.universe._
    dieIfNothing[T](c)("To")
    c.Expr[T](q"${c.prefix}.macroTo0[$e, ${c.prefix}.To]")
  }
  def applyNullableTo[T](c: scala.reflect.macros.blackbox.Context)(implicit e: c.WeakTypeTag[T]): c.Expr[T] = {
    import c.universe._
    dieIfNothing[T](c)("To")
    c.Expr[T](q"${c.prefix}.macroNullableTo0[$e, ${c.prefix}.To]")
  }
  def applyFrom[T](c: scala.reflect.macros.blackbox.Context)(implicit e: c.WeakTypeTag[T]): c.Expr[T] = {
    import c.universe._
    dieIfNothing[T](c)("From")
    c.Expr[T](q"${c.prefix}.macroFrom0[$e, ${c.prefix}.From]")
  }

  def applyFromTo[T](c: scala.reflect.macros.blackbox.Context)(implicit e: c.WeakTypeTag[T]): c.Expr[T] = {
    import c.universe._
    dieIfNothing[T](c)("From")
    c.Expr[T](q"${c.prefix}.FromTo.join(${c.prefix}.macroTo, ${c.prefix}.macroFrom)")
  }

  /**
    * Implementation of macros used by weepickle to serialize and deserialize
    * case classes automatically. You probably shouldn't need to use these
    * directly, since they are called implicitly when trying to read/write
    * types you don't have a To/From in scope for.
    */

  private trait DeriveDefaults[M[_]] {
    /*
     * Abstract members
     */
    protected val c: scala.reflect.macros.blackbox.Context
    import c.universe._

    protected def typeclass: WeakTypeTag[M[_]]

    protected def wrapObject(obj: Tree): Tree

    protected def wrapCaseN(companion: Tree, args: Seq[Argument], targetType: Type, varargs: Boolean): Tree

    protected def mergeTrait(subtrees: Seq[Tree], subtypes: Seq[Type], targetType: Type): Tree

    /*
     * Concrete, accessible members
     */
    def derive(tpe: Type): Tree = {
      if (tpe.typeSymbol.asClass.isTrait || (tpe.typeSymbol.asClass.isAbstract && !tpe.typeSymbol.isJava)) {
        val derived = deriveTrait(tpe)
        derived
      } else if (tpe.typeSymbol.isModuleClass) deriveObject(tpe)
      else deriveClass(tpe)
    }

    protected def getArgSyms(tpe: Type): Either[String, (Tree, List[Symbol], List[Symbol])] = {
      companionTree(tpe).right.flatMap { companion =>
        //tickle the companion members -- Not doing this leads to unexpected runtime behavior
        //I wonder if there is an SI related to this?
        companion.tpe.members.foreach(_ => ())
        tpe.members.find(x => x.isMethod && x.asMethod.isPrimaryConstructor) match {
          case None => Left("Can't find primary constructor of " + tpe)
          case Some(primaryConstructor) =>
            val flattened = primaryConstructor.asMethod.paramLists.flatten
            Right(
              (
                companion,
                tpe.typeSymbol.asClass.typeParams,
                flattened
              )
            )
        }
      }
    }

    private def fail(tpe: Type, s: String): Nothing = c.abort(c.enclosingPosition, s)

    private def companionTree(tpe: Type): Either[String, Tree] = {
      val companionSymbol = tpe.typeSymbol.companion

      if (companionSymbol == NoSymbol && tpe.typeSymbol.isClass) {
        val clsSymbol = tpe.typeSymbol.asClass
        val msg = "[error] The companion symbol could not be determined for " +
          s"[[${clsSymbol.name}]]. This may be due to a bug in scalac (SI-7567) " +
          "that arises when a case class within a function is com.rallyhealth.weepickle.v1. As a " +
          "workaround, move the declaration to the module-level."
        Left(msg)
      } else {
        val symTab = c.universe.asInstanceOf[reflect.internal.SymbolTable]
        val pre = tpe.asInstanceOf[symTab.Type].prefix.asInstanceOf[Type]
        Right(c.universe.internal.gen.mkAttributedRef(pre, companionSymbol))
      }
    }

    /*
     * Wrap all the argument stuff up in a case class for easy processing
     */
    protected case class Argument(
      i: Int,
      raw: String,
      mapped: String,
      argType: Type,
      typeConstructor: Type,
      hasDefault: Boolean,
      omitDefault: Boolean,
      default: Tree,
      localTo: TermName,
      aggregate: TermName
    )

    protected object Argument {

      /**
        * Unlike lihaoyi/upickle, rallyhealth/weepickle will write values even if they're
        * the same as the default value, unless instructed explicitly not to with the
        * [[dropDefault]] annotation at the class or argument level.
        *
        * We are upgrading from play-json which always sends default values.
        * If teams swapped in weepickle for play-json and their rest endpoints started
        * omitting fields, that would be a surprising breaking API change.
        * The play-json will throw if a default valued field is missing,
        * e.g. Json.parse("{}").as[FooDefault] // throws: missing i
        *
        * Over time, consumers will transition to tolerant weepickle readers, and we
        * can revisit this.
        */
      private def shouldDropDefault(classSym: c.Symbol, argSym: c.Symbol): Boolean =
        argSym.annotations.exists(_.tree.tpe == typeOf[dropDefault]) ||
          classSym.annotations.exists(_.tree.tpe == typeOf[dropDefault])

      /*
       * Play Json assumes None as the default for Option types, so None is serialized as missing from the output,
       * and when data is missing on input, deserialization assigns None to these fields. upickle only processes
       * defaults as specified explicitly. To be compatible deserializing JSON serialized with Play Json previously,
       * WeePickle will automatically force a default of None to all Option fields without an explicit default.
       */
      private def deriveDefault(
        companion: Tree,
        index: Int,
        isParamWithDefault: Boolean,
        assumeDefaultNone: Boolean
      ): Tree = {
        val defaultName = TermName("apply$default$" + (index + 1))
        if (!isParamWithDefault) {
          if (assumeDefaultNone) q"${TermName("None")}"
          else q"null"
        } else {
          q"$companion.$defaultName"
        }
      }

      private def argTypeFromSignature(tpe: Type, typeParams: List[Symbol], t: Type): Type = {
        val concrete = tpe.dealias.asInstanceOf[TypeRef].args
        if (t.typeSymbol != definitions.RepeatedParamClass) {
          t.substituteTypes(typeParams, concrete)
        } else {
          val TypeRef(pref, sym, _) = typeOf[Seq[Int]]
          internal.typeRef(pref, sym, t.asInstanceOf[TypeRef].args)
        }
      }

      def apply(tpe: Type, companion: Tree, typeParams: List[Symbol])(indexedArg: (Symbol, Int)): Argument = {
        val (argSym, index) = indexedArg
        val isParamWithDefault = argSym.asTerm.isParamWithDefault
        // include .erasure to represent varargs as "Seq", not "Whatever*"
        val typeConstructor = argSym.typeSignature.erasure.typeConstructor
        val isOptionWithoutDefault = !isParamWithDefault && typeConstructor.toString == "Option"
        val hasDefault = isParamWithDefault || isOptionWithoutDefault

        Argument(
          i = index,
          raw = argSym.name.toString,
          mapped = customKey(argSym).getOrElse(argSym.name.toString),
          argType = argTypeFromSignature(tpe, typeParams, argSym.typeSignature),
          typeConstructor = typeConstructor,
          hasDefault = hasDefault,
          omitDefault = shouldDropDefault(tpe.typeSymbol, argSym),
          default = deriveDefault(companion, index, isParamWithDefault, isOptionWithoutDefault),
          localTo = TermName("localTo" + index),
          aggregate = TermName("aggregated" + index)
        )
      }
    }

    /*
     * Concrete, private members
     */
    private def deriveTrait(tpe: Type): Tree = {
      val clsSymbol = tpe.typeSymbol.asClass

      if (!clsSymbol.isSealed) {
        fail(tpe, s"[error] The referenced trait [[${clsSymbol.name}]] must be sealed.")
      } else if (clsSymbol.knownDirectSubclasses.forall(_.toString.contains("<local child>"))) {
        val msg =
          s"The referenced trait [[${clsSymbol.name}]] does not have any sub-classes. This may " +
            "happen due to a limitation of scalac (SI-7046). To work around this, " +
            "try manually specifying the sealed trait picklers as described in " +
            "http://www.lihaoyi.com/upickle/#ManualSealedTraitPicklers"
        fail(tpe, msg)
      } else {
        val subTypes = fleshedOutSubtypes(tpe).toSeq
        //    println("deriveTrait")
        val subDerives = subTypes.map(subCls => q"implicitly[${typeclassFor(subCls)}]")
        //    println(Console.GREEN + "subDerives " + Console.RESET + subDrivess)
        val merged = mergeTrait(subDerives, subTypes, tpe)
        merged
      }
    }

    private def deriveObject(tpe: Type): Tree = {
      val mod = tpe.typeSymbol.asClass.module
      val symTab = c.universe.asInstanceOf[reflect.internal.SymbolTable]
      val pre = tpe.asInstanceOf[symTab.Type].prefix.asInstanceOf[Type]
      val mod2 = c.universe.internal.gen.mkAttributedRef(pre, mod)

      annotate(tpe)(wrapObject(mod2))
    }

    private def deriveClass(tpe: Type): Tree = {
      getArgSyms(tpe) match {
        case Left(msg) => fail(tpe, msg)
        case Right((companion, typeParams, argSyms)) =>
          val args = argSyms.zipWithIndex.map(Argument(tpe, companion, typeParams))

          // According to @retronym, this is necessary in order to force the
          // default argument `apply$default$n` methods to be synthesized
          companion.tpe.member(TermName("apply")).info

          val derive =
            // Otherwise, reading and writing are kinda identical
            wrapCaseN(
              companion,
              args,
              tpe,
              argSyms.exists(_.typeSignature.typeSymbol == definitions.RepeatedParamClass)
            )

          annotate(tpe)(derive)
      }
    }

    /**
      * If a super-type is generic, find all the subtypes, but at the same time
      * fill in all the generic type parameters that are based on the super-type's
      * concrete type
      */
    private def fleshedOutSubtypes(tpe: Type): Set[Type] = {
      for {
        subtypeSym <- tpe.typeSymbol.asClass.knownDirectSubclasses.filter(!_.toString.contains("<local child>"))
          if subtypeSym.isType
          st = subtypeSym.asType.toType
          baseClsArgs = st.baseType(tpe.typeSymbol).asInstanceOf[TypeRef].args
      } yield {
        tpe match {
          case ExistentialType(_, TypeRef(_, _, args)) =>
            st.substituteTypes(baseClsArgs.map(_.typeSymbol), args)
          case ExistentialType(_, _) => st
          case TypeRef(_, _, args) =>
            st.substituteTypes(baseClsArgs.map(_.typeSymbol), args)
        }
      }
    }

    private def typeclassFor(t: Type): Type = {
      //    println("typeclassFor " + weakTypeOf[M[_]](typeclass))

      weakTypeOf[M[_]](typeclass) match {
        case TypeRef(a, b, _) =>
          internal.typeRef(a, b, List(t))
        case ExistentialType(_, TypeRef(a, b, _)) =>
          internal.typeRef(a, b, List(t))
        case x =>
          println("Dunno Wad Dis Typeclazz Is " + x)
          println(x)
          println(x.getClass)
          ???
      }
    }

    /** If there is a sealed base class, annotate the derived tree in the JSON
      * representation with a class label.
      */
    private def annotate(tpe: Type)(derived: Tree): Tree = {
      val sealedParent = tpe.baseClasses.find(_.asClass.isSealed)
      sealedParent.fold(derived) { parent =>
        val tagName = customDiscriminator(parent) match {
          case Some(customName) => Literal(Constant(customName))
          case None             => q"${c.prefix}.tagName"
        }
        val tag = customKey(tpe.typeSymbol).getOrElse(tpe.typeSymbol.fullName)
        q"""${c.prefix}.annotate($derived, $tagName, $tag)"""
      }
    }

    private def customDiscriminator(sym: Symbol): Option[String] = {
      sym.annotations
        .find(_.tree.tpe == typeOf[discriminator])
        .flatMap(_.tree.children.tail.headOption)
        .map { case Literal(Constant(s)) => s.toString }
    }

    private def customKey(sym: Symbol): Option[String] = {
      sym.annotations
        .find(_.tree.tpe == typeOf[key])
        .flatMap(_.tree.children.tail.headOption)
        .map { case Literal(Constant(s)) => s.toString }
    }
  }

  private abstract class Reading[M[_]] extends DeriveDefaults[M] {
    import c.universe._

    def applyDefaultsWhenMissing(args: Seq[Argument]): Tree =
      q"""..${
        for (arg <- args if arg.hasDefault) yield
          q"if ((found & (1L << ${arg.i})) == 0) {found |= (1L << ${arg.i}); storeAggregatedValue(${arg.i}, ${arg.default})}"
      }"""

    /*
     * Original pattern-match based method (not used, here more for documentation)
     */
    def getKeyIndexUsingPatternMatch(args: Seq[Argument]): Tree =
      q"""${c.prefix}.objectAttributeKeyReadMap(s.toString).toString match {
                case ..${
        for (arg <- args) yield cq"${arg.mapped} => ${arg.i}"
      }
                case _ => -1
      }"""


    /*
     * https://en.wikipedia.org/wiki/Radix_tree -- performance is better than just pattern matching by 20% - 60%
     * depending of the size of the case class. Fewer gains for case classes with 5 - 8 fields, more gains for
     * (weirdly) both smaller and larger case classes.
     *
     * Assigns currentIndex within visitKeyValue using the key value length as the primary driver. Within each
     * length, Radix logic compares common string prefixes only once. In fact, any character will only be compared
     * with a positive match at most once -- there may be multiple mismatches per character, but these are minimized
     * by having logic in regionMatches to return mismatch at the first mismatching character in a region.
     *
     * For example, the following shows the matching logic for fields "icn" (only three-character field), "claimFacility"
     * and "claimRemarkCd" (same length with common prefix "claim"), "icnSuffixCode" and "icnVersionNum" (also the same
     * length with common prefix "icn"), and "claimLevelAdjudicationAmt" (only 25 character field).
     *
     *  import com.rallyhealth.weepickle.v1.core.Util.regionMatches
     *  val cs = WeePickle.objectAttributeKeyReadMap(s match {
     *     case alreadyCs: CharSequence => alreadyCs
     *     case force => force.toString
     *   });
     *   cs.length: @scala.annotation.switch match {
     *     case 3 =>
     *       if (regionMatches(cs, 0, "icn", 0, 3)) 21 else -1
     *
     *     case 7 =>
     *       if ...
     *
     *     case 13 =>
     *       if (regionMatches(cs, 0, "claim", 0, 5)) {
     *         if (regionMatches(cs, 5, "Facility", 0, 8)) 31
     *         else if (regionMatches(cs, 5, "RemarkCd", 0, 8)) 41
     *         else -1
     *       }
     *       else if (regionMatches(cs, 0, "icn", 0, 3)) {
     *         if (regionMatches(cs, 3, "SuffixCode", 0, 10)) 25
     *         else if (regionMatches(cs, 3, "VersionNum", 0, 10)) 26
     *         else -1
     *       }
     *       else -1
     * ...
     *
     *     case 25 =>
     *       if (regionMatches(cs, 0, "claimLevelAdjudicationAmt", 0, 25)) 36
     *       else -1
     *
     *     case _ => -1
     *  }
     */
    def getKeyIndexUsingRadix(args: Seq[Argument]): Tree = {

      sealed trait Node

      case class Branch(prefix: String, start: Int, end: Int, children: List[Node]) extends Node

      case class Leaf(suffix: String, start: Int, fullKey: String, value: Int) extends Node

      val leafGroupThreshold = 1 // increase if you want leaf piles

      /*
       * Each node in the returned list is its own Radix tree. Conceptually you can think of this as
       * one Radix tree with the root node having an empty prefix.
       */
      def buildRadix(fields: List[(String, Int)], fullPrefix: String = ""): List[Node] = {
        if (fields.length <= leafGroupThreshold)
          fields.map { case (s, t) => Leaf(s, fullPrefix.length, s"$fullPrefix$s", t) }
        else {
          val (empties, nonEmpties) = fields.span(_._1.length == 0)

          if (empties.length > 1) throw new AssertionError(s"duplicate field: $fullPrefix")
          val leaf = empties.map { case (_, t) => Leaf("", fullPrefix.length, fullPrefix, t) }

          val children = nonEmpties.groupBy(_._1.charAt(0)).toList.sortBy(_._1).map {
            case (k, vs) => buildRadix(vs.map { case (s, t) => s.drop(1) -> t }, s"$fullPrefix$k") match {
                case Branch(prefix, start, end, children) :: Nil => Branch(s"$k$prefix", start - 1, end, children) // compress
                case Leaf(suffix, start, fullKey, value) :: Nil => Leaf(s"$k$suffix", start - 1, fullKey, value) // compress
                case many => Branch(s"$k", fullPrefix.length, fullPrefix.length + 1, many) // don't compress
              }
          }

          children ++ leaf
        }
      }

      def evalRadix(nodes: List[Node]): Tree = {
        // assemble chained if/else if/else if/.../else statement recursively
        def chainConditions(conditionActions: List[(Tree, Tree)], otherwise: Tree): Tree = conditionActions match {
          case (condition, action) :: tail => q"if ($condition) $action else ${chainConditions(tail, otherwise)}"
          case Nil => otherwise
        }

        def nodeCondition(n: Node): Tree = n match {
          case Branch(prefix, start, end, _) => q"regionMatches(cs, $start, $prefix, 0, ${end - start})"
          case Leaf(suffix, start, _, _) =>
            if (suffix.isEmpty) q"true" // avoid compare when suffix is empty
            else q"regionMatches(cs, $start, $suffix, 0, ${suffix.length})"
        }

        def nodeAction(n: Node): Tree = n match {
          case b: Branch => evalRadix(b.children) // recursively evaluate children
          case l: Leaf => q"${l.value}"
        }

        chainConditions(nodes.map(n => (nodeCondition(n), nodeAction(n))), q"-1")
      }

      val evalRadixBySize: List[(Int, Tree)] = args.groupBy(_.mapped.length)
        .toList.sortBy(_._1).map {
        case (size, argsForSize) => size -> evalRadix(buildRadix(argsForSize.map(arg => (arg.mapped, arg.i)).toList))
      }
      // avoid string allocations by keeping a CharSequence a CharSequence
      q"""import com.rallyhealth.weepickle.v1.core.Util.regionMatches
          val cs = ${c.prefix}.objectAttributeKeyReadMap(s match {
            case alreadyCs: CharSequence => alreadyCs
            case force => force.toString
          })
          (cs.length: @scala.annotation.switch) match {
            case ..${for ((size, tree) <- evalRadixBySize)
                       yield cq"$size => $tree"}
            case _ => -1
          }
       """
    }

    override def wrapObject(t: Tree) = q"new ${c.prefix}.SingletonR($t)"

    override def wrapCaseN(companion: Tree, args: Seq[Argument], targetType: Type, varargs: Boolean): Tree = {
      if (args.size > 64) {
        c.abort(c.enclosingPosition, "weepickle does not support serializing case classes with >64 fields")
      }
      q"""
        ..${for (arg <- args)
        yield q"private[this] lazy val ${arg.localTo} = implicitly[${c.prefix}.To[${arg.argType}]]"}
        new ${c.prefix}.CaseR[$targetType]{
          override def visitObject(length: Int) = new CaseObjectContext{
            ..${for (arg <- args)
        yield q"private[this] var ${arg.aggregate}: ${arg.argType} = _"}
            def storeAggregatedValue(currentIndex: Int, v: Any): Unit = currentIndex match{
              case ..${for (arg <- args)
        yield cq"${arg.i} => ${arg.aggregate} = v.asInstanceOf[${arg.argType}]"}
            }
            def visitKey() = com.rallyhealth.weepickle.v1.core.StringVisitor
            def visitKeyValue(s: Any) = {
              currentIndex = ${getKeyIndexUsingRadix(args)}
            }

            def visitEnd() = {
              ..${applyDefaultsWhenMissing(args)}

              // Special-case 64 because java bit shifting ignores any RHS values above 63
              // https://docs.oracle.com/javase/specs/jls/se7/html/jls-15.html#jls-15.19
              if (found != ${if (args.length == 64) -1 else (1L << args.length) - 1}){
                var i = 0
                val keys = for{
                  i <- 0 until ${args.length}
                  if (found & (1L << i)) == 0
                } yield i match{
                  case ..${for (arg <- args)
        yield cq"${arg.i} => ${arg.mapped}"}
                }
                throw new com.rallyhealth.weepickle.v1.core.Abort(
                  "missing keys in dictionary: " + keys.mkString(", ")
                )
              }
              $companion.apply(
                ..${for (arg <- args)
        yield
          if (arg.i == args.length - 1 && varargs) q"${arg.aggregate}:_*"
          else q"${arg.aggregate}"}
              )
            }

            def subVisitor: com.rallyhealth.weepickle.v1.core.Visitor[_, _] = currentIndex match{
              case -1 => com.rallyhealth.weepickle.v1.core.NoOpVisitor
              case ..${for (arg <- args)
        yield cq"${arg.i} => ${arg.localTo} "}
            }
          }
        }
      """
    }

    override def mergeTrait(subtrees: Seq[Tree], subtypes: Seq[Type], targetType: Type): Tree = {
      q"${c.prefix}.To.merge[$targetType](..$subtrees)"
    }
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
   */
  private abstract class NullableReading[M[_]] extends Reading[M] {

    import c.universe._

    val nullableContainerTypes = Set("Seq", "List", "Array", "scala.collection.immutable.Map") // Not "Option" -- see above

    override def applyDefaultsWhenMissing(args: Seq[Argument]): Tree =
      q"""..${
        for (arg <- args) yield
          if (arg.hasDefault)
            q"if ((found & (1L << ${arg.i})) == 0) {found |= (1L << ${arg.i}); storeAggregatedValue(${arg.i}, ${arg.default})}"
          else if (nullableContainerTypes contains arg.typeConstructor.toString)
            q"if ((found & (1L << ${arg.i})) == 0) {found |= (1L << ${arg.i}); storeAggregatedValue(${arg.i}, ${arg.localTo}.visitNull())}"
          else
            q""
      }"""
  }

  private abstract class Writing[M[_]] extends DeriveDefaults[M] {
    import c.universe._

    override def wrapObject(obj: Tree) = q"new ${c.prefix}.SingletonW($obj)"

    //private def findUnapply(tpe: Type): TermName = {
    //  val (companion, _, _) = getArgSyms(tpe).fold(
    //    errMsg => c.abort(c.enclosingPosition, errMsg),
    //    x => x
    //  )
    //  Seq("unapply", "unapplySeq")
    //    .map(TermName(_))
    //    .find(companion.tpe.member(_) != NoSymbol)
    //    .getOrElse(
    //      c.abort(
    //        c.enclosingPosition,
    //        "None of the following methods " +
    //          "were defined: unapply, unapplySeq"
    //      )
    //    )
    //}

    //private def internal = q"${c.prefix}.Internal"

    override def wrapCaseN(companion: Tree, args: Seq[Argument], targetType: Type, varargs: Boolean): Tree = {
      def write(arg: Argument) = {
        val snippet = q"""

          val keyVisitor = ctx.visitKey()
          ctx.visitKeyValue(
            keyVisitor.visitString(
              ${c.prefix}.objectAttributeKeyWriteMap(${arg.mapped})
            )
          )
          val w = implicitly[${c.prefix}.From[${arg.argType}]]
          ctx.narrow.visitValue(w.transform(v.${TermName(arg.raw)}, ctx.subVisitor))
        """

        /**
          * @see [[shouldDropDefault()]]
          */
        if (arg.omitDefault) q"""if (v.${TermName(arg.raw)} != ${arg.default}) $snippet"""
        else snippet
      }
      q"""
        new ${c.prefix}.CaseW[$targetType]{
          def length(v: $targetType) = {
            var n = 0
            ..${for (arg <- args)
        yield {
          if (!arg.omitDefault) q"n += 1"
          else q"""if (v.${TermName(arg.raw)} != ${arg.default}) n += 1"""
        }}
            n
          }
          def writeToObject[R](ctx: com.rallyhealth.weepickle.v1.core.ObjVisitor[_, R],
                               v: $targetType): Unit = {
            ..${args.map(write)}

          }
        }
       """
    }

    override def mergeTrait(subtree: Seq[Tree], subtypes: Seq[Type], targetType: Type): Tree = {
      q"${c.prefix}.From.merge[$targetType](..$subtree)"
    }
  }

  /*
   * Publicly accessible members
   */

  def macroRImpl[T, R[_]](
    c0: scala.reflect.macros.blackbox.Context
  )(implicit e1: c0.WeakTypeTag[T], e2: c0.WeakTypeTag[R[_]]): c0.Expr[R[T]] = {
    val res = new Reading[R] {
      override val c: c0.type = c0
      def typeclass: c.universe.WeakTypeTag[R[_]] = e2
    }.derive(e1.tpe)
    //    println(res)
    c0.Expr[R[T]](res)
  }

  def macroNullableRImpl[T, R[_]](
    c0: scala.reflect.macros.blackbox.Context
  )(implicit e1: c0.WeakTypeTag[T], e2: c0.WeakTypeTag[R[_]]): c0.Expr[R[T]] = {
    val res = new NullableReading[R] {
      override val c: c0.type = c0
      def typeclass: c.universe.WeakTypeTag[R[_]] = e2
    }.derive(e1.tpe)
    //    println(res)
    c0.Expr[R[T]](res)
  }

  def macroTImpl[T, W[_]](
    c0: scala.reflect.macros.blackbox.Context
  )(implicit e1: c0.WeakTypeTag[T], e2: c0.WeakTypeTag[W[_]]): c0.Expr[W[T]] = {
    val res = new Writing[W] {
      override val c: c0.type = c0
      def typeclass: c.universe.WeakTypeTag[W[_]] = e2
    }.derive(e1.tpe)
    //    println(res)
    c0.Expr[W[T]](res)
  }
}

trait MacroImplicits { this: com.rallyhealth.weepickle.v1.core.Types =>
  implicit def macroSingletonTo[T <: Singleton]: To[T] = macro MacroImplicits.applyTo[T]
  implicit def macroSingletonFrom[F <: Singleton]: From[F] = macro MacroImplicits.applyFrom[F]
  implicit def macroSingletonFromTo[X <: Singleton]: FromTo[X] = macro MacroImplicits.applyFromTo[X]
  def macroFrom[F]: From[F] = macro MacroImplicits.applyFrom[F]
  def macroTo[T]: To[T] = macro MacroImplicits.applyTo[T]
  def macroNullableTo[T]: To[T] = macro MacroImplicits.applyNullableTo[T]
  def macroFromTo[X]: FromTo[X] = macro MacroImplicits.applyFromTo[FromTo[X]]

  def macroTo0[T, M[_]]: To[T] = macro MacroImplicits.macroRImpl[T, M]
  def macroNullableTo0[T, M[_]]: To[T] = macro MacroImplicits.macroNullableRImpl[T, M]
  def macroFrom0[T, M[_]]: From[T] = macro MacroImplicits.macroTImpl[T, M]
}
