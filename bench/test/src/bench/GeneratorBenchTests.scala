package bench

import com.rallyhealth.weepack.v1.FromMsgPack
import com.rallyhealth.weepickle.v1.Common._
import com.rallyhealth.weepickle.v1.WeePickle.ToScala
import utest._

object GeneratorBenchTests extends TestSuite {

  val tests = Tests {
    val bench = new GeneratorBench()

    test("jsonBytes")(bench.jsonBytes ==> benchmarkSampleJsonBytes)
    test("jsonString")(bench.jsonString ==> benchmarkSampleJson)
    test("msgpackScala")(bench.msgpackScala ==> benchmarkSampleMsgPack)
    test("msgpackJackson") {
      // jackson encodes as 0xce uint 32, rather than 0xd2 int 32. okay.
      FromMsgPack(bench.msgpackJackson).transform(ToScala[Seq[Data]]) ==> benchmarkSampleData
    }
    test("smile")(bench.smile ==> SmileBytes.smileBytes)
  }
}
