package bench

import com.rallyhealth.weejson.v1.BufferedValue._
import utest._

object JsoniterScalaBenchTests extends TestSuite {

  val tests = Tests {
    val bench = new JsoniterScalaBench()
    test("pi")(bench.pi ==> Num("-3.14", 2, -1))
  }
}
