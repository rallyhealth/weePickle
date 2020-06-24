package com.rallyhealth.weepickle.v1.implicits.internal

import com.rallyhealth.weepickle.v1.implicits.{discriminator, dropDefault, key}

import scala.language.experimental.macros
import scala.language.{existentials, higherKinds}

/**
  * Implementation of macros used by weepickle to serialize and deserialize
  * case classes automatically. You probably shouldn't need to use these
  * directly, since they are called implicitly when trying to read/write
  * types you don't have a To/From in scope for.
  */
@deprecated("Use MacroImplicits instead (has better visibility definitions)", "v1.1.0")
object Macros {

  trait DeriveDefaults[M[_]] {
    val c: scala.reflect.macros.blackbox.Context
    def fail(tpe: c.Type, s: String) = c.abort(c.enclosingPosition, s)

    import c.universe._
    def companionTree(tpe: c.Type): Either[String, Tree] = {
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
    def getArgSyms(tpe: c.Type) = {
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

    /*
     * Wrap all the argument stuff up in a case class for easy processing
     */
    private[internal] case class Argument(
      i: Int,
      raw: String,
      mapped: String,
      argType: Type,
      hasDefault: Boolean,
      assumeDefaultNone: Boolean,
      omitDefault: Boolean,
      default: c.Tree,
      localTo: TermName,
      aggregate: TermName
    ) {
      def writingCheckDefault: Boolean = hasDefault && omitDefault
      def readingCheckDefault: Boolean = hasDefault || assumeDefaultNone
    }

    private[internal] object Argument {

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
        companion: c.Tree,
        index: Int,
        isParamWithDefault: Boolean,
        assumeDefaultNone: Boolean
      ): c.Tree = {
        val defaultName = TermName("apply$default$" + (index + 1))
        if (!isParamWithDefault) {
          if (assumeDefaultNone) q"${TermName("None")}"
          else q"null"
        } else {
          q"$companion.$defaultName"
        }
      }

      private def argTypeFromSignature(tpe: Type, typeParams: List[Symbol], t: Type) = {
        val concrete = tpe.dealias.asInstanceOf[TypeRef].args
        if (t.typeSymbol != definitions.RepeatedParamClass) {
          t.substituteTypes(typeParams, concrete)
        } else {
          val TypeRef(pref, sym, _) = typeOf[Seq[Int]]
          internal.typeRef(pref, sym, t.asInstanceOf[TypeRef].args)
        }
      }

      def apply(tpe: Type, companion: c.Tree, typeParams: List[Symbol])(indexedArg: (Symbol, Int)): Argument = {
        val (argSym, index) = indexedArg
        val isParamWithDefault = argSym.asTerm.isParamWithDefault
        // include .erasure to represent varargs as "Seq", not "Whatever*"
        val isOptionWithoutDefault = !isParamWithDefault &&
          argSym.typeSignature.erasure.typeConstructor.toString == "Option"

        new Argument(
          i = index,
          raw = argSym.name.toString,
          mapped = customKey(argSym).getOrElse(argSym.name.toString),
          argType = argTypeFromSignature(tpe, typeParams, argSym.typeSignature),
          hasDefault = isParamWithDefault,
          assumeDefaultNone = isOptionWithoutDefault,
          omitDefault = shouldDropDefault(tpe.typeSymbol, argSym),
          default = deriveDefault(companion, index, isParamWithDefault, isOptionWithoutDefault),
          localTo = TermName("localTo" + index),
          aggregate = TermName("aggregated" + index)
        )
      }
    }

    /**
      * If a super-type is generic, find all the subtypes, but at the same time
      * fill in all the generic type parameters that are based on the super-type's
      * concrete type
      */
    def fleshedOutSubtypes(tpe: Type) = {
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

    def deriveObject(tpe: c.Type): c.universe.Tree = {
      val mod = tpe.typeSymbol.asClass.module
      val symTab = c.universe.asInstanceOf[reflect.internal.SymbolTable]
      val pre = tpe.asInstanceOf[symTab.Type].prefix.asInstanceOf[Type]
      val mod2 = c.universe.internal.gen.mkAttributedRef(pre, mod)

      annotate(tpe)(wrapObject(mod2))

    }
    def mergeTrait(subtrees: Seq[Tree], subtypes: Seq[Type], targetType: c.Type): Tree

    def derive(tpe: c.Type) = {
      if (tpe.typeSymbol.asClass.isTrait || (tpe.typeSymbol.asClass.isAbstract && !tpe.typeSymbol.isJava)) {
        val derived = deriveTrait(tpe)
        derived
      } else if (tpe.typeSymbol.isModuleClass) deriveObject(tpe)
      else deriveClass(tpe)
    }
    def deriveTrait(tpe: c.Type): c.universe.Tree = {
      val clsSymbol = tpe.typeSymbol.asClass

      if (!clsSymbol.isSealed) {
        fail(tpe, s"[error] The referenced trait [[${clsSymbol.name}]] must be sealed.")
      } else if (clsSymbol.knownDirectSubclasses.filter(!_.toString.contains("<local child>")).isEmpty) {
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

    def typeclass: c.WeakTypeTag[M[_]]

    def typeclassFor(t: Type) = {
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

    def deriveClass(tpe: c.Type) = {
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

    /** If there is a sealed base class, annotate the derived tree in the JSON
      * representation with a class label.
      */
    def annotate(tpe: c.Type)(derived: c.universe.Tree) = {
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

    def customKey(sym: c.Symbol): Option[String] = {
      sym.annotations
        .find(_.tree.tpe == typeOf[key])
        .flatMap(_.tree.children.tail.headOption)
        .map { case Literal(Constant(s)) => s.toString }
    }

    def customDiscriminator(sym: c.Symbol): Option[String] = {
      sym.annotations
        .find(_.tree.tpe == typeOf[discriminator])
        .flatMap(_.tree.children.tail.headOption)
        .map { case Literal(Constant(s)) => s.toString }
    }

    def wrapObject(obj: Tree): Tree

    def wrapCaseN(companion: Tree, args: Seq[Argument], targetType: c.Type, varargs: Boolean): Tree
  }

  abstract class Reading[M[_]] extends DeriveDefaults[M] {
    val c: scala.reflect.macros.blackbox.Context
    import c.universe._
    def wrapObject(t: c.Tree) = q"new ${c.prefix}.SingletonR($t)"

    def wrapCaseN(companion: c.Tree, args: Seq[Argument], targetType: c.Type, varargs: Boolean) = {
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
              currentIndex = ${c.prefix}.objectAttributeKeyReadMap(s.toString).toString match {
                case ..${for (arg <- args)
        yield cq"${arg.mapped} => ${arg.i}"}
                case _ => -1
              }
            }

            def visitEnd() = {
              ..${for (arg <- args if arg.readingCheckDefault)
        yield q"if ((found & (1L << ${arg.i})) == 0) {found |= (1L << ${arg.i}); storeAggregatedValue(${arg.i}, ${arg.default})}"}

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
    def mergeTrait(subtrees: Seq[Tree], subtypes: Seq[Type], targetType: c.Type): Tree = {
      q"${c.prefix}.To.merge[$targetType](..$subtrees)"
    }
  }

  abstract class Writing[M[_]] extends DeriveDefaults[M] {
    val c: scala.reflect.macros.blackbox.Context
    import c.universe._
    def wrapObject(obj: c.Tree) = q"new ${c.prefix}.SingletonW($obj)"
    def findUnapply(tpe: Type) = {
      val (companion, _, _) = getArgSyms(tpe).fold(
        errMsg => c.abort(c.enclosingPosition, errMsg),
        x => x
      )
      Seq("unapply", "unapplySeq")
        .map(TermName(_))
        .find(companion.tpe.member(_) != NoSymbol)
        .getOrElse(
          c.abort(
            c.enclosingPosition,
            "None of the following methods " +
              "were defined: unapply, unapplySeq"
          )
        )
    }

    def internal = q"${c.prefix}.Internal"
    def wrapCaseN(companion: c.Tree, args: Seq[Argument], targetType: c.Type, varargs: Boolean) = {
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
        if (arg.writingCheckDefault) q"""if (v.${TermName(arg.raw)} != ${arg.default}) $snippet"""
        else snippet
      }
      q"""
        new ${c.prefix}.CaseW[$targetType]{
          def length(v: $targetType) = {
            var n = 0
            ..${for (arg <- args)
        yield {
          if (!arg.writingCheckDefault) q"n += 1"
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
    def mergeTrait(subtree: Seq[Tree], subtypes: Seq[Type], targetType: c.Type): Tree = {
      q"${c.prefix}.From.merge[$targetType](..$subtree)"
    }
  }
  def macroRImpl[T, R[_]](
    c0: scala.reflect.macros.blackbox.Context
  )(implicit e1: c0.WeakTypeTag[T], e2: c0.WeakTypeTag[R[_]]): c0.Expr[R[T]] = {
    val res = new Reading[R] {
      val c: c0.type = c0
      def typeclass = e2
    }.derive(e1.tpe)
//    println(res)
    c0.Expr[R[T]](res)
  }

  def macroTImpl[T, W[_]](
    c0: scala.reflect.macros.blackbox.Context
  )(implicit e1: c0.WeakTypeTag[T], e2: c0.WeakTypeTag[W[_]]): c0.Expr[W[T]] = {
    val res = new Writing[W] {
      val c: c0.type = c0
      def typeclass = e2
    }.derive(e1.tpe)
//    println(res)
    c0.Expr[W[T]](res)
  }
}
