package com.rallyhealth.weepickle.v1.example

import java.time._
import java.util.Date

import acyclic.file
import com.rallyhealth.weepickle.v1.{TestUtil, WeePickle}
import utest._

object JvmPrimitiveTests extends TestSuite {
  import TestUtil._

  def tests = Tests {
    test("Date/Time") {
      test("LocalDate") { rw(LocalDate.of(2000, 1, 1), "\"2000-01-01\"") }
      test("LocalTime") { rw(LocalTime.parse("00:00"), "\"00:00\"") }
      test("LocalDateTime") { rw(LocalDateTime.parse("2000-01-01T00:00:00"), "\"2000-01-01T00:00:00\"") }
      test("OffsetDateTime") { rw(OffsetDateTime.parse("2007-12-03T10:15:30+01:00"), "\"2007-12-03T10:15:30+01:00\"") }
      test("ZonedDateTime") {rw(ZonedDateTime.parse("2000-01-01T00:00:00.000-06:00[America/Chicago]"), "\"2000-01-01T00:00:00.000-06:00[America/Chicago]\"") }
      test("Instant") {
        test("String") { rw(Instant.parse("2015-05-03T10:15:30Z"), "\"2015-05-03T10:15:30Z\"") }
        test("Millis") {
          WeePickle.ToInstant.visitInt64(1579664660824L) ==> Instant.parse("2020-01-22T03:44:20.824Z")
          WeePickle.ToInstant.visitFloat64String("1579664660824") ==> Instant.parse("2020-01-22T03:44:20.824Z")
          WeePickle.ToInstant.visitFloat64StringParts("1579664660824", -1, -1) ==> Instant.parse(
            "2020-01-22T03:44:20.824Z"
          )
        }
      }
      test("Date") {
        rw(new Date(1579664660824L), "\"2020-01-22T03:44:20.824Z\"")
      }
    }
  }
}
