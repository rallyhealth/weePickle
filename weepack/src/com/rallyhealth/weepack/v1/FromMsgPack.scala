package com.rallyhealth.weepack.v1

import com.rallyhealth.weepickle.v1.core.{Transmittable, Visitor}

object FromMsgPack {

  def apply(bytes: Array[Byte]): Transmittable = new Transmittable {
    override def transmit[T](into: Visitor[_, T]): T = {
      new MsgPackReceiver(0, bytes).parse(into)
    }
  }

  def apply(ast: Msg): Transmittable = ast
}
