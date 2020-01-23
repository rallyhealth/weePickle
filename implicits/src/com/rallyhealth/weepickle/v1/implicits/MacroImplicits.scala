package com.rallyhealth.weepickle.v1.implicits

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
    dieIfNothing[T](c)("Receiver")
    c.Expr[T](q"${c.prefix}.macroR0[$e, ${c.prefix}.Receiver]")
  }
  def applyT[T](c: scala.reflect.macros.blackbox.Context)
               (implicit e: c.WeakTypeTag[T]): c.Expr[T] = {
    import c.universe._
    dieIfNothing[T](c)("Transmitter")
    c.Expr[T](q"${c.prefix}.macroT0[$e, ${c.prefix}.Transmitter]")
  }

  def applyX[T](c: scala.reflect.macros.blackbox.Context)
                (implicit e: c.WeakTypeTag[T]): c.Expr[T] = {
    import c.universe._
    dieIfNothing[T](c)("Transmitter")
    c.Expr[T](q"${c.prefix}.Transceiver.join(${c.prefix}.macroR, ${c.prefix}.macroT)")
  }

}
trait MacroImplicits{ this: com.rallyhealth.weepickle.v1.core.Types =>
  implicit def macroSingletonR[R <: Singleton]: Receiver[R] = macro MacroImplicits.applyR[R]
  implicit def macroSingletonT[T <: Singleton]: Transmitter[T] = macro MacroImplicits.applyT[T]
  implicit def macroSingletonX[X <: Singleton]: Transceiver[X] = macro MacroImplicits.applyX[X]
  def macroT[T]: Transmitter[T] = macro MacroImplicits.applyT[T]
  def macroR[R]: Receiver[R] = macro MacroImplicits.applyR[R]
  def macroX[X]: Transceiver[X] = macro MacroImplicits.applyX[Transceiver[X]]

  def macroR0[T, M[_]]: Receiver[T] = macro internal.Macros.macroRImpl[T, M]
  def macroT0[T, M[_]]: Transmitter[T] = macro internal.Macros.macroTImpl[T, M]
}

