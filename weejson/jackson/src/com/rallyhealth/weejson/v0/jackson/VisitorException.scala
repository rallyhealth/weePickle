package com.rallyhealth.weejson.v0.jackson

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.base.ParserBase

import scala.util.control.NoStackTrace

class VisitorException(msg: String, cause: Throwable) extends Exception(msg, cause) with NoStackTrace

object VisitorException {

  def apply(parser: JsonParser, t: Throwable): Throwable = {
    val sb = new StringBuilder("Parser or Visitor failure at")
    def append(k: String, v: String): Unit = sb.append(' ').append(k).append('=').append(v)

    append("jsonPointer", splunkFriendly(parser.getParsingContext.pathAsPointer().toString))
    parser match {
      case pb: ParserBase =>
        append("off", pb.getTokenCharacterOffset.toInt.toString)
        append("line", pb.getTokenLineNr.toString)
        append("col", pb.getTokenColumnNr.toString)
      case _ =>
    }
    append("token", Option(parser.currentToken()).map(_.name()).getOrElse("null"))

    new VisitorException(sb.result(), t)
  }

  private def splunkFriendly(value: String): String = {
    if (value.contains('"') || value.contains(" ")) {
      "\"" + value.replaceAllLiterally("\"", "\\\"") + "\""
    } else {
      value
    }
  }
}
