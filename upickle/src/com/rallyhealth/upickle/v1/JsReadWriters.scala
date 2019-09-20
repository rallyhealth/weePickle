package com.rallyhealth.upickle.v1
import com.rallyhealth.upickle.v1.implicits.MacroImplicits
import com.rallyhealth.upickle.v1.core.Visitor

trait JsReadWriters extends com.rallyhealth.upickle.v1.core.Types with MacroImplicits{

  implicit val JsValueR: Reader[com.rallyhealth.ujson.v1.Value] = new Reader.Delegate(com.rallyhealth.ujson.v1.Value)

  implicit def JsObjR: Reader[com.rallyhealth.ujson.v1.Obj] = JsValueR.narrow[com.rallyhealth.ujson.v1.Obj]
  implicit def JsArrR: Reader[com.rallyhealth.ujson.v1.Arr] = JsValueR.narrow[com.rallyhealth.ujson.v1.Arr]
  implicit def JsStrR: Reader[com.rallyhealth.ujson.v1.Str] = JsValueR.narrow[com.rallyhealth.ujson.v1.Str]
  implicit def JsNumR: Reader[com.rallyhealth.ujson.v1.Num] = JsValueR.narrow[com.rallyhealth.ujson.v1.Num]
  implicit def JsBoolR: Reader[com.rallyhealth.ujson.v1.Bool] = JsValueR.narrow[com.rallyhealth.ujson.v1.Bool]
  implicit def JsTrueR: Reader[com.rallyhealth.ujson.v1.True.type] = JsValueR.narrow[com.rallyhealth.ujson.v1.True.type]
  implicit def JsFalseR: Reader[com.rallyhealth.ujson.v1.False.type] = JsValueR.narrow[com.rallyhealth.ujson.v1.False.type]
  implicit def JsNullR: Reader[com.rallyhealth.ujson.v1.Null.type] = JsValueR.narrow[com.rallyhealth.ujson.v1.Null.type]


  implicit def JsObjW: Writer[com.rallyhealth.ujson.v1.Obj] = JsValueW.narrow[com.rallyhealth.ujson.v1.Obj]
  implicit def JsArrW: Writer[com.rallyhealth.ujson.v1.Arr] = JsValueW.narrow[com.rallyhealth.ujson.v1.Arr]
  implicit def JsStrW: Writer[com.rallyhealth.ujson.v1.Str] = JsValueW.narrow[com.rallyhealth.ujson.v1.Str]
  implicit def JsNumW: Writer[com.rallyhealth.ujson.v1.Num] = JsValueW.narrow[com.rallyhealth.ujson.v1.Num]
  implicit def JsBoolW: Writer[com.rallyhealth.ujson.v1.Bool] = JsValueW.narrow[com.rallyhealth.ujson.v1.Bool]
  implicit def JsTrueW: Writer[com.rallyhealth.ujson.v1.True.type] = JsValueW.narrow[com.rallyhealth.ujson.v1.True.type]
  implicit def JsFalseW: Writer[com.rallyhealth.ujson.v1.False.type] = JsValueW.narrow[com.rallyhealth.ujson.v1.False.type]
  implicit def JsNullW: Writer[com.rallyhealth.ujson.v1.Null.type] = JsValueW.narrow[com.rallyhealth.ujson.v1.Null.type]
  implicit val JsValueW: Writer[com.rallyhealth.ujson.v1.Value] = new Writer[com.rallyhealth.ujson.v1.Value] {
    def write0[R](out: Visitor[_, R], v: com.rallyhealth.ujson.v1.Value): R = com.rallyhealth.ujson.v1.transform(v, out)
  }
}
