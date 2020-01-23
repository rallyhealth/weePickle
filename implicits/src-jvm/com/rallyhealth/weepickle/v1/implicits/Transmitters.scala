package com.rallyhealth.weepickle.v1.implicits

import java.time._
import java.util.Date

import com.rallyhealth.weepickle.v1.core.Visitor

trait Transmitters extends DefaultTransmitters {

  implicit val LocalDateTransmitter: Transmitter[LocalDate] = StringWriter.comap[LocalDate](_.toString)
  implicit val LocalTimeTransmitter: Transmitter[LocalTime] = StringWriter.comap[LocalTime](_.toString)
  implicit val LocalDateTimeTransmitter: Transmitter[LocalDateTime] = StringWriter.comap[LocalDateTime](_.toString)
  implicit val OffsetDateTimeTransmitter: Transmitter[OffsetDateTime] = StringWriter.comap[OffsetDateTime](_.toString)
  implicit val InstantTransmitter: Transmitter[Instant] = new Transmitter[Instant] {
    override def transmit0[V](v: Instant, out: Visitor[_, V]): V = out.visitTimestamp(v)
  }
  implicit val DateTransmitter: Transmitter[Date] = InstantTransmitter.comap(_.toInstant)
}
