package com.rallyhealth.weepack.v1

import com.rallyhealth.weepickle.v1.core.{Transformable, Visitor}

object FromMsgPack {

  def apply(bytes: Array[Byte]): Transformable = new Transformable {
    override def transform[T](into: Visitor[_, T]): T = {
      new MsgPackReader(0, bytes).parse(into)
    }
  }

  def apply(ast: Msg): Transformable = ast
}
