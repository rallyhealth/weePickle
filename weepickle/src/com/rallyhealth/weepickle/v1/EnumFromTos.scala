package com.rallyhealth.weepickle.v1

import java.util.concurrent.ConcurrentHashMap

import com.rallyhealth.weepickle.v1.core.Types
import com.rallyhealth.weepickle.v1.implicits.{DefaultFroms, DefaultTos}

trait EnumFromTos extends Types with DefaultTos with DefaultFroms {

  private def toEnumerationName[E <: scala.Enumeration](e: E): To[E#Value] = {
    val cache = new ConcurrentHashMap[String, E#Value] // mitigate withName() slowness.

    ToString.map { s =>
      var enum = cache.get(s) // 68x faster than withName()
      if (enum eq null) {
        enum = e.withName(s) // slow! throws on miss.
        cache.put(s, enum)
      }
      enum
    }
  }

  private def fromEnumerationName[E <: scala.Enumeration](e: E): From[E#Value] = {
    FromString.comap(_.toString)
  }

  def fromToEnumerationName[E <: scala.Enumeration](e: E): FromTo[E#Value] = {
    FromTo.join(toEnumerationName(e), fromEnumerationName(e))
  }

  def fromToEnumerationId[E <: scala.Enumeration](e: E): FromTo[E#Value] = {
    FromTo.join(ToInt.map(e.apply(_)), FromInt.comap(_.id))
  }
}
