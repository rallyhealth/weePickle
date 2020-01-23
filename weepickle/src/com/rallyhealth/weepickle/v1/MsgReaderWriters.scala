package com.rallyhealth.weepickle.v1

import com.rallyhealth.weepack.v1.{FromMsgPack, Msg}
import com.rallyhealth.weepickle.v1.core.Visitor
import com.rallyhealth.weepickle.v1.implicits.MacroImplicits

trait MsgTransceivers extends com.rallyhealth.weepickle.v1.core.Types with MacroImplicits{
  implicit val MsgValueR: Receiver[com.rallyhealth.weepack.v1.Msg] = new Receiver.Delegate(com.rallyhealth.weepack.v1.Msg)

  implicit val MsgValueW: Transmitter[com.rallyhealth.weepack.v1.Msg] = new Transmitter[com.rallyhealth.weepack.v1.Msg] {
    def transmit0[Out](v: Msg, out: Visitor[_, Out]): Out = FromMsgPack(v).transmit(out)
  }
}
