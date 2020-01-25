package com.rallyhealth.weepack.v1

import com.rallyhealth.weepickle.v1.core.{FromData, Visitor}

object FromMsgPack {

  def apply(bytes: Array[Byte]): FromData = new FromData {
    override def transform[T](to: Visitor[_, T]): T = {
      new MsgPackParser(0, bytes).parse(to)
    }
  }

  def apply(ast: Msg): FromData = ast
}
