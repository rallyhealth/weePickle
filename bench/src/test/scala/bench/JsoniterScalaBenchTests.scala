package bench

import com.rallyhealth.weejson.v1.BufferedValue._
import utest._

object JsoniterScalaBenchTests extends TestSuite {

  val tests = Tests {
    val bench = new JsoniterScalaBench()
    test("parseFloat")(bench.parseFloat ==> Num("-3.14159", 2, -1))
    test("parseInt")(bench.parseInt ==> NumLong(186282))
  }
}
