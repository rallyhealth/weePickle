package com.rallyhealth.weepickle.v1.implicits

import scala.annotation.StaticAnnotation

/**
  * If the value of the annotated field .equals() the default value at runtime
  * after it has been translated from the source (using an implicit From[T]),
  * then the macro-generated From will omit both the key and value from the serialized blob.
  *
  * This is useful if the implicit From applies some extra (potentially expensive) logic where
  * the source value does not match the default, but it may arrive at it through translation.
  *
  * dropDefault is still the available where pre-transformed data are considered when omitting
  * defaults. This is preferable if From[T] is expensive, unavailable, or otherwise impractical.
  *
  * ==Example==
  * {{{
  *  // Customizes the behavior of transforming an Option[String]
  *  implicit def OptionStringFrom: From[Option[String]] = OptionFrom[String].comap {
  *    case Some("") => None
  *    case other    => other
  *  }
  *
  *  case class FooDefault(
  *    @dropDefault            s1: Option[String] = None,
  *    @dropTransformedDefault s2: Option[String] = None
  *  )
  *
  *  write(FooDefault(s1 = Some("lol"), s2 = Some("lol")))  ==> """{"s1": "lol", "s2": "lol"}"""
  *  write(FooDefault(s1 = Some(""),    s2 = Some("")))     ==> """{"s1": ""}"""
  *  write(FooDefault(s1 = None,        s2 = None))         ==> """{}"""
  * }}}
  */
class dropTransformedDefault extends StaticAnnotation
