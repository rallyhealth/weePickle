package com.rallyhealth.weejson.v1.jsoniter_scala

import com.github.plokhotnyuk.jsoniter_scala.core._
import com.rallyhealth.weepickle.v1.core.{FromInput, ObjArrVisitor, RootArrVisitor, Visitor}

/**
  * @see https://github.com/plokhotnyuk/jsoniter-scala
  */
object FromJsoniterScala {

  def apply(s: Array[Byte]): FromInput = new FromInput {
    override def transform[T](to: Visitor[_, T]): T = readFromArray(s)(codec(to))
  }

  def apply(s: String): FromInput = new FromInput {
    override def transform[T](to: Visitor[_, T]): T = readFromString(s)(codec(to))
  }

  implicit def codec[J](v: Visitor[_, J]): JsonValueCodec[J] = new JsonValueCodec[J] {

    override def decodeValue(in: JsonReader, default: J): J = {
      decodeValue(in, default, new RootArrVisitor(v) :: Nil)
    }

    def decodeValue(in: JsonReader, default: J, stack: List[ObjArrVisitor[Any, _]]): J = {
      def facade: Visitor[_, J] = stack.head.subVisitor.asInstanceOf[Visitor[_, J]]

      val b = in.nextToken()

      if (b == 'n') {
        in.readNullOrError(None, "expected `null` value")
        facade.visitNull()
      } else if (b == '"') {
        in.rollbackToken()
        facade.visitString(in.readString(null))
      } else if (b == 't' || b == 'f') {
        in.rollbackToken()
        if (in.readBoolean()) facade.visitTrue() else facade.visitFalse()
      } else if ((b >= '0' && b <= '9') || b == '-') {
        in.rollbackToken()
        val d = in.readDouble()
        val i = d.toInt
        if (i.toDouble == d) facade.visitInt32(i)
        else facade.visitFloat64(d)
      } else if (b == '[') {
        val arr = facade.visitArray(-1).narrow
        if (!in.isNextToken(']')) {
          in.rollbackToken()
          do arr.visitValue(decodeValue(in, default, arr :: stack))
          while (in.isNextToken(','))
          if (!in.isCurrentToken(']')) in.arrayEndOrCommaError()
        }
        arr.visitEnd()
      } else if (b == '{') {
        val obj = facade.visitObject(-1).narrow
        if (!in.isNextToken('}')) {
          in.rollbackToken()
          do {
            obj.visitKeyValue(obj.visitKey().visitString(in.readKeyAsString()))
            obj.visitValue(decodeValue(in, default, obj :: stack))
          }
          while (in.isNextToken(','))
          if (!in.isCurrentToken('}')) in.objectEndOrCommaError()
        }
        obj.visitEnd()
      } else in.decodeError("expected JSON value")
    }

    override val nullValue: J = v.visitNull()

    override def encodeValue(x: J, out: JsonWriter): Unit = ???
  }
}
