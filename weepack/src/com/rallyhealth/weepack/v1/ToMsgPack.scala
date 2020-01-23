package com.rallyhealth.weepack.v1

import java.io.ByteArrayOutputStream

import com.rallyhealth.weepickle.v1.core.Visitor

object ToMsgPack {

  /**
    * Write the given MessagePack struct as a binary
    */
  def bytes: Visitor[ByteArrayOutputStream, Array[Byte]] = { // TODO, no ByteArrayOutputStream
    outputStream(new ByteArrayOutputStream()).map(_.toByteArray)
  }

  /**
    * Write the given MessagePack struct as a binary to the given OutputStream
    */
  def outputStream[OutputStream <: java.io.OutputStream](out: OutputStream): Visitor[OutputStream, OutputStream] = {
    new MsgPackTransmitter(out)
  }

  def ast: Visitor[Msg, Msg] = Msg
}
