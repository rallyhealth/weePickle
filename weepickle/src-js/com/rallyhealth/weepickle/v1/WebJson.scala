package com.rallyhealth.weepickle.v1

import scala.scalajs.js

trait WebJson extends com.rallyhealth.weepickle.v1.core.Types {
  object web {
    def read[T: Receiver](s: String) = {
      com.rallyhealth.weejson.v1.WebJson.transform(js.JSON.parse(s), implicitly[Receiver[T]])
    }

    def write[T: Transmitter](t: T, indent: Int = -1) = {
      js.JSON.stringify(implicitly[Transmitter[T]].transmit())
    }
  }
}
