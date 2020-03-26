package com.rallyhealth.weepickle.v1.core

object VisitorImplicits {

  implicit class VisitorOps[T, J](val visitor: Visitor[T, J]) {
    def mapKeys(f: CharSequence => CharSequence): Visitor[T, J] =
      new Visitor.MapKeys[T, J](visitor, f)
  }

}
