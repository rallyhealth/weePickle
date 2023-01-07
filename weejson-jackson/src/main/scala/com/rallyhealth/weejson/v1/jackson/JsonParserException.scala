package com.rallyhealth.weejson.v1.jackson

import com.fasterxml.jackson.core.{JsonParser, JsonStreamContext}
import com.fasterxml.jackson.core.base.ParserBase
import com.rallyhealth.weepickle.v1.core.TransformException

import scala.collection.compat._
import scala.collection.mutable.ArrayBuffer

object JsonParserException {

  def apply(msg: String, parser: JsonParser, t: Throwable = null): Throwable = {
    val parserBase = Option(parser).collect { case pb: ParserBase => pb }
    new TransformException(
      shortMsg = msg,
      jsonPointer = splunkFriendly(toJsonPointer(parser.getParsingContext)),
      index = parserBase.map(_.getTokenCharacterOffset),
      line = parserBase.map(_.getTokenLineNr),
      col = parserBase.map(_.getTokenColumnNr),
      token = parserBase.map(p => String.valueOf(p.currentToken())),
      cause = t
    )
  }

  private def toJsonPointer(context: JsonStreamContext): String = {
    // context.pathAsPointer().toString requires O(depth^2) memory because of
    // JsonPointer._asString, and OOMEs on FromJson("[" * 100000).transform().
    // For our own, we collect the nodes from leaf to root, reverse them,
    // then build our string in order.
    val leafToRoot =
    Iterator
      .iterate(Option(context))(_.flatMap(c => Option(c.getParent)))
      .takeWhile(_.nonEmpty)
      .flatten
      .filter(_.hasPathSegment)
      .to(ArrayBuffer)

    leafToRoot
      .reverseIterator
      .foldLeft(new StringBuilder()) { (sb, ctx) =>
        if (ctx.inArray()) {
          sb.append('/').append(ctx.getCurrentIndex)
        }
        else if (ctx.inObject()) {
          sb.append('/')
          for {
            name <- Option(ctx.getCurrentName)
              c <- name
          } {
            c match {
              case '/' => sb.append("~1")
              case '~' => sb.append("~0")
              case _ => sb.append(c)
            }
          }
        }
        sb
      }
      .result()
  }

  private def splunkFriendly(value: String): String = {
    if (value.contains('"') || value.contains(" ")) {
      "\"" + value.replace("\"", "\\\"") + "\""
    } else {
      value
    }
  }
}
