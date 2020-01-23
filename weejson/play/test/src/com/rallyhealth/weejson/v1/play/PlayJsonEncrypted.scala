package com.rallyhealth.weejson.v1.play

import com.rallyhealth.weepickle.v1.WeePickle
import com.rallyhealth.weejson.v1.play.PlayJsonImplicits._
import play.api.libs.json.Json

case class PlayCiphertext(ciphertext: String)

object PlayCiphertext {

  import WeePickleConversions._
  implicit val format = Json.format[PlayCiphertext]
}

case class WeePickleEnvelope(enveloped: PlayCiphertext)

object WeePickleEnvelope {

  import PlayJsonConversions._
  implicit val rw = WeePickle.macroX[WeePickleEnvelope]
}

case class WeePickleCiphertext(ciphertext: String)

object WeePickleCiphertext {

  import PlayJsonConversions._
  implicit val rw = WeePickle.macroX[WeePickleCiphertext]
}

case class PlayEnvelope(enveloped: WeePickleCiphertext)

object PlayEnvelope {

  import WeePickleConversions._
  implicit val format = Json.format[PlayEnvelope]

}
