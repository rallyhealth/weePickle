package com.rallyhealth.weepickle.v1.implicits

import java.time._
import java.util.Date

import com.rallyhealth.weepickle.v1.core.Abort

import scala.util.{Failure, Success, Try}

trait Receivers extends DefaultReceivers {

  implicit val LocalDateReceiver: Receiver[LocalDate] = new MapStringReceiver(s => LocalDate.parse(s.toString))
  implicit val LocalTimeReceiver: Receiver[LocalTime] = new MapStringReceiver(s => LocalTime.parse(s.toString))
  implicit val LocalDateTimeReceiver: Receiver[LocalDateTime] = new MapStringReceiver(
    s => LocalDateTime.parse(s.toString)
  )
  implicit val OffsetDateTimeReceiver: Receiver[OffsetDateTime] = new MapStringReceiver(
    s => OffsetDateTime.parse(s.toString)
  )
  implicit val InstantReceiver: Receiver[Instant] = new SimpleReceiver[Instant] {
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
  implicit val DateReceiver: Receiver[Date] = InstantReceiver.map(i => new Date(i.toEpochMilli))
}
