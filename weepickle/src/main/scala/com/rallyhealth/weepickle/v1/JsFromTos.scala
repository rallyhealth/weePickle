package com.rallyhealth.weepickle.v1
import com.rallyhealth.weejson.v1.Value
import com.rallyhealth.weepickle.v1.implicits.MacroImplicits
import com.rallyhealth.weepickle.v1.core.{Annotator, Types, Visitor}

trait FromToValue extends MacroImplicits { this: Types with Annotator =>

  implicit val ToValue: To[com.rallyhealth.weejson.v1.Value] =
    new To.Delegate(com.rallyhealth.weejson.v1.Value)

  implicit def ToValueObj: To[com.rallyhealth.weejson.v1.Obj] = ToValue.narrow[com.rallyhealth.weejson.v1.Obj]
  implicit def ToValueArr: To[com.rallyhealth.weejson.v1.Arr] = ToValue.narrow[com.rallyhealth.weejson.v1.Arr]
  implicit def ToValueStr: To[com.rallyhealth.weejson.v1.Str] = ToValue.narrow[com.rallyhealth.weejson.v1.Str]
  implicit def ToValueNum: To[com.rallyhealth.weejson.v1.Num] = ToValue.narrow[com.rallyhealth.weejson.v1.Num]
  implicit def ToValueBool: To[com.rallyhealth.weejson.v1.Bool] = ToValue.narrow[com.rallyhealth.weejson.v1.Bool]
  implicit def ToValueTrue: To[com.rallyhealth.weejson.v1.True.type] =
    ToValue.narrow[com.rallyhealth.weejson.v1.True.type]
  implicit def ToValueFalse: To[com.rallyhealth.weejson.v1.False.type] =
    ToValue.narrow[com.rallyhealth.weejson.v1.False.type]
  implicit def ToValueNull: To[com.rallyhealth.weejson.v1.Null.type] =
    ToValue.narrow[com.rallyhealth.weejson.v1.Null.type]

  implicit def FromValueObj: From[com.rallyhealth.weejson.v1.Obj] = FromValue.narrow[com.rallyhealth.weejson.v1.Obj]
  implicit def FromValueArr: From[com.rallyhealth.weejson.v1.Arr] = FromValue.narrow[com.rallyhealth.weejson.v1.Arr]
  implicit def FromValueStr: From[com.rallyhealth.weejson.v1.Str] = FromValue.narrow[com.rallyhealth.weejson.v1.Str]
  implicit def FromValueNum: From[com.rallyhealth.weejson.v1.Num] = FromValue.narrow[com.rallyhealth.weejson.v1.Num]
  implicit def FromValueBool: From[com.rallyhealth.weejson.v1.Bool] = FromValue.narrow[com.rallyhealth.weejson.v1.Bool]
  implicit def FromValueTrue: From[com.rallyhealth.weejson.v1.True.type] =
    FromValue.narrow[com.rallyhealth.weejson.v1.True.type]
  implicit def FromValueFalse: From[com.rallyhealth.weejson.v1.False.type] =
    FromValue.narrow[com.rallyhealth.weejson.v1.False.type]
  implicit def FromValueNull: From[com.rallyhealth.weejson.v1.Null.type] =
    FromValue.narrow[com.rallyhealth.weejson.v1.Null.type]
  implicit val FromValue: From[com.rallyhealth.weejson.v1.Value] =
    new From[com.rallyhealth.weejson.v1.Value] {
      def transform0[Out](v: Value, out: Visitor[_, Out]): Out = v.transform(out)
    }
}
