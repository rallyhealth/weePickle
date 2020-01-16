package com.rallyhealth.weepickle.v1.example

import java.time._

import acyclic.file
import com.rallyhealth.weepickle.v1.TestUtil
import utest._

object JvmPrimitiveTests extends TestSuite {
  import TestUtil._

  def tests = Tests {
    test("Date/Time") - {
      test("LocalDate") - rw(LocalDate.of(2000, 1, 1), "\"2000-01-01\"")
      test("LocalTime") - rw(LocalTime.parse("00:00"), "\"00:00\"")
      test("LocalDateTime") - rw(LocalDateTime.parse("2000-01-01T00:00:00"), "\"2000-01-01T00:00:00\"")
      test("OffsetDateTime") - rw(OffsetDateTime.parse("2007-12-03T10:15:30+01:00"), "\"2007-12-03T10:15:30+01:00\"")
      test("Instant") - rw(Instant.parse("2015-05-03T10:15:30Z"), "\"2015-05-03T10:15:30Z\"")
    }
  }
}
