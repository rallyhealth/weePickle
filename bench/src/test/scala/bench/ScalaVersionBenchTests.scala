package bench

import bench.ScalaVersionBench.Data
import com.rallyhealth.weepickle.v1.WeePickle
import upickle.core.Visitor
import utest._

object ScalaVersionBenchTests extends TestSuite {

  val tests = Tests {
    test("fromSample") {
      new ScalaVersionBench()
        .testableFromSample(
          WeePickle.to[Seq[Data]]
          .map { data =>
            data ==> ScalaVersionBench.benchmarkSampleData
            "assertion reached"
          }
      ) ==> "assertion reached"
    }

    test("toSample") {
      new ScalaVersionBench().toSample ==> ScalaVersionBench.benchmarkSampleData
    }
  }
}
