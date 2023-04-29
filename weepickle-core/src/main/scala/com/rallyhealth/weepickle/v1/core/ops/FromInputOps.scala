package com.rallyhealth.weepickle.v1.core.ops

import com.rallyhealth.weepickle.v1.core.FromInput

/**
 * Mixes extensions into bundles, e.g. WeePickle.
 */
trait FromInputOpsImplicits {

  implicit def asFromInputOps(a: FromInput): FromInputOps = new FromInputOps(a)
}

/**
 * Extension operations on [[FromInput]] that are part of the core API.
 *
 * More methods may be added here without breaking binary compatibility (which
 * is not possible with traits).
 */
class FromInputOps(
  val a: FromInput
) extends AnyVal {

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
  def ++(
    b: FromInput
  ): FromInput = ConcatFromInputs(a, b)
}
