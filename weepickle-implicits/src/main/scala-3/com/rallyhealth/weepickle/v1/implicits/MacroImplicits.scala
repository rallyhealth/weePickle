package com.rallyhealth.weepickle.v1.implicits

import deriving.Mirror
import scala.compiletime.erasedValue
import scala.reflect.ClassTag
import com.rallyhealth.weepickle.v1.core.{Annotator, Types}

trait MacroImplicits extends CaseClassToPiece with CaseClassFromPiece:
  this: Annotator with Types =>

  inline def macroFromTo[T: ClassTag](using Mirror.Of[T]): FromTo[T] =
    FromTo.join(
      macroTo[T],
      macroFrom[T]
    )
  end macroFromTo

  inline def macroEnumFromTo[T: ClassTag](using Mirror.Of[T]): FromTo[T] =
    FromTo.join(
      macroEnumTo[T],
      macroEnumFrom[T]
    )
  end macroEnumFromTo


  // Usually, we would use an extension method to add `derived` to FromTo's
  // companion object. Something along the lines of:
  //
  //   extension [T](r: FromTo.type)
  //     inline def derived(using Mirror.Of[T]): FromTo[T] = macroFromTo[T]
  //
  // Unfortunately however, the above does not work for typeclass derivation.
  // Consider the following:
  //
  //   case class Foo() derives FromTo
  //
  // which is syntax sugar for:
  //
  //   object Foo:
  //     given FromTo[Foo] = FromTo.derived
  //
  // Now, since the type parameter of the extension must come after `extension`
  // and is not allowed to be part of the method itself, the compiler cannot
  // infer the correct type, and hence the extension lookup fails.
  //
  // As is mentioned here, https://dotty.epfl.ch/docs/reference/contextual/extension-methods.html#generic-extensions,
  // this limitation may be lifted in the future:
  //
  // > Note: Type parameters have to be given after the extension keyword; they
  // > cannot be given after the def. This restriction might be lifted in the
  // > future once we support multiple type parameter clauses in a method. By
  // > contrast, using clauses can be defined for the extension as well as per
  // > def.
  //
  // Until that is the case, we'll have to resort to using Scala 2's implicit
  // classes to emulate extension methods for deriving readers and writers.
  implicit class FromToExtension(r: FromTo.type):
    inline def derived[T](using Mirror.Of[T], ClassTag[T]): FromTo[T] = inline erasedValue[T] match
      case _: scala.reflect.Enum => macroEnumFromTo[T]
      case _ => macroFromTo[T]
  end FromToExtension

end MacroImplicits
