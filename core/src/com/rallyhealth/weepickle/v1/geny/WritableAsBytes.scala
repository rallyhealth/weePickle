package com.rallyhealth.weepickle.v1.geny

import java.io.{ByteArrayInputStream, InputStream, OutputStream}
import java.nio.charset.StandardCharsets

/**
 * A [[WritableAsBytes]] is a source of bytes that can be written to an OutputStream.
 *
 * Essentially a push-based version of `java.io.InputStream`, that allows an
 * implementation to guarantee that cleanup logic runs after the bytes are
 * written.
 *
 * [[WritableAsBytes]] is also much easier to implement than `java.io.InputStream`: any
 * code that previously wrote output to an `ByteArrayOutputStream` or
 * `StringBuilder` can trivially satisfy the [[WritableAsBytes]] interface. That makes
 * [[WritableAsBytes]] very convenient to use for allowing zero-friction zero-overhead
 * streaming data exchange between different libraries.
 *
 * [[WritableAsBytes]] comes with implicit constructors from `Array[Byte]`, `String`
 * and `InputStream`, and is itself a tiny interface with minimal functionality.
 * Libraries using [[WritableAsBytes]] are expected to extend it to provide additional
 * methods or additional implicit constructors that make sense in their context.
 */
trait WritableAsBytes{
  def writeBytesTo(out: OutputStream): Unit
}
object WritableAsBytes{
  implicit def readableWritable[T](t: T)(implicit f: T => ReadableAsBytes): WritableAsBytes = f(t)
}

/**
 * A [[ReadableAsBytes]] is a source of bytes that can be read from an InputStream
 *
 * A subtype of [[WritableAsBytes]], every [[ReadableAsBytes]] can be trivially used as a
 * [[WritableAsBytes]] by transferring the bytes from the InputStream to the OutputStream,
 * but not every [[WritableAsBytes]] is a [[ReadableAsBytes]].
 *
 * Note that the InputStream is only available inside the `readBytesThrough`, and
 * may be closed and cleaned up (along with any associated resources) once the
 * callback returns.
 */
trait ReadableAsBytes extends WritableAsBytes{
  def readBytesThrough[T](f: InputStream => T): T
  def writeBytesTo(out: OutputStream): Unit = readBytesThrough(Internal.transfer(_, out))
}
object ReadableAsBytes{
  implicit class StringByteSource(s: String) extends ReadableAsBytes{
    def readBytesThrough[T](f: InputStream => T): T = {
      f(new ByteArrayInputStream(s.getBytes(StandardCharsets.UTF_8)))
    }
  }

  implicit class ByteArrayByteSource(a: Array[Byte]) extends ReadableAsBytes{
    def readBytesThrough[T](f: InputStream => T): T = f(new ByteArrayInputStream(a))
  }

  implicit class InputStreamByteSource(i: InputStream) extends ReadableAsBytes{
    def readBytesThrough[T](f: InputStream => T): T = f(i)
  }
}
