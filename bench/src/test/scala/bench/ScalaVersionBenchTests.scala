package bench

import utest._

object ScalaVersionBenchTests extends TestSuite {

  val tests = Tests {
    test("SampleBench") {
      val bench = new SampleBench()
      test("toWeePickle")(bench.toWeePickle ==> bench.data)
      test("toUPickle")(bench.toUPickle ==> bench.data)
    }
  }
}
