package com.rallyhealth.weejson.v1.play

import com.rallyhealth.weepickle.v1.WeePickle._
import com.rallyhealth.weejson.v1.play.PlayJsonImplicits._
import play.api.libs.json.{Json, OFormat}

case class PlayCiphertext(ciphertext: String)

object PlayCiphertext {

  import WeePickleConversions._
  implicit val format: OFormat[PlayCiphertext] = Json.format
}

case class WeePickleEnvelope(enveloped: PlayCiphertext)

object WeePickleEnvelope {

  import PlayJsonConversions._
  implicit val pickler: FromTo[WeePickleEnvelope] = macroFromTo
}

case class WeePickleCiphertext(ciphertext: String)

object WeePickleCiphertext {

  import PlayJsonConversions._
  implicit val pickler: FromTo[WeePickleCiphertext] = macroFromTo
}

case class PlayEnvelope(enveloped: WeePickleCiphertext)

object PlayEnvelope {

  import WeePickleConversions._
  implicit val format: OFormat[PlayEnvelope] = Json.format

}
