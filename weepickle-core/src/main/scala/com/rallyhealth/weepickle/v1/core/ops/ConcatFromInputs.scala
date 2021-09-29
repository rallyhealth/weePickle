package com.rallyhealth.weepickle.v1.core.ops

import com.rallyhealth.weepickle.v1.core.Visitor.{ArrDelegate, ObjDelegate}
import com.rallyhealth.weepickle.v1.core._
import scala.collection.compat._

import scala.annotation.tailrec

object ConcatFromInputs {

  /**
    * Concatenates elements of one or more objects or arrays.
    *
    * ==Examples==
    *   - {{{ [1] ++ [2] ==> [1, 2] }}}
    *   - {{{ {"a": 0} ++ {"b": 1} ==> {"a": 0, "b": 1} }}}
    *   - {{{ {"a": 0} ++ {"a": 1} ==> {"a": 0, "a": 1} }}}
    *   - {{{ {"a": 0} ++ [false] ==> }}} [[com.rallyhealth.weepickle.v1.core.Abort]]
    *
    * ==Caveats==
    * - Does not report object or array length. Will not work for msgpack.
    */
  def apply(
    head: FromInput,
    tail: FromInput*
  ): FromInput = from(head +: tail).get

  /**
    * Concatenates elements of one or more objects or arrays.
    *
    * ==Examples==
    *   - {{{ [1] ++ [2] ==> [1, 2] }}}
    *   - {{{ {"a": 0} ++ {"b": 1} ==> {"a": 0, "b": 1} }}}
    *   - {{{ {"a": 0} ++ {"a": 1} ==> {"a": 0, "a": 1} }}}
    *   - {{{ {"a": 0} ++ [false] ==> }}} [[com.rallyhealth.weepickle.v1.core.Abort]]
    *
    * ==Caveats==
    * - Does not report object or array length. Will not work for msgpack.
    */
  def from(
    inputs: Seq[FromInput]
  ): Option[FromInput] = {
    if (inputs.isEmpty) None
    else if (inputs.sizeCompare(1) == 0) Some(inputs.head)
    else {
      // flatten for stack safety
      val flattened =
        if (inputs.exists(_.isInstanceOf[ConcatFromInputs])) {
          inputs.flatMap {
            case c: ConcatFromInputs => c.inputs
            case other => other :: Nil
          }
        } else inputs
      Some(new ConcatFromInputs(flattened))
    }
  }

  private[this] class ConcatFromInputs(
    val inputs: Seq[FromInput]
  ) extends FromInput {

    assert(inputs.sizeCompare(1) > 0)

    override def transform[T](
      to: Visitor[_, T]
    ): T = {
      val it = inputs.iterator
      it.next().transform {
        new SimpleVisitor[Any, T] {
          override def expectedMsg: String = "expected arr or obj"

          override def visitObject(
            length: Int
          ): ObjVisitor[Any, T] = {
            val obj = to.visitObject(-1).narrow // final length unknown
            val nonLastObj = new ObjDelegate[Any, T](obj) {
              override def visitEnd() = null.asInstanceOf[T]
            }

            new ObjDelegate[Any, T](obj) {
              override def visitEnd(): T = {
                @tailrec def transformNext(): T = {
                  val result = it.next().transform {
                    new SimpleVisitor[Any, T] {
                      override def expectedMsg: String = "expected another obj"

                      override def visitObject(
                        length: Int
                      ): ObjVisitor[Any, T] = {
                        // suppress visitEnd if more input is coming
                        if (it.hasNext) nonLastObj
                        else obj
                      }
                    }
                  }

                  if (it.hasNext) transformNext()
                  else result
                }

                transformNext()
              }
            }
          }

          override def visitArray(
            length: Int
          ): ArrVisitor[Any, T] = {
            val arr = to.visitArray(-1).narrow // final length unknown
            val nonLastArr = new ArrDelegate[Any, T](arr) {
              override def visitEnd() = null.asInstanceOf[T]
            }

            new ArrDelegate[Any, T](arr) {
              override def visitEnd(): T = {
                @tailrec def transformNext(): T = {
                  val result = it.next().transform {
                    new SimpleVisitor[Any, T] {
                      override def expectedMsg: String = "expected another arr"

                      override def visitArray(
                        length: Int
                      ): ArrVisitor[Any, T] = {
                        // suppress visitEnd if more input is coming
                        if (it.hasNext) nonLastArr
                        else arr
                      }
                    }
                  }

                  if (it.hasNext) transformNext()
                  else result
                }

                transformNext()
              }
            }
          }
        }
      }
    }
  }
}
