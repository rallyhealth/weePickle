package com.rallyhealth.weepickle.v1.implicits

// Common things for derivation
// For Scala 3 macro implicits -- imp in API
trait MacrosCommon {

  def serializeDefaults: Boolean = false

  def objectAttributeKeyReadMap(s: CharSequence): CharSequence //= s
  def objectAttributeKeyWriteMap(s: CharSequence): CharSequence //= s

  def objectTypeKeyReadMap(s: CharSequence): CharSequence //= s
  def objectTypeKeyWriteMap(s: CharSequence): CharSequence //= s

}
