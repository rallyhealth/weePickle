package com.rallyhealth.weepickle.v1.implicits

import java.time._
import java.util.Date

import com.rallyhealth.weepickle.v1.core.{Abort, Annotator}

import scala.util.{Failure, Success, Try}

trait Tos extends DefaultTos { this: Annotator =>

  implicit val ToLocalDate: To[LocalDate] = new MapStringTo(s => LocalDate.parse(s.toString))
  implicit val ToLocalTime: To[LocalTime] = new MapStringTo(s => LocalTime.parse(s.toString))
  implicit val ToLocalDateTime: To[LocalDateTime] = new MapStringTo(
    s => LocalDateTime.parse(s.toString)
  )
  implicit val ToOffsetDateTime: To[OffsetDateTime] = new MapStringTo(
    s => OffsetDateTime.parse(s.toString)
  )
  implicit val ToInstant: To[Instant] = new SimpleTo[Instant] {
    override def expectedMsg: String = "expected timestamp"
    override def visitTimestamp(instant: Instant): Instant = instant
    override def visitString(cs: CharSequence): Instant = Instant.parse(cs.toString)
    override def visitInt64(i: Long): Instant = Instant.ofEpochMilli(i)
    override def visitFloat64String(s: String): Instant = {
      Try(s.toDouble) match {
        case Success(d: Double) if d == d.toLong => visitInt64(d.toLong)
        case Success(d: Double)                  => throw new Abort(s"$expectedMsg got float")
        case Failure(_)                          => throw new Abort(s"$expectedMsg got strange number")
      }
    }
    override def visitFloat64StringParts(s: CharSequence, decIndex: Int, expIndex: Int): Instant = {
      if (decIndex == -1 && expIndex == -1) visitInt64(s.toString.toLong) // fast path
      else visitFloat64String(s.toString) // likely invalid path
    }
  }
  implicit val ToDate: To[Date] = ToInstant.map(i => new Date(i.toEpochMilli))
  implicit val ToZoneId: To[ZoneId] = new MapStringTo(
    s => ZoneId.of(s.toString)
  )
}
