package com.rallyhealth.weepickle.v0

import scala.scalajs.js

trait WebJson extends com.rallyhealth.weepickle.v0.core.Types {
  object web {
    def read[T: Reader](s: String) = {
      com.rallyhealth.ujson.v0.WebJson.transform(js.JSON.parse(s), implicitly[Reader[T]])
    }

    def write[T: Writer](t: T, indent: Int = -1) = {
      js.JSON.stringify(implicitly[Writer[T]].write(com.rallyhealth.ujson.v0.WebJson.Builder, t))
    }
  }
}
