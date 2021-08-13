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

  /*
   * And the same for BufferedValue types
   */
  implicit val ToBufferedValue: To[com.rallyhealth.weejson.v1.BufferedValue] =
    new To.Delegate(com.rallyhealth.weejson.v1.BufferedValue.Builder)

  implicit def ToBufferedValueObj: To[com.rallyhealth.weejson.v1.BufferedValue.Obj] = ToBufferedValue.narrow[com.rallyhealth.weejson.v1.BufferedValue.Obj]
  implicit def ToBufferedValueArr: To[com.rallyhealth.weejson.v1.BufferedValue.Arr] = ToBufferedValue.narrow[com.rallyhealth.weejson.v1.BufferedValue.Arr]
  implicit def ToBufferedValueStr: To[com.rallyhealth.weejson.v1.BufferedValue.Str] = ToBufferedValue.narrow[com.rallyhealth.weejson.v1.BufferedValue.Str]
  implicit def ToBufferedValueAnyNum: To[com.rallyhealth.weejson.v1.BufferedValue.AnyNum] = ToBufferedValue.narrow[com.rallyhealth.weejson.v1.BufferedValue.AnyNum]
  implicit def ToBufferedValueNum: To[com.rallyhealth.weejson.v1.BufferedValue.Num] = ToBufferedValue.narrow[com.rallyhealth.weejson.v1.BufferedValue.Num]
  implicit def ToBufferedValueNumLong: To[com.rallyhealth.weejson.v1.BufferedValue.NumLong] = ToBufferedValue.narrow[com.rallyhealth.weejson.v1.BufferedValue.NumLong]
  implicit def ToBufferedValueNumDouble: To[com.rallyhealth.weejson.v1.BufferedValue.NumDouble] = ToBufferedValue.narrow[com.rallyhealth.weejson.v1.BufferedValue.NumDouble]
  implicit def ToBufferedValueBinary: To[com.rallyhealth.weejson.v1.BufferedValue.Binary] =
    ToBufferedValue.narrow[com.rallyhealth.weejson.v1.BufferedValue.Binary]
  implicit def ToBufferedValueExt: To[com.rallyhealth.weejson.v1.BufferedValue.Ext] =
    ToBufferedValue.narrow[com.rallyhealth.weejson.v1.BufferedValue.Ext]
  implicit def ToBufferedValueTimestamp: To[com.rallyhealth.weejson.v1.BufferedValue.Timestamp] =
    ToBufferedValue.narrow[com.rallyhealth.weejson.v1.BufferedValue.Timestamp]
  implicit def ToBufferedValueBool: To[com.rallyhealth.weejson.v1.BufferedValue.Bool] = ToBufferedValue.narrow[com.rallyhealth.weejson.v1.BufferedValue.Bool]
  implicit def ToBufferedValueTrue: To[com.rallyhealth.weejson.v1.BufferedValue.True.type] =
    ToBufferedValue.narrow[com.rallyhealth.weejson.v1.BufferedValue.True.type]
  implicit def ToBufferedValueFalse: To[com.rallyhealth.weejson.v1.BufferedValue.False.type] =
    ToBufferedValue.narrow[com.rallyhealth.weejson.v1.BufferedValue.False.type]
  implicit def ToBufferedValueNull: To[com.rallyhealth.weejson.v1.BufferedValue.Null.type] =
    ToBufferedValue.narrow[com.rallyhealth.weejson.v1.BufferedValue.Null.type]

  implicit val FromBufferedValue: From[com.rallyhealth.weejson.v1.BufferedValue] =
    new From[com.rallyhealth.weejson.v1.BufferedValue] {
      def transform0[Out](v: com.rallyhealth.weejson.v1.BufferedValue, out: Visitor[_, Out]): Out =
        com.rallyhealth.weejson.v1.BufferedValue.transform(v, out)
    }

  implicit def FromBufferedValueObj: From[com.rallyhealth.weejson.v1.BufferedValue.Obj] = FromBufferedValue.narrow[com.rallyhealth.weejson.v1.BufferedValue.Obj]
  implicit def FromBufferedValueArr: From[com.rallyhealth.weejson.v1.BufferedValue.Arr] = FromBufferedValue.narrow[com.rallyhealth.weejson.v1.BufferedValue.Arr]
  implicit def FromBufferedValueStr: From[com.rallyhealth.weejson.v1.BufferedValue.Str] = FromBufferedValue.narrow[com.rallyhealth.weejson.v1.BufferedValue.Str]
  implicit def FromBufferedValueAnyNum: From[com.rallyhealth.weejson.v1.BufferedValue.AnyNum] = FromBufferedValue.narrow[com.rallyhealth.weejson.v1.BufferedValue.AnyNum]
  implicit def FromBufferedValueNum: From[com.rallyhealth.weejson.v1.BufferedValue.Num] = FromBufferedValue.narrow[com.rallyhealth.weejson.v1.BufferedValue.Num]
  implicit def FromBufferedValueNumLong: From[com.rallyhealth.weejson.v1.BufferedValue.NumLong] = FromBufferedValue.narrow[com.rallyhealth.weejson.v1.BufferedValue.NumLong]
  implicit def FromBufferedValueNumDouble: From[com.rallyhealth.weejson.v1.BufferedValue.NumDouble] = FromBufferedValue.narrow[com.rallyhealth.weejson.v1.BufferedValue.NumDouble]
  implicit def FromBufferedValueBinary: From[com.rallyhealth.weejson.v1.BufferedValue.Binary] =
    FromBufferedValue.narrow[com.rallyhealth.weejson.v1.BufferedValue.Binary]
  implicit def FromBufferedValueExt: From[com.rallyhealth.weejson.v1.BufferedValue.Ext] =
    FromBufferedValue.narrow[com.rallyhealth.weejson.v1.BufferedValue.Ext]
  implicit def FromBufferedValueTimestamp: From[com.rallyhealth.weejson.v1.BufferedValue.Timestamp] =
    FromBufferedValue.narrow[com.rallyhealth.weejson.v1.BufferedValue.Timestamp]
  implicit def FromBufferedValueBool: From[com.rallyhealth.weejson.v1.BufferedValue.Bool] = FromBufferedValue.narrow[com.rallyhealth.weejson.v1.BufferedValue.Bool]
  implicit def FromBufferedValueTrue: From[com.rallyhealth.weejson.v1.BufferedValue.True.type] =
    FromBufferedValue.narrow[com.rallyhealth.weejson.v1.BufferedValue.True.type]
  implicit def FromBufferedValueFalse: From[com.rallyhealth.weejson.v1.BufferedValue.False.type] =
    FromBufferedValue.narrow[com.rallyhealth.weejson.v1.BufferedValue.False.type]
  implicit def FromBufferedValueNull: From[com.rallyhealth.weejson.v1.BufferedValue.Null.type] =
    FromBufferedValue.narrow[com.rallyhealth.weejson.v1.BufferedValue.Null.type]
}
