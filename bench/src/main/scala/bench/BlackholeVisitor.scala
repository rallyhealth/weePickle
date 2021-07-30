package bench

import com.rallyhealth.weepickle.v1.core.{ArrVisitor, JsVisitor, ObjVisitor, Visitor}
import org.openjdk.jmh.infra.Blackhole

class BlackholeVisitor(bh: Blackhole) extends JsVisitor[Any, Null] {

  override def visitArray(length: Int): ArrVisitor[Any, Null] = new ArrVisitor[Any, Null] {
    override def subVisitor: Visitor[_, _] = BlackholeVisitor.this

    override def visitValue(v: Any): Unit = bh.consume(v)

    override def visitEnd(): Null = {
      bh.consume(this)
      null
    }
  }

  override def visitObject(length: Int): ObjVisitor[Any, Null] = new ObjVisitor[Any, Null] {
    override def visitKey(): Visitor[_, _] = {
      BlackholeVisitor.this
    }

    override def visitKeyValue(v: Any): Unit = bh.consume(v)

    override def subVisitor: Visitor[_, _] = BlackholeVisitor.this

    override def visitValue(v: Any): Unit = bh.consume(v)

    override def visitEnd(): Null = {
      bh.consume(this)
      null
    }
  }

  override def visitNull(): Null = {
    bh.consume(this)
    null
  }

  override def visitFalse(): Null = {
    bh.consume(false)
    null
  }

  override def visitTrue(): Null = {
    bh.consume(true)
    null
  }

  override def visitFloat64StringParts(s: CharSequence, decIndex: Int, expIndex: Int): Null = {
    bh.consume(s)
    null
  }

  override def visitFloat64String(s: String): Null = {
    bh.consume(s)
    null
  }

  override def visitString(s: CharSequence): Null = {
    bh.consume(s)
    null
  }
}
