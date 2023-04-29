package com.rallyhealth.weepickle.v1.core.ops

import rallyhealth.weepickle.v1.core.ops.ConcatFromInputsBench
import utest._

object ConcatFromInputsBenchTests extends TestSuite {

  override val tests = Tests {
    test("objs")(new ConcatFromInputsBench().objs ==> 1000000)
    test("arrs")(new ConcatFromInputsBench().arrs ==> 1000000)
  }
}
