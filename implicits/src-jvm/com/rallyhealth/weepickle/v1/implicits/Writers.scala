package com.rallyhealth.weepickle.v1.implicits

import java.time._
import java.util.Date

import com.rallyhealth.weepickle.v1.core.Visitor

trait Writers extends DefaultWriters {

  implicit val LocalDateWriter: Writer[LocalDate] = StringWriter.comap[LocalDate](_.toString)
  implicit val LocalTimeWriter: Writer[LocalTime] = StringWriter.comap[LocalTime](_.toString)
  implicit val LocalDateTimeWriter: Writer[LocalDateTime] = StringWriter.comap[LocalDateTime](_.toString)
  implicit val OffsetDateTimeWriter: Writer[OffsetDateTime] = StringWriter.comap[OffsetDateTime](_.toString)
  implicit val InstantWriter: Writer[Instant] = new Writer[Instant] {
    override def write0[V](out: Visitor[_, V], v: Instant): V = out.visitTimestamp(v, -1)
  }
  implicit val DateWriter: Writer[Date] = InstantWriter.comap(_.toInstant)
}
