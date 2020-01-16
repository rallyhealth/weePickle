package com.rallyhealth.weepickle.v1

import com.rallyhealth.weepack.v1.WeePack
import com.rallyhealth.weepickle.v1.core.Visitor
import com.rallyhealth.weepickle.v1.implicits.MacroImplicits

trait MsgReaderWriters extends com.rallyhealth.weepickle.v1.core.Types with MacroImplicits{
  implicit val MsgValueR: Reader[com.rallyhealth.weepack.v1.Msg] = new Reader.Delegate(com.rallyhealth.weepack.v1.Msg)

  implicit val MsgValueW: Writer[com.rallyhealth.weepack.v1.Msg] = new Writer[com.rallyhealth.weepack.v1.Msg] {
    def write0[R](out: Visitor[_, R], v: com.rallyhealth.weepack.v1.Msg): R = WeePack.transform(v, out)
  }
}
