package com.rallyhealth.weepickle.v1

import com.rallyhealth.weepickle.v1.core.{FromInput, Visitor}

import java.time.ZonedDateTime
import java.util.concurrent.ConcurrentHashMap
import scala.jdk.CollectionConverters._
import scala.util.control.NoStackTrace

abstract class LowPriorityImplicits extends AttributeTagged {

  implicit val FromFromInput: From[FromInput] = new From[FromInput] {
    override def transform0[Out](in: FromInput, out: Visitor[_, Out]): Out = in.transform(out)
  }

  implicit val FromZonedDateTime: From[ZonedDateTime] = FromString.comap[ZonedDateTime](_.toString)
  implicit val ToZonedDateTime: To[ZonedDateTime] = new MapStringTo(s => ZonedDateTime.parse(s.toString))

  //  Note that, in Scala 3, using E#Value is not allowed: "E is not a legal path since it is not a concrete type".
  //  Parameterizing and using evidence for type equivilance to get around this. TBD if there is a better way.

  private def toEnumerationName[E <: scala.Enumeration, V <: scala.Enumeration#Value](e: E)(
    implicit ev: V =:= e.Value): To[V] = {
    val cache = new ConcurrentHashMap[String, V] // mitigate withName() slowness.
    var lastVset: e.ValueSet = null

    ToString.map { s =>
      val vset: e.ValueSet = e.values // exploit that e.values returns a cached instance unless new values are added
      if (vset ne lastVset) {
        // add any new values since last time
        vset.foreach(v => cache.put(v.toString, v.asInstanceOf[V]))
        lastVset = vset
      }

      val enumValue: V = cache.get(s) // 68x faster than withName()
      if (enumValue eq null) {
        throw new NoSuchElementException(s"'$s' is not a valid value of $e") with NoStackTrace
      }
      enumValue
    }
  }

  private def fromEnumerationName[E <: scala.Enumeration, V <: scala.Enumeration#Value](e: E)(
    implicit ev: V =:= e.Value): From[V] = {
    val cache = new ConcurrentHashMap[V, String].asScala // mitigate toString() slowness.
    FromString.comap(v => cache.getOrElseUpdate(v, v.toString))
  }

  def fromToEnumerationName[E <: scala.Enumeration, V <: scala.Enumeration#Value](e: E)(
    implicit ev: V =:= e.Value): FromTo[V] = {
    FromTo.join(toEnumerationName(e), fromEnumerationName(e))
  }

  def fromToEnumerationId[E <: scala.Enumeration, V <: scala.Enumeration#Value](e: E)(
    implicit ev: V =:= e.Value): FromTo[V] = {
    FromTo.join(ToInt.map(e.apply(_).asInstanceOf[V]), FromInt.comap(_.id))
  }

  //  Native Scala 3 enums extend scala.reflect.Enum transparently.
  //  By name encoding provided in default derives, so only id encoding provided here.
  // TODO: do we even need this? If we do, should it be here (it isn't implicit)?

  abstract class FromToById[V <: scala.reflect.Enum] {
    // implemented by the companion of an enum
    def fromOrdinal(i: Int): V

    implicit val fromId: From[V] = FromInt.comap(_.ordinal)
    implicit val toId: To[V] = ToInt.map(fromOrdinal)
  }
}
