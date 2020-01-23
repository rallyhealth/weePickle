package com.rallyhealth.weepickle.v1
import com.rallyhealth.weejson.v1.Value
import com.rallyhealth.weepickle.v1.implicits.MacroImplicits
import com.rallyhealth.weepickle.v1.core.Visitor

trait JsTransceivers extends com.rallyhealth.weepickle.v1.core.Types with MacroImplicits{

  implicit val JsValueR: Receiver[com.rallyhealth.weejson.v1.Value] = new Receiver.Delegate(com.rallyhealth.weejson.v1.Value)

  implicit def JsObjR: Receiver[com.rallyhealth.weejson.v1.Obj] = JsValueR.narrow[com.rallyhealth.weejson.v1.Obj]
  implicit def JsArrR: Receiver[com.rallyhealth.weejson.v1.Arr] = JsValueR.narrow[com.rallyhealth.weejson.v1.Arr]
  implicit def JsStrR: Receiver[com.rallyhealth.weejson.v1.Str] = JsValueR.narrow[com.rallyhealth.weejson.v1.Str]
  implicit def JsNumR: Receiver[com.rallyhealth.weejson.v1.Num] = JsValueR.narrow[com.rallyhealth.weejson.v1.Num]
  implicit def JsBoolR: Receiver[com.rallyhealth.weejson.v1.Bool] = JsValueR.narrow[com.rallyhealth.weejson.v1.Bool]
  implicit def JsTrueR: Receiver[com.rallyhealth.weejson.v1.True.type] = JsValueR.narrow[com.rallyhealth.weejson.v1.True.type]
  implicit def JsFalseR: Receiver[com.rallyhealth.weejson.v1.False.type] = JsValueR.narrow[com.rallyhealth.weejson.v1.False.type]
  implicit def JsNullR: Receiver[com.rallyhealth.weejson.v1.Null.type] = JsValueR.narrow[com.rallyhealth.weejson.v1.Null.type]


  implicit def JsObjW: Transmitter[com.rallyhealth.weejson.v1.Obj] = JsValueW.narrow[com.rallyhealth.weejson.v1.Obj]
  implicit def JsArrW: Transmitter[com.rallyhealth.weejson.v1.Arr] = JsValueW.narrow[com.rallyhealth.weejson.v1.Arr]
  implicit def JsStrW: Transmitter[com.rallyhealth.weejson.v1.Str] = JsValueW.narrow[com.rallyhealth.weejson.v1.Str]
  implicit def JsNumW: Transmitter[com.rallyhealth.weejson.v1.Num] = JsValueW.narrow[com.rallyhealth.weejson.v1.Num]
  implicit def JsBoolW: Transmitter[com.rallyhealth.weejson.v1.Bool] = JsValueW.narrow[com.rallyhealth.weejson.v1.Bool]
  implicit def JsTrueW: Transmitter[com.rallyhealth.weejson.v1.True.type] = JsValueW.narrow[com.rallyhealth.weejson.v1.True.type]
  implicit def JsFalseW: Transmitter[com.rallyhealth.weejson.v1.False.type] = JsValueW.narrow[com.rallyhealth.weejson.v1.False.type]
  implicit def JsNullW: Transmitter[com.rallyhealth.weejson.v1.Null.type] = JsValueW.narrow[com.rallyhealth.weejson.v1.Null.type]
  implicit val JsValueW: Transmitter[com.rallyhealth.weejson.v1.Value] = new Transmitter[com.rallyhealth.weejson.v1.Value] {
    def transmit0[Out](v: Value, out: Visitor[_, Out]): Out = v.transmit(out)
  }
}
