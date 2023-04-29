package rallyhealth.weepickle.v1

import com.rallyhealth.weepickle.v1.core._

/**
  * Returns the number of elements in an array or object.
  *
  * Useful for benchmarks to prevent the compiler from cheating.
  */
class CardinalityVisitor extends SimpleVisitor[Any, Int] {

  override def expectedMsg: String = "expected obj/arr"

  override def visitObject(
    length: Int
  ): ObjVisitor[Any, Int] = new ObjVisitor[Any, Int] {
    private var i = 0

    override def visitKey(): Visitor[_, _] = NoOpVisitor

    override def visitKeyValue(
      v: Any
    ): Unit = ()

    override def subVisitor: Visitor[_, _] = NoOpVisitor

    override def visitValue(
      v: Any
    ): Unit = i += 1

    override def visitEnd(): Int = i
  }

  override def visitArray(
    length: Int
  ): ArrVisitor[Any, Int] = new ArrVisitor[Any, Int] {
    private var i = 0

    override def subVisitor: Visitor[_, _] = NoOpVisitor

    override def visitValue(
      v: Any
    ): Unit = i += 1

    override def visitEnd(): Int = i
  }
}
