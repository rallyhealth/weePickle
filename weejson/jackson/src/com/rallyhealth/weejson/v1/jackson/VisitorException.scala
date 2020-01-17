package com.rallyhealth.weejson.v1.jackson

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.base.ParserBase
import com.rallyhealth.weejson.v1.jackson.VisitorException.splunkFriendly

import scala.util.control.NoStackTrace

/**
  * Enriches an exception with parser-level information.
  *
  * @param shortMsg free-text of what/where went wrong.
  * @param index    byte or character position of the input data (1-indexed)
  * @param line     line of text (if applicable) (1-indexed)
  * @param col      column of text (if applicable) (1-indexed)
  * @param token    textual representation of the json token (if applicable)
  */
class VisitorException(
  val shortMsg: String,
  val jsonPointer: String,
  val index: Option[Long],
  val line: Option[Long],
  val col: Option[Long],
  val token: Option[String],
  cause: Throwable
) extends Exception(
  {
    val sb = new StringBuilder(shortMsg)

    @inline def append(k: String, v: String): Unit = sb.append(' ').append(k).append('=').append(v)

    @inline def appendOpt(k: String, v: Option[Any]): Unit = v.foreach(v => sb.append(' ').append(k).append('=').append(v))

    append("jsonPointer", splunkFriendly(jsonPointer))
    appendOpt("index", index)
    appendOpt("line", line)
    appendOpt("col", col)
    appendOpt("token", token)
    sb.result()
  },
  cause
) with NoStackTrace

object VisitorException {

  def apply(msg: String, parser: JsonParser, t: Throwable = null): Throwable = {
    val parserBase = Option(parser).collect { case pb: ParserBase => pb }
    new VisitorException(
      shortMsg = msg,
      jsonPointer = parser.getParsingContext.pathAsPointer().toString,
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
