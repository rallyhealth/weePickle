package com.rallyhealth.weepickle.v1.implicits

import scala.annotation.StaticAnnotation

/**
  * If the value of the annotated field .equals() the default value at runtime,
  * then the macro-generated Transmitter will omit both the key and value from the serialized blob.
  *
  * ==Example==
  * {{{
  *  case class FooDefault(
  *    @dropDefault i: Int = 10,
  *    @dropDefault s: String = "lol"
  *  )
  *
  *  write(FooDefault(i = 11, s = "lol"))  ==> """{"i":11}"""
  *  write(FooDefault(i = 10, s = "lol"))  ==> """{}"""
  *  write(FooDefault())                   ==> """{}"""
  * }}}
  */
class dropDefault extends StaticAnnotation
