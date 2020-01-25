package com.rallyhealth.weepickle.v1

import com.rallyhealth.weepack.v1.{FromMsgPack, Msg}
import com.rallyhealth.weepickle.v1.core.Visitor
import com.rallyhealth.weepickle.v1.implicits.MacroImplicits

trait FromToMsg extends com.rallyhealth.weepickle.v1.core.Types with MacroImplicits {
  implicit val ToMsgValue: To[com.rallyhealth.weepack.v1.Msg] =
    new To.Delegate(com.rallyhealth.weepack.v1.Msg)

  implicit val FromMsgValue: From[com.rallyhealth.weepack.v1.Msg] =
    new From[com.rallyhealth.weepack.v1.Msg] {
      def transform0[Out](v: Msg, out: Visitor[_, Out]): Out = FromMsgPack(v).transform(out)
    }
}
