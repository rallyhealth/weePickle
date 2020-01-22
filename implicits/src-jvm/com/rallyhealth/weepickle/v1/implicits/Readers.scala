package com.rallyhealth.weepickle.v1.implicits

import java.time._
import java.util.Date

import com.rallyhealth.weepickle.v1.core.Abort

import scala.util.{Failure, Success, Try}

trait Readers extends DefaultReaders {

  implicit val LocalDateReader: Reader[LocalDate] = new MapStringReader(s => LocalDate.parse(s.toString))
  implicit val LocalTimeReader: Reader[LocalTime] = new MapStringReader(s => LocalTime.parse(s.toString))
  implicit val LocalDateTimeReader: Reader[LocalDateTime] = new MapStringReader(s => LocalDateTime.parse(s.toString))
  implicit val OffsetDateTimeReader: Reader[OffsetDateTime] = new MapStringReader(s => OffsetDateTime.parse(s.toString))
  implicit val InstantReader: Reader[Instant] = new SimpleReader[Instant] {
    override def expectedMsg: String = "expected timestamp"
    override def visitTimestamp(instant: Instant, index: Int): Instant = instant
    override def visitString(s: CharSequence, index: Int): Instant = Instant.parse(s.toString)
    override def visitInt64(i: Long, index: Int): Instant = Instant.ofEpochMilli(i)
    override def visitFloat64String(s: String, index: Int): Instant = {
      Try(s.toDouble) match {
        case Success(d: Double) if d == d.toLong => visitInt64(d.toLong, index)
        case Success(d: Double) => throw new Abort(s"$expectedMsg got float", index)
        case Failure(_) => throw new Abort(s"$expectedMsg got strange number", index)
      }
    }
    override def visitFloat64StringParts(s: CharSequence, decIndex: Int, expIndex: Int, index: Int): Instant = {
      if (decIndex == -1 && expIndex == -1) visitInt64(s.toString.toLong, index) // fast path
      else visitFloat64String(s.toString, index) // likely invalid path
    }

  }
  implicit val DateReader: Reader[Date] = InstantReader.map(i => new Date(i.toEpochMilli))
}
