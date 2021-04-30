package bench

import com.fasterxml.jackson.dataformat.smile.SmileFactory
import com.rallyhealth.weejson.v1.jackson.{JsonGeneratorOps, JsonParserOps}

object DefaultSmileFactory {

  val Instance = new SmileFactory() // javadoc suggests reusing this
}

object ToSmile extends JsonGeneratorOps(DefaultSmileFactory.Instance)

object FromSmile extends JsonParserOps(DefaultSmileFactory.Instance)
