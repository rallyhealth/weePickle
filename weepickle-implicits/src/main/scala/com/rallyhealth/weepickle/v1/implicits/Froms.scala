package com.rallyhealth.weepickle.v1.implicits

import com.rallyhealth.weepickle.v1.core.{Annotator, Visitor}

import java.time._
import java.util.Date

trait Froms extends DefaultFroms { this: Annotator =>

  implicit val FromLocalDate: From[LocalDate] = FromString.comap[LocalDate](_.toString)
  implicit val FromLocalTime: From[LocalTime] = FromString.comap[LocalTime](_.toString)
  implicit val FromLocalDateTime: From[LocalDateTime] = FromString.comap[LocalDateTime](_.toString)
  implicit val FromOffsetDateTime: From[OffsetDateTime] = FromString.comap[OffsetDateTime](_.toString)
  implicit val FromInstant: From[Instant] = new From[Instant] {
    override def transform0[V](v: Instant, out: Visitor[_, V]): V = out.visitTimestamp(v)
  }
  implicit val FromDate: From[Date] = FromInstant.comap(_.toInstant)
}
