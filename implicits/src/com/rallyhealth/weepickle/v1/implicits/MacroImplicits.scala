package com.rallyhealth.weepickle.v0.implicits

import scala.language.experimental.macros

/**
  * Stupid hacks to work around scalac not forwarding macro type params properly
  */
object MacroImplicits{
  def dieIfNothing[T: c.WeakTypeTag]
  (c: scala.reflect.macros.blackbox.Context)
  (name: String) = {
    if (c.weakTypeOf[T] =:= c.weakTypeOf[Nothing]) {
      c.abort(
        c.enclosingPosition,
        s"weepickle is trying to infer a $name[Nothing]. That probably means you messed up"
      )
    }
  }
  def applyR[T](c: scala.reflect.macros.blackbox.Context)
               (implicit e: c.WeakTypeTag[T]): c.Expr[T] = {
    import c.universe._
    dieIfNothing[T](c)("Reader")
    c.Expr[T](q"${c.prefix}.macroR0[$e, ${c.prefix}.Reader]")
  }
  def applyW[T](c: scala.reflect.macros.blackbox.Context)
               (implicit e: c.WeakTypeTag[T]): c.Expr[T] = {
    import c.universe._
    dieIfNothing[T](c)("Writer")
    c.Expr[T](q"${c.prefix}.macroW0[$e, ${c.prefix}.Writer]")
  }

  def applyRW[T](c: scala.reflect.macros.blackbox.Context)
                (implicit e: c.WeakTypeTag[T]): c.Expr[T] = {
    import c.universe._
    dieIfNothing[T](c)("Writer")
    c.Expr[T](q"${c.prefix}.ReaderWriter.join(${c.prefix}.macroR, ${c.prefix}.macroW)")
  }

}
trait MacroImplicits{ this: com.rallyhealth.weepickle.v0.core.Types =>
  implicit def macroSingletonR[T <: Singleton]: Reader[T] = macro MacroImplicits.applyR[T]
  implicit def macroSingletonW[T <: Singleton]: Writer[T] = macro MacroImplicits.applyW[T]
  implicit def macroSingletonRW[T <: Singleton]: ReaderWriter[T] = macro MacroImplicits.applyRW[T]
  def macroR[T]: Reader[T] = macro MacroImplicits.applyR[T]
  def macroW[T]: Writer[T] = macro MacroImplicits.applyW[T]
  def macroRW[T]: ReaderWriter[T] = macro MacroImplicits.applyRW[ReaderWriter[T]]

  def macroR0[T, M[_]]: Reader[T] = macro internal.Macros.macroRImpl[T, M]
  def macroW0[T, M[_]]: Writer[T] = macro internal.Macros.macroWImpl[T, M]
}

