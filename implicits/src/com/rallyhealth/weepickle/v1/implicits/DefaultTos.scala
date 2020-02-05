package com.rallyhealth.weepickle.v1.implicits

import java.net.URI
import java.time.Instant
import java.util.{Base64, UUID}
import java.util.concurrent.TimeUnit

import com.rallyhealth.weepickle.v1.core._

import scala.collection.compat._
import scala.collection.mutable
import scala.concurrent.duration.{Duration, FiniteDuration}
import scala.reflect.ClassTag

trait DefaultTos extends com.rallyhealth.weepickle.v1.core.Types with Generated with MacroImplicits {
  implicit val ToUnit: To[Unit] = new SimpleTo[Unit] {
    override def expectedMsg = "expected unit"
    override def visitObject(length: Int): ObjVisitor[Any, Unit] = new ObjVisitor[Any, Unit] {
      def subVisitor = NoOpVisitor

      def visitValue(v: Any): Unit = ()

      def visitEnd(): Unit = ()

      def visitKey(): Visitor[_, _] = NoOpVisitor

      def visitKeyValue(v: Any): Unit = ()
    }
  }
  implicit val ToBoolean: To[Boolean] = new SimpleTo[Boolean] {
    override def expectedMsg = "expected boolean"
    override def visitTrue() = true
    override def visitFalse() = false
  }

  implicit val ToDouble: To[Double] = new NumericTo[Double] {
    override def expectedMsg = "expected number"
    override def visitString(cs: CharSequence): Double = cs.toString.toDouble
    override def visitInt32(d: Int): Double = d
    override def visitInt64(d: Long): Double = d
    override def visitUInt64(d: Long): Double = d
    override def visitFloat64(d: Double): Double = d
    override def visitFloat64String(s: String): Double = s.toDouble
    override def visitFloat64StringParts(cs: CharSequence, decIndex: Int, expIndex: Int): Double = {
      cs.toString.toDouble
    }
  }

  implicit val ToInt: To[Int] = new NumericTo[Int] {
    override def expectedMsg = "expected number"
    override def visitInt32(d: Int): Int = d
    override def visitInt64(d: Long): Int = d.toInt
    override def visitUInt64(d: Long): Int = d.toInt
    override def visitFloat64(d: Double): Int = d.toInt
    override def visitFloat64StringParts(cs: CharSequence, decIndex: Int, expIndex: Int): Int = {
      Util.parseIntegralNum(cs, decIndex, expIndex).toInt
    }
  }
  implicit val ToFloat: To[Float] = new NumericTo[Float] {
    override def expectedMsg = "expected number"

    override def visitString(cs: CharSequence): Float = cs.toString.toFloat
    override def visitInt32(d: Int): Float = d.toFloat
    override def visitInt64(d: Long): Float = d.toFloat
    override def visitUInt64(d: Long): Float = d.toFloat
    override def visitFloat64(d: Double): Float = d.toFloat
    override def visitFloat64String(s: String) = s.toFloat
    override def visitFloat64StringParts(cs: CharSequence, decIndex: Int, expIndex: Int): Float = {
      cs.toString.toFloat
    }
  }
  implicit val ToShort: To[Short] = new NumericTo[Short] {
    override def expectedMsg = "expected number"
    override def visitInt32(d: Int): Short = d.toShort
    override def visitInt64(d: Long): Short = d.toShort
    override def visitUInt64(d: Long): Short = d.toShort
    override def visitFloat64(d: Double) = d.toShort
    override def visitFloat64StringParts(cs: CharSequence, decIndex: Int, expIndex: Int): Short = {
      Util.parseIntegralNum(cs, decIndex, expIndex).toShort
    }
  }
  implicit val ToByte: To[Byte] = new NumericTo[Byte] {
    override def expectedMsg = "expected number"
    override def visitInt32(d: Int): Byte = d.toByte
    override def visitInt64(d: Long): Byte = d.toByte
    override def visitUInt64(d: Long): Byte = d.toByte
    override def visitFloat64(d: Double): Byte = d.toByte
    override def visitFloat64StringParts(cs: CharSequence, decIndex: Int, expIndex: Int): Byte = {
      Util.parseIntegralNum(cs, decIndex, expIndex).toByte
    }
  }

