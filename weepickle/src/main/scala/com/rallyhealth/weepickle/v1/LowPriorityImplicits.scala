package com.rallyhealth.weepickle.v1

import com.rallyhealth.weepickle.v1.core.{FromInput, Visitor}

import java.time.ZonedDateTime
import java.util.concurrent.ConcurrentHashMap
import scala.jdk.CollectionConverters._
import scala.util.control.NoStackTrace

abstract class LowPriorityImplicits
  extends AttributeTagged {

  implicit val FromFromInput: From[FromInput] = new From[FromInput] {
    override def transform0[Out](in: FromInput, out: Visitor[_, Out]): Out = in.transform(out)
  }

  implicit val FromZonedDateTime: From[ZonedDateTime] = FromString.comap[ZonedDateTime](_.toString)
  implicit val ToZonedDateTime: To[ZonedDateTime] = new MapStringTo(s => ZonedDateTime.parse(s.toString))

  private def toEnumerationName[E <: scala.Enumeration](e: E): To[E#Value] = {
    val cache = new ConcurrentHashMap[String, E#Value] // mitigate withName() slowness.
    var lastVset: E#ValueSet = null

    ToString.map { s =>
      val vset = e.values // exploit that e.values returns a cached instance unless changed
      if (vset ne lastVset) {
        // add any new values since last time
        vset.foreach(v => cache.put(v.toString, v))
        lastVset = vset
      }

      val enum = cache.get(s) // 68x faster than withName()
      if (enum eq null) {
        throw new NoSuchElementException(s"'$s' is not a valid value of $e") with NoStackTrace
      }
      enum
    }
  }

  private def fromEnumerationName[E <: scala.Enumeration](e: E): From[E#Value] = {
    val cache = new ConcurrentHashMap[E#Value, String].asScala // mitigate withName() slowness.
    FromString.comap(v => cache.getOrElseUpdate(v, v.toString))
  }

  def fromToEnumerationName[E <: scala.Enumeration](e: E): FromTo[E#Value] = {
    FromTo.join(toEnumerationName(e), fromEnumerationName(e))
  }

  def fromToEnumerationId[E <: scala.Enumeration](e: E): FromTo[E#Value] = {
    FromTo.join(ToInt.map(e.apply(_)), FromInt.comap(_.id))
  }
}
