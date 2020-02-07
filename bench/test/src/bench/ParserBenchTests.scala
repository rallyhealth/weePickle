package bench

import com.rallyhealth.weepickle.v1.Common._
import org.openjdk.jmh.annotations.Benchmark
import utest._

object ParserBenchTests extends TestSuite {

  val tests = Tests {
    val bench = new ParserBench()
    test("all benchmarks produce the right data") {
      for {
        method <- classOf[ParserBench].getMethods
          if method.getAnnotation(classOf[Benchmark]) != null
      } {
        try {
          method.invoke(bench) ==> benchmarkSampleData
        } catch {
          case t: Throwable => throw new java.lang.AssertionError(s"Did not return correct data: $method", t)
        }
      }
    }
  }
}