  implicit val ToString: To[String] = new SimpleTo[String] {
    override def expectedMsg = "expected string"
    override def visitString(cs: CharSequence): String = cs.toString
    override def visitTimestamp(instant: Instant): String = instant.toString
  }

  class MapStringTo[T](f: CharSequence => T) extends SimpleTo[T] {
    override def expectedMsg = "expected string"
    override def visitString(cs: CharSequence): T = f(cs)
  }

  /**
    * Forwards some methods to their alternate implementations for numeric types.
    * Similar to weeJson/JsVisitor, but for numeric types.
    */
  private trait NumericTo[J] extends SimpleTo[J] {
    override def visitFloat64String(s: String) = {
      visitFloat64StringParts(s, s.indexOf('.'), s.indexOf('E') match {
        case -1 => s.indexOf('e')
        case n  => n
      })
    }
  }

  implicit val ToChar: To[Char] = new NumericTo[Char] {
    override def expectedMsg = "expected char"
    override def visitString(d: CharSequence): Char = d.charAt(0)
    override def visitChar(d: Char): Char = d
    override def visitInt32(d: Int): Char = d.toChar
    override def visitInt64(d: Long): Char = d.toChar
    override def visitUInt64(d: Long): Char = d.toChar
    override def visitFloat64(d: Double): Char = d.toChar
    override def visitFloat64StringParts(cs: CharSequence, decIndex: Int, expIndex: Int): Char = {
      Util.parseIntegralNum(cs, decIndex, expIndex).toChar
    }
  }
  implicit val ToUUID: To[UUID] = new MapStringTo(s => UUID.fromString(s.toString))
  implicit val ToLong: To[Long] = new NumericTo[Long] {
    override def expectedMsg = "expected number"
    override def visitString(d: CharSequence): Long = com.rallyhealth.weepickle.v1.core.Util.parseLong(d, 0, d.length())
    override def visitInt32(d: Int): Long = d.toLong
    override def visitInt64(d: Long): Long = d.toLong
    override def visitUInt64(d: Long): Long = d.toLong
    override def visitFloat64(d: Double) = d.toLong
    override def visitFloat64StringParts(cs: CharSequence, decIndex: Int, expIndex: Int): Long = {
      Util.parseIntegralNum(cs, decIndex, expIndex).toLong
    }
    override def visitTimestamp(instant: Instant): Long = instant.toEpochMilli
  }
  private val digitLimit = 10000
  implicit val ToBigInt: To[BigInt] = new SimpleTo[BigInt] {
    override def expectedMsg = "expected number or numeric string"
    override def visitString(cs: CharSequence): BigInt = {
      if (cs.length() > digitLimit) {
        // Don't put the number in the exception otherwise trying to render it to a string could also cause problems
        throw new NumberFormatException(s"Number too large with ${cs.length} digits")
      } else {
        BigInt(cs.toString)
      }
    }
    override def visitFloat64StringParts(cs: CharSequence, decIndex: Int, expIndex: Int): BigInt = BigInt(cs.toString)
    override def visitFloat64String(s: String): BigInt = BigInt(s.toString)
    override def visitInt32(d: Int): BigInt = BigInt(d)
    override def visitInt64(d: Long): BigInt = BigInt(d)
    override def visitUInt64(d: Long): BigInt = BigInt(java.lang.Long.toUnsignedString(d))
  }
  implicit val ToBigDecimal: To[BigDecimal] = new SimpleTo[BigDecimal] {
    override def expectedMsg = "expected number or numeric string"
    override def visitString(cs: CharSequence): BigDecimal = {
      if (cs.length() > digitLimit) {
        // Don't put the number in the exception otherwise trying to render it to a string could also cause problems
        throw new NumberFormatException(s"Number too large with ${cs.length} digits")
      } else {
        val str = cs.toString
        BigDecimal(str)
      }
    }
    override def visitFloat64StringParts(cs: CharSequence, decIndex: Int, expIndex: Int): BigDecimal =
      BigDecimal(cs.toString)
    override def visitFloat64String(s: String): BigDecimal = BigDecimal(s.toString)
    override def visitInt32(d: Int): BigDecimal = BigDecimal(d)
    override def visitInt64(d: Long): BigDecimal = BigDecimal(d)
    override def visitUInt64(d: Long): BigDecimal = BigDecimal(java.lang.Long.toUnsignedString(d))
    override def visitFloat64(d: Double): BigDecimal = BigDecimal(d)
  }
  implicit val ToSymbol: To[Symbol] = new MapStringTo(s => Symbol(s.toString))
  implicit val ToUri: To[URI] = new MapStringTo(s => URI.create(s.toString))

