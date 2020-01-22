package com.rallyhealth.weejson.v1.jackson

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.base.ParserBase
import com.rallyhealth.weepickle.v1.core.TransformException

object JsonParserException {

  def apply(msg: String, parser: JsonParser, t: Throwable = null): Throwable = {
    val parserBase = Option(parser).collect { case pb: ParserBase => pb }
    new TransformException(
      shortMsg = msg,
      jsonPointer = splunkFriendly(parser.getParsingContext.pathAsPointer().toString),
      index = parserBase.map(_.getTokenCharacterOffset),
      line = parserBase.map(_.getTokenLineNr),
      col = parserBase.map(_.getTokenColumnNr),
      token = parserBase.map(_.getCurrentToken.toString()),
      cause = t
    )
  }

  private def splunkFriendly(value: String): String = {
    if (value.contains('"') || value.contains(" ")) {
      "\"" + value.replaceAllLiterally("\"", "\\\"") + "\""
    } else {
      value
    }
  }
}
