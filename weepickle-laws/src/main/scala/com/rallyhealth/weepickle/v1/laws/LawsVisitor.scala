package com.rallyhealth.weepickle.v1.laws

import com.rallyhealth.weepickle.v1.core.Visitor.{ArrDelegate, ObjDelegate}
import com.rallyhealth.weepickle.v1.core.{ArrVisitor, ObjVisitor, Visitor}
import com.rallyhealth.weepickle.v1.laws.LawsVisitor.{ObjArrState, VisitorState}

import java.time.Instant

/**
  * Enforces that all "visit" calls are made against this visitor in the correct order.
  *
  * ==Usage==
  * {{{
  *   val v = new YourVisitor(new LawsVisitor(NullVisitor))
  *   input.transform(v)
  * }}}
  *
  * All methods throw `IllegalStateException` if invoked in an invalid order.
  */
class LawsVisitor[T, J](
  delegate: Visitor[T, J]
) extends Visitor.Delegate[T, J](delegate) {

  private var parentState: VisitorState.State = VisitorState.Initialized

  /**
    * Invoked on start of:
    * - [[visitObject]]
    * - [[visitArray]]
    * - [[visitTrue]]
    * - etc.
    */
  protected[LawsVisitor] def onValueStart(
  )(
    implicit
    enc: sourcecode.Enclosing
  ): Unit = {
    import VisitorState._
    parentState.assertOneOf(Initialized)
    parentState = VisitStart
  }

  /**
    * Invoked on completion of:
    * - [[com.rallyhealth.weepickle.v1.core.ObjVisitor.visitEnd]]
    * - [[com.rallyhealth.weepickle.v1.core.ArrVisitor.visitEnd]]
    * - [[visitTrue]]
    * - etc.
    */
  protected[LawsVisitor] def onValueEnd(
  )(
    implicit
    enc: sourcecode.Enclosing
  ): Unit = {
    import VisitorState._
    parentState.assertOneOf(VisitStart)
    parentState = VisitComplete
  }

  /**
    * Shortcut for eveything except [[visitObject]] and [[visitArray]].
    */
  protected[LawsVisitor] def onValue(
  )(
    implicit
    enc: sourcecode.Enclosing
  ): Unit = {
    onValueStart()
    onValueEnd()
  }

  override def close(): Unit = {
    import VisitorState._
    parentState.assertOneOf(VisitComplete)
    parentState = Closed
    super.close()
  }

  override def visitObject(
    length: Int
  ): ObjVisitor[T, J] = {
    require(length >= -1, s"length $length must be >= -1")

    import com.rallyhealth.weepickle.v1.laws.LawsVisitor.ObjArrState._

    onValueStart()

    var objState: ObjArrState.State = Initialized

    val underlying = super.visitObject(length)
    new ObjDelegate[T, J](underlying) {
      private var keys = 0
      private var subvisitors = 0
      private var values = 0

      override def visitKey(): Visitor[_, _] = {
        keys += 1
        objState.assertOneOf(Initialized, VisitValue)
        objState = VisitKeyStart
        new LawsVisitor[Nothing, Any](super.visitKey()) {

          override protected[LawsVisitor] def onValueStart(
          )(
            implicit
            enc: sourcecode.Enclosing
          ): Unit = {
            objState.assertOneOf(VisitKeyStart)
            super.onValueStart()
          }

          override protected[LawsVisitor] def onValueEnd(
          )(
            implicit
            enc: sourcecode.Enclosing
          ): Unit = {
            super.onValueEnd()
            objState = VisitKeyComplete
          }
        }
      }

      override def visitKeyValue(
        s: Any
      ): Unit = {
        objState.assertOneOf(VisitKeyComplete)
        super.visitKeyValue(s)
        objState = VisitKeyValue
      }

      override def subVisitor: Visitor[Nothing, Any] = {
        subvisitors += 1
        objState.assertOneOf(VisitKeyValue)
        objState = SubvisitorStart
        new LawsVisitor[Nothing, Any](super.subVisitor) {

          override protected[LawsVisitor] def onValueStart(
          )(
            implicit
            enc: sourcecode.Enclosing
          ): Unit = {
            objState.assertOneOf(SubvisitorStart)
            super.onValueStart()
          }

          override protected[LawsVisitor] def onValueEnd(
          )(
            implicit
            enc: sourcecode.Enclosing
          ): Unit = {
            super.onValueEnd()
            objState = SubvisitorComplete
          }
        }
      }

      override def visitValue(
        v: T
      ): Unit = {
        values += 1
        objState.assertOneOf(SubvisitorComplete)
        super.visitValue(v)
        objState = VisitValue
      }

      override def visitEnd(): J = {
        objState.assertOneOf(Initialized, VisitValue)
        objState = VisitEnd
        onValueEnd()
        if (keys != subvisitors || keys != values)
          throw new IllegalStateException(
            s"Mismatch: visitKey() $keys times, subVisitor() $subvisitors times, visitValue() $values times"
          )
        if (length != -1 && keys != length)
          throw new IllegalStateException(
            s"visitObject($length), but got $values entries"
          )
        super.visitEnd()
      }
    }
  }

  override def visitArray(
    length: Int
  ): ArrVisitor[T, J] = {
    require(length >= -1, s"length $length must be >= -1")

    import ObjArrState._

    onValueStart()

    var arrState: ObjArrState.State = Initialized

    val underlying = super.visitArray(length)
    new ArrDelegate[T, J](underlying) {
      private var subvisitors = 0
      private var values = 0

      override def subVisitor: Visitor[Nothing, Any] = {
        arrState.assertOneOf(Initialized, VisitValue)
        arrState = SubvisitorStart
        subvisitors += 1
        new LawsVisitor[Nothing, Any](super.subVisitor) {

          override protected[LawsVisitor] def onValueStart(
          )(
            implicit
            enc: sourcecode.Enclosing
          ): Unit = {
            arrState.assertOneOf(SubvisitorStart)
            super.onValueStart()
          }

          override protected[LawsVisitor] def onValueEnd(
          )(
            implicit
            enc: sourcecode.Enclosing
          ): Unit = {
            super.onValueEnd()
            arrState = SubvisitorComplete
          }
        }
      }

      override def visitValue(
        v: T
      ): Unit = {
        arrState.assertOneOf(SubvisitorComplete)
        values += 1
        super.visitValue(v)
        arrState = VisitValue
      }

      override def visitEnd(): J = {
        arrState.assertOneOf(Initialized, VisitValue)
        onValueEnd()
        arrState = VisitEnd
        if (subvisitors != values)
          throw new IllegalStateException(
            s"visitEnd() called after $subvisitors subVisitor() and $values visitValue()"
          )
        if (length != -1 && subvisitors != length)
          throw new IllegalStateException(
            s"visitArray($length), but got $values entries"
          )
        super.visitEnd()
      }
    }
  }

  override def visitNull(): J = {
    onValue()
    super.visitNull()
  }

  override def visitTrue(): J = {
    onValue()
    super.visitTrue()
  }

  override def visitFalse(): J = {
    onValue()
    super.visitFalse()
  }

  override def visitString(
    cs: CharSequence
  ): J = {
    onValue()
    super.visitString(cs)
  }

  override def visitFloat64StringParts(
    cs: CharSequence,
    decIndex: Int,
    expIndex: Int
  ): J = {
    onValue()
    super.visitFloat64StringParts(cs, decIndex, expIndex)
  }

  override def visitFloat64(
    d: Double
  ): J = {
    onValue()
    super.visitFloat64(d)
  }

  override def visitFloat32(
    d: Float
  ): J = {
    onValue()
    super.visitFloat32(d)
  }

  override def visitInt32(
    i: Int
  ): J = {
    onValue()
    super.visitInt32(i)
  }

  override def visitInt64(
    l: Long
  ): J = {
    onValue()
    super.visitInt64(l)
  }

  override def visitUInt64(
    ul: Long
  ): J = {
    onValue()
    super.visitUInt64(ul)
  }

  override def visitFloat64String(
    s: String
  ): J = {
    onValue()
    super.visitFloat64String(s)
  }

  override def visitChar(
    c: Char
  ): J = {
    onValue()
    super.visitChar(c)
  }

  override def visitBinary(
    bytes: Array[Byte],
    offset: Int,
    len: Int
  ): J = {
    onValue()
    super.visitBinary(bytes, offset, len)
  }

  override def visitExt(
    tag: Byte,
    bytes: Array[Byte],
    offset: Int,
    len: Int
  ): J = {
    onValue()
    super.visitExt(tag, bytes, offset, len)
  }

  override def visitTimestamp(
    instant: Instant
  ): J = {
    onValue()
    super.visitTimestamp(instant)
  }
}

object LawsVisitor {

  trait StateEnumeration extends Enumeration {

    final class State extends super.Val {

      def assertOneOf(
        validStates: State*
      )(
        implicit
        enc: sourcecode.Enclosing
      ) = {
        if (!validStates.contains(this)) {
          throw new IllegalStateException(
            validStates.mkString(
              s"Invalid call to ${enc.value}. currentState=$this validStates=[",
              ", ",
              "]"
            )
          )
        }
      }
    }
  }

  object ObjArrState extends StateEnumeration {

    val Initialized, VisitKeyStart, VisitKeyComplete, VisitKeyValue,
    SubvisitorStart, SubvisitorComplete, VisitValue, VisitEnd = new State()
  }

  object VisitorState extends StateEnumeration {

    val Initialized, VisitStart, VisitComplete, Closed = new State()
  }
}

