package com.rallyhealth.weepickle.v1

import com.rallyhealth.weepickle.v1.WeePickle.{FromTo, macroFromTo}

/**
 * @see https://github.com/com-lihaoyi/upickle/issues/345
 */
case class SingleInt(num: Int)
object SingleInt {
  implicit val pickler: FromTo[SingleInt] = macroFromTo
}
