package com.rallyhealth.weepickle.v1.implicits

trait MacrosCommon {

  def serializeDefaults: Boolean = false

  def objectAttributeKeyReadMap(s: CharSequence): CharSequence
  def objectAttributeKeyWriteMap(s: CharSequence): CharSequence

  def objectTypeKeyReadMap(s: CharSequence): CharSequence
  def objectTypeKeyWriteMap(s: CharSequence): CharSequence

}
