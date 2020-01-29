package com.rallyhealth.weepack.v1

import com.rallyhealth.weepickle.v1.core.{FromInput, Visitor}

object FromMsgPack {

  def apply(bytes: Array[Byte]): FromInput = new FromInput {
    override def transform[T](to: Visitor[_, T]): T = {
      new MsgPackParser(0, bytes).parse(to)
    }
  }

  def apply(ast: Msg): FromInput = ast
}