  def MapTo0[M[A, B] <: collection.Map[A, B], K, V](
    make: Iterable[(K, V)] => M[K, V]
  )(implicit k: To[K], v: To[V]): To[M[K, V]] = {
    if (k ne ToString) ToSeqLike[Array, (K, V)].map(x => make(x))
    else
      new SimpleTo[M[K, V]] {
        override def visitObject(length: Int): ObjVisitor[Any, M[K, V]] = new ObjVisitor[Any, M[K, V]] {
          val strings = mutable.Buffer.empty[K]
          val values = mutable.Buffer.empty[V]
          def subVisitor = v

          def visitKey(): Visitor[_, _] = ToString

          def visitKeyValue(s: Any): Unit = {
            strings.append(s.toString.asInstanceOf[K])
          }

          def visitValue(v: Any): Unit = values.append(v.asInstanceOf[V])

          def visitEnd(): M[K, V] = make(strings.zip(values))

        }

        def expectedMsg = "expected map"
      }
  }
  implicit def ToMap[K, V](implicit k: To[K], v: To[V]): To[collection.Map[K, V]] = {
    MapTo0[collection.Map, K, V](_.toMap)
  }
  implicit def ToImmutableMap[K, V](implicit k: To[K], v: To[V]): To[collection.immutable.Map[K, V]] = {
    MapTo0[collection.immutable.Map, K, V] { seq =>
      val b = collection.immutable.Map.newBuilder[K, V]
      seq.foreach(b += _)
      b.result()
    }
  }
  implicit def ToMutableMap[K, V](implicit k: To[K], v: To[V]): To[collection.mutable.Map[K, V]] = {
    MapTo0[collection.mutable.Map, K, V] { seq =>
      val b = collection.mutable.Map.newBuilder[K, V]
      seq.foreach(b += _)
      b.result()
    }
  }

  implicit def ToOption[T: To]: To[Option[T]] = {
    new To.MapTo[T, T, Option[T]](implicitly[To[T]]) {

      private def f(t: T): Option[T] = Option(t)

      override def visitNull(): Option[T] = None

      override def mapFunction(v: T): Option[T] = f(v)

      def mapNonNullsFunction(v: T): Option[T] = f(v)
    }
  }

  implicit def ToSome[T: To]: To[Some[T]] = ToOption[T].narrow[Some[T]]
  implicit def ToNone: To[None.type] = ToOption[Unit].narrow[None.type]

