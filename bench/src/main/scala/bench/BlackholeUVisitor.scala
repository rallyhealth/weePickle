package bench

import upickle.core.{ArrVisitor, ObjVisitor, Visitor}
import org.openjdk.jmh.infra.Blackhole

class BlackholeUVisitor(bh: Blackhole) extends Visitor[Any, Null] {

  override def visitArray(length: Int, index: Int): ArrVisitor[Any, Null] = new ArrVisitor[Any, Null] {
    override def subVisitor: Visitor[_, _] = BlackholeUVisitor.this

    override def visitValue(v: Any, index: Int): Unit = bh.consume(v)

    override def visitEnd(index: Int): Null = {
      bh.consume(this)
      null
    }
  }

  override def visitObject(length: Int, jsonableKeys: Boolean, index: Int): ObjVisitor[Any, Null] = new ObjVisitor[Any, Null] {
    override def visitKey(index: Int): Visitor[_, _] = {
      BlackholeUVisitor.this
    }

    override def visitKeyValue(v: Any): Unit = bh.consume(v)

    override def subVisitor: Visitor[_, _] = BlackholeUVisitor.this

    override def visitValue(v: Any, index: Int): Unit = bh.consume(v)

    override def visitEnd(index: Int): Null = {
      bh.consume(this)
      null
    }
  }

  override def visitNull(index: Int): Null = {
    bh.consume(this)
    null
  }

  override def visitFalse(index: Int): Null = {
    bh.consume(false)
    null
  }

  override def visitTrue(index: Int): Null = {
    bh.consume(true)
    null
  }

  override def visitFloat64StringParts(s: CharSequence, decIndex: Int, expIndex: Int, index: Int): Null = {
    bh.consume(s)
    null
  }

  override def visitFloat64String(s: String, index: Int): Null = {
    bh.consume(s)
    null
  }

  override def visitInt32(i: Int, index: Int): Null = {
    bh.consume(i)
    null
  }

  override def visitInt64(l: Long, index: Int): Null = {
    bh.consume(l)
    null
  }

  override def visitFloat64(d: Double, index: Int): Null = {
    bh.consume(d)
    null
  }

  override def visitFloat32(d: Float, index: Int): Null = {
    bh.consume(d)
    null
  }

  override def visitString(s: CharSequence, index: Int): Null = {
    bh.consume(s)
    null
  }

  override def visitChar(c: Char, index: Int): Null = {
    bh.consume(c)
    null
  }

  override def visitBinary(bytes: Array[Byte], offset: Int, len: Int, index: Int): Null = {
    bh.consume(bytes)
    null
  }

  override def visitExt(tag: Byte, bytes: Array[Byte], offset: Int, len: Int, index: Int): Null = {
    bh.consume(bytes)
    null
  }

  override def visitUInt64(i: Long, index: Int): Null = {
    bh.consume(i)
    null
  }

}
