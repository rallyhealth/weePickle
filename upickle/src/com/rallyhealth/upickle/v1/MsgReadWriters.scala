package com.rallyhealth.upickle.v1

import com.rallyhealth.upickle.v1.core.Visitor
import com.rallyhealth.upickle.v1.implicits.MacroImplicits

trait MsgReadWriters extends com.rallyhealth.upickle.v1.core.Types with MacroImplicits{
  implicit val MsgValueR: Reader[com.rallyhealth.upack.v1.Msg] = new Reader.Delegate(com.rallyhealth.upack.v1.Msg)

  implicit val MsgValueW: Writer[com.rallyhealth.upack.v1.Msg] = new Writer[com.rallyhealth.upack.v1.Msg] {
    def write0[R](out: Visitor[_, R], v: com.rallyhealth.upack.v1.Msg): R = com.rallyhealth.upack.v1.transform(v, out)
  }
}
