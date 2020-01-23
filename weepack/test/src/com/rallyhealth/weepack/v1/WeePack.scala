package com.rallyhealth.weepack.v1

/**
  * Legacy test shim from the old APIs to the new high level APIs.
  */
object WeePack {

  def transform[T](t: Msg, v: com.rallyhealth.weepickle.v1.core.Visitor[_, T]): T = FromMsgPack(t).transmit(v)

  def transform[T](t: Array[Byte], v: com.rallyhealth.weepickle.v1.core.Visitor[_, T]): T = FromMsgPack(t).transmit(v)

  /**
    * Read the given MessagePack input into a MessagePack struct
    */
  def read(s: Msg): Msg = FromMsgPack(s).transmit(ToMsgPack.ast)
  def read(s: Array[Byte]): Msg = FromMsgPack(s).transmit(ToMsgPack.ast)

  def copy(t: Msg): Msg = FromMsgPack(t).transmit(ToMsgPack.ast)

  /**
    * Write the given MessagePack struct as a binary
    */
  def write(t: Msg): Array[Byte] = {
    FromMsgPack(t).transmit(ToMsgPack.bytes)
  }

  /**
    * Write the given MessagePack struct as a binary to the given OutputStream
    */
  def writeTo(t: Msg, out: java.io.OutputStream): Unit = {
    FromMsgPack(t).transmit(ToMsgPack.outputStream(out))
  }
}