  implicit def ToArray[T: To: ClassTag]: To[Array[T]] =
    if (implicitly[To[T]] == ToByte) new SimpleTo[Array[T]] {
      override def expectedMsg = "expected sequence"

      override def visitBinary(bytes: Array[Byte], offset: Int, len: Int) = {
        bytes.slice(offset, offset + len).asInstanceOf[Array[T]]
      }
      override def visitArray(length: Int): ArrVisitor[Any, Array[T]] = new ArrVisitor[Any, Array[T]] {
        val b = mutable.ArrayBuilder.make[T]

        def visitValue(v: Any): Unit = {
          b += v.asInstanceOf[T]
        }

        def visitEnd(): Array[T] = b.result()

        def subVisitor = implicitly[To[T]]
      }
      override def visitString(cs: CharSequence): Array[T] = {
        Base64.getDecoder.decode(cs.toString).asInstanceOf[Array[T]]
      }
    } else
      new SimpleTo[Array[T]] {
        override def expectedMsg = "expected sequence"
        override def visitArray(length: Int): ArrVisitor[Any, Array[T]] = new ArrVisitor[Any, Array[T]] {
          val b = mutable.ArrayBuilder.make[T]

          def visitValue(v: Any): Unit = {
            b += v.asInstanceOf[T]
          }

          def visitEnd(): Array[T] = b.result()

          def subVisitor = implicitly[To[T]]
        }
      }
  implicit def ToSeqLike[C[_], T](implicit r: To[T], factory: Factory[T, C[T]]): To[C[T]] =
    new SimpleTo[C[T]] {
      override def expectedMsg = "expected sequence"
      override def visitArray(length: Int): ArrVisitor[Any, C[T]] = new ArrVisitor[Any, C[T]] {
        val b = factory.newBuilder

        def visitValue(v: Any): Unit = {
          b += v.asInstanceOf[T]
        }

        def visitEnd(): C[T] = b.result()

        def subVisitor = r
      }
    }

  implicit val ToDuration = new MapStringTo(
    s =>
      if (s.charAt(0) == 'i' &&
          s.charAt(1) == 'n' &&
          s.charAt(2) == 'f'
          && s.length() == 3) {
        Duration.Inf
      } else if (s.charAt(0) == '-' &&
                 s.charAt(1) == 'i' &&
                 s.charAt(2) == 'n' &&
                 s.charAt(3) == 'f' &&
                 s.length() == 4) {
        Duration.MinusInf
      } else if (s.charAt(0) == 'u' &&
                 s.charAt(1) == 'n' &&
                 s.charAt(2) == 'd' &&
                 s.charAt(3) == 'e' &&
                 s.charAt(4) == 'f' &&
                 s.length() == 5) {
        Duration.Undefined
      } else Duration(com.rallyhealth.weepickle.v1.core.Util.parseLong(s, 0, s.length()), TimeUnit.NANOSECONDS)
  )

  implicit val ToInfiniteDuration = ToDuration.narrow[Duration.Infinite]
  implicit val ToFiniteDuration = ToDuration.narrow[FiniteDuration]

  implicit def ToEither[T1: To, T2: To]: SimpleTo[Either[T1, T2]] =
    new SimpleTo[Either[T1, T2]] {
      override def expectedMsg = "expected sequence"
      override def visitArray(length: Int): ArrVisitor[Any, Either[T1, T2]] = new ArrVisitor[Any, Either[T1, T2]] {
        var right: java.lang.Boolean = null
        var value: Either[T1, T2] = _
        def visitValue(v: Any): Unit = right match {
          case null =>
            v match {
              case 0 => right = false
              case 1 => right = true
            }
          case java.lang.Boolean.TRUE  => value = Right(v.asInstanceOf[T2])
          case java.lang.Boolean.FALSE => value = Left(v.asInstanceOf[T1])
        }

        def visitEnd(): Either[T1, T2] = value

        def subVisitor: Visitor[_, _] = right match {
          case null                    => ToInt
          case java.lang.Boolean.TRUE  => implicitly[To[T2]]
          case java.lang.Boolean.FALSE => implicitly[To[T1]]
        }
      }
    }
  implicit def ToRight[T1: To, T2: To] =
    ToEither[T1, T2].narrow[Right[T1, T2]]
  implicit def ToLeft[T1: To, T2: To] =
    ToEither[T1, T2].narrow[Left[T1, T2]]
}
