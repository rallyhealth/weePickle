package com.rallyhealth.ujson.v0

object Platform{
  @inline def charAt(s: CharSequence, i: Int) = {
    if (i >= s.length) throw new StringIndexOutOfBoundsException(i)
    s.charAt(i)
  }
  @inline def charAt(s: String, i: Int) = {
    if (i >= s.length) throw new StringIndexOutOfBoundsException(i)
    s.charAt(i)
  }
}
