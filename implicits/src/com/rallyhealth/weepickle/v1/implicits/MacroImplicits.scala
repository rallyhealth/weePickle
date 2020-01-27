package com.rallyhealth.weepickle.v1.implicits

import scala.language.experimental.macros

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

}
trait MacroImplicits { this: com.rallyhealth.weepickle.v1.core.Types =>
  implicit def macroSingletonTo[T <: Singleton]: To[T] = macro MacroImplicits.applyTo[T]
  implicit def macroSingletonFrom[F <: Singleton]: From[F] = macro MacroImplicits.applyFrom[F]
  implicit def macroSingletonFromTo[X <: Singleton]: FromTo[X] = macro MacroImplicits.applyFromTo[X]
  def macroFrom[F]: From[F] = macro MacroImplicits.applyFrom[F]
  def macroTo[T]: To[T] = macro MacroImplicits.applyTo[T]
  def macroFromTo[X]: FromTo[X] = macro MacroImplicits.applyFromTo[FromTo[X]]

  def macroTo0[T, M[_]]: To[T] = macro internal.Macros.macroRImpl[T, M]
  def macroFrom0[T, M[_]]: From[T] = macro internal.Macros.macroTImpl[T, M]
}
