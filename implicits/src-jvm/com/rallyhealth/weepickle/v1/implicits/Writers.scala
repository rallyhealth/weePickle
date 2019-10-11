package com.rallyhealth.weepickle.v0.implicits

import java.time._

trait Writers extends DefaultWriters {

  implicit val LocalDateWriter: Writer[LocalDate] = StringWriter.comap[LocalDate](_.toString)
  implicit val LocalTimeWriter: Writer[LocalTime] = StringWriter.comap[LocalTime](_.toString)
  implicit val LocalDateTimeWriter: Writer[LocalDateTime] = StringWriter.comap[LocalDateTime](_.toString)
  implicit val OffsetDateTimeWriter: Writer[OffsetDateTime] = StringWriter.comap[OffsetDateTime](_.toString)
  implicit val InstantWriter: Writer[Instant] = StringWriter.comap[Instant](_.toString)
}
