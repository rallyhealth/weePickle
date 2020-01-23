package com.rallyhealth.weepickle.v1.implicits

import java.net.URI
import java.util.UUID
import java.util.concurrent.TimeUnit

import com.rallyhealth.weepickle.v1.core._

import scala.collection.compat._
import scala.collection.mutable
import scala.concurrent.duration.{Duration, FiniteDuration}
import scala.reflect.ClassTag

trait DefaultReceivers extends com.rallyhealth.weepickle.v1.core.Types with Generated with MacroImplicits{
  implicit val UnitReceiver: Receiver[Unit] = new SimpleReceiver[Unit] {
    override def expectedMsg = "expected unit"
    override def visitObject(length: Int): ObjVisitor[Any, Unit] = new ObjVisitor[Any, Unit] {
      def subVisitor = NoOpVisitor

      def visitValue(v: Any): Unit = ()

      def visitEnd(): Unit = ()

      def visitKey(): Visitor[_, _] = NoOpVisitor

      def visitKeyValue(v: Any): Unit = ()
    }
  }
  implicit val BooleanReceiver: Receiver[Boolean] = new SimpleReceiver[Boolean] {
    override def expectedMsg = "expected boolean"
    override def visitTrue() = true
    override def visitFalse() = false
  }

  implicit val DoubleReceiver: Receiver[Double] = new NumericReceiver[Double] {
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

  implicit val IntReceiver: Receiver[Int] = new NumericReceiver[Int] {
    override def expectedMsg = "expected number"
    override def visitInt32(d: Int): Int = d
    override def visitInt64(d: Long): Int = d.toInt
    override def visitUInt64(d: Long): Int = d.toInt
    override def visitFloat64(d: Double): Int = d.toInt
    override def visitFloat64StringParts(cs: CharSequence, decIndex: Int, expIndex: Int): Int = {
      Util.parseIntegralNum(cs, decIndex, expIndex).toInt
    }
  }
  implicit val FloatReceiver: Receiver[Float] = new NumericReceiver[Float] {
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
  implicit val ShortReceiver: Receiver[Short] = new NumericReceiver[Short] {
    override def expectedMsg = "expected number"
    override def visitInt32(d: Int): Short = d.toShort
    override def visitInt64(d: Long): Short = d.toShort
    override def visitUInt64(d: Long): Short = d.toShort
    override def visitFloat64(d: Double) = d.toShort
    override def visitFloat64StringParts(cs: CharSequence, decIndex: Int, expIndex: Int): Short = {
      Util.parseIntegralNum(cs, decIndex, expIndex).toShort
    }
  }
  implicit val ByteReceiver: Receiver[Byte] = new NumericReceiver[Byte] {
    override def expectedMsg = "expected number"
    override def visitInt32(d: Int): Byte = d.toByte
    override def visitInt64(d: Long): Byte = d.toByte
    override def visitUInt64(d: Long): Byte = d.toByte
    override def visitFloat64(d: Double): Byte = d.toByte
    override def visitFloat64StringParts(cs: CharSequence, decIndex: Int, expIndex: Int): Byte = {
      Util.parseIntegralNum(cs, decIndex, expIndex).toByte
    }
  }

  implicit val StringReceiver: Receiver[String] = new SimpleReceiver[String] {
    override def expectedMsg = "expected string"
    override def visitString(cs: CharSequence): String = cs.toString
  }

  class MapStringReceiver[T](f: CharSequence => T) extends SimpleReceiver[T] {
    override def expectedMsg = "expected string"
    override def visitString(cs: CharSequence): T = f(cs)
  }

  /**
    * Forwards some methods to their alternate implementations for numeric types.
    * Similar to weeJson/JsVisitor, but for numeric types.
    */
  private trait NumericReceiver[J] extends SimpleReceiver[J] {
    override def visitFloat64String(s: String) = {
      visitFloat64StringParts(s, s.indexOf('.'), s.indexOf('E') match {
                      case -1 => s.indexOf('e')
                      case n => n
                    })
    }
  }

  implicit val CharReceiver: Receiver[Char] = new NumericReceiver[Char] {
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
  implicit val UUIDReceiver: Receiver[UUID] = new MapStringReceiver(s => UUID.fromString(s.toString))
  implicit val LongReceiver: Receiver[Long] = new NumericReceiver[Long] {
    override def expectedMsg = "expected number"
    override def visitString(d: CharSequence): Long = com.rallyhealth.weepickle.v1.core.Util.parseLong(d, 0, d.length())
    override def visitInt32(d: Int): Long = d.toLong
    override def visitInt64(d: Long): Long = d.toLong
    override def visitUInt64(d: Long): Long = d.toLong
    override def visitFloat64(d: Double) = d.toLong
    override def visitFloat64StringParts(cs: CharSequence, decIndex: Int, expIndex: Int): Long = {
      Util.parseIntegralNum(cs, decIndex, expIndex).toLong
    }
  }
  private val digitLimit = 10000
  implicit val BigIntReceiver: Receiver[BigInt] = new SimpleReceiver[BigInt] {
    override def expectedMsg = "expected number or numeric string"
    override def visitString(cs: CharSequence): BigInt = {
      if(cs.length() > digitLimit) {
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
  implicit val BigDecimalReceiver: Receiver[BigDecimal] = new SimpleReceiver[BigDecimal] {
    override def expectedMsg = "expected number or numeric string"
    override def visitString(cs: CharSequence): BigDecimal = {
      if(cs.length() > digitLimit) {
        // Don't put the number in the exception otherwise trying to render it to a string could also cause problems
        throw new NumberFormatException(s"Number too large with ${cs.length} digits")
      } else {
        val str = cs.toString
        BigDecimal(str)
      }
    }
    override def visitFloat64StringParts(cs: CharSequence, decIndex: Int, expIndex: Int): BigDecimal = BigDecimal(cs.toString)
    override def visitFloat64String(s: String): BigDecimal = BigDecimal(s.toString)
    override def visitInt32(d: Int): BigDecimal = BigDecimal(d)
    override def visitInt64(d: Long): BigDecimal = BigDecimal(d)
    override def visitUInt64(d: Long): BigDecimal = BigDecimal(java.lang.Long.toUnsignedString(d))
    override def visitFloat64(d: Double): BigDecimal = BigDecimal(d)
  }
  implicit val SymbolReceiver: Receiver[Symbol] = new MapStringReceiver(s => Symbol(s.toString))
  implicit val UriReceiver: Receiver[URI] = new MapStringReceiver(s => URI.create(s.toString))

  def MapReceiver0[M[A, B] <: collection.Map[A, B], K, V]
                (make: Iterable[(K, V)] => M[K, V])
                (implicit k: Receiver[K], v: Receiver[V]): Receiver[M[K, V]] = {
    if (k ne StringReceiver) SeqLikeReceiver[Array, (K, V)].map(x => make(x))
    else new SimpleReceiver[M[K, V]]{
      override def visitObject(length: Int): ObjVisitor[Any, M[K, V]] = new ObjVisitor[Any, M[K, V]] {
        val strings = mutable.Buffer.empty[K]
        val values = mutable.Buffer.empty[V]
        def subVisitor = v

        def visitKey(): Visitor[_, _] = StringReceiver

        def visitKeyValue(s: Any): Unit = {
          strings.append(s.toString.asInstanceOf[K])
        }

        def visitValue(v: Any): Unit = values.append(v.asInstanceOf[V])

        def visitEnd(): M[K, V] = make(strings.zip(values))

      }

      def expectedMsg = "expected map"
    }
  }
  implicit def MapReceiver1[K, V](implicit k: Receiver[K], v: Receiver[V]): Receiver[collection.Map[K, V]] = {
    MapReceiver0[collection.Map, K, V](_.toMap)
  }
  implicit def MapReceiver2[K, V](implicit k: Receiver[K], v: Receiver[V]): Receiver[collection.immutable.Map[K, V]] = {
    MapReceiver0[collection.immutable.Map, K, V]{seq =>
      val b = collection.immutable.Map.newBuilder[K, V]
      seq.foreach(b += _)
      b.result()
    }
  }
  implicit def MapReceiver3[K, V](implicit k: Receiver[K], v: Receiver[V]): Receiver[collection.mutable.Map[K, V]] = {
    MapReceiver0[collection.mutable.Map, K, V]{seq =>
      val b = collection.mutable.Map.newBuilder[K, V]
      seq.foreach(b += _)
      b.result()
    }
  }

  implicit def OptionReceiver[T: Receiver]: Receiver[Option[T]] = {
    new Receiver.MapReceiver[T, T, Option[T]](implicitly[Receiver[T]]) {

      private def f(t: T): Option[T] = Option(t)

      override def visitNull(): Option[T] = None

      override def mapFunction(v: T): Option[T] = f(v)

      def mapNonNullsFunction(v: T): Option[T] = f(v)
    }
  }

  implicit def SomeReceiver[T: Receiver]: Receiver[Some[T]] = OptionReceiver[T].narrow[Some[T]]
  implicit def NoneReceiver: Receiver[None.type] = OptionReceiver[Unit].narrow[None.type]

  implicit def ArrayReceiver[T: Receiver: ClassTag]: Receiver[Array[T]] =
    if (implicitly[Receiver[T]] == ByteReceiver) new SimpleReceiver[Array[T]] {
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

        def subVisitor = implicitly[Receiver[T]]
      }
    }
    else new SimpleReceiver[Array[T]] {
      override def expectedMsg = "expected sequence"
      override def visitArray(length: Int): ArrVisitor[Any, Array[T]] = new ArrVisitor[Any, Array[T]] {
        val b = mutable.ArrayBuilder.make[T]

        def visitValue(v: Any): Unit = {
          b += v.asInstanceOf[T]
        }

        def visitEnd(): Array[T] = b.result()

        def subVisitor = implicitly[Receiver[T]]
      }
    }
  implicit def SeqLikeReceiver[C[_], T](implicit r: Receiver[T],
                                      factory: Factory[T, C[T]]): Receiver[C[T]] = new SimpleReceiver[C[T]] {
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

  implicit val DurationReceiver = new MapStringReceiver( s =>
    if (s.charAt(0) == 'i' &&
        s.charAt(1) == 'n' &&
        s.charAt(2) == 'f'
        && s.length() == 3){
      Duration.Inf
    } else if (s.charAt(0) == '-' &&
               s.charAt(1) == 'i' &&
               s.charAt(2) == 'n' &&
               s.charAt(3) == 'f' &&
               s.length() == 4){
      Duration.MinusInf
    } else if (s.charAt(0) == 'u' &&
               s.charAt(1) == 'n' &&
               s.charAt(2) == 'd' &&
               s.charAt(3) == 'e' &&
               s.charAt(4) == 'f' &&
               s.length() == 5){
      Duration.Undefined
    }else Duration(com.rallyhealth.weepickle.v1.core.Util.parseLong(s, 0, s.length()), TimeUnit.NANOSECONDS)
  )

  implicit val InfiniteDurationReceiver = DurationReceiver.narrow[Duration.Infinite]
  implicit val FiniteDurationReceiver = DurationReceiver.narrow[FiniteDuration]

  implicit def EitherReceiver[T1: Receiver, T2: Receiver]: SimpleReceiver[Either[T1, T2]] = new SimpleReceiver[Either[T1, T2]]{
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
        case java.lang.Boolean.TRUE => value = Right(v.asInstanceOf[T2])
        case java.lang.Boolean.FALSE => value = Left(v.asInstanceOf[T1])
      }

      def visitEnd(): Either[T1, T2] = value

      def subVisitor: Visitor[_, _] = right match{
        case null => IntReceiver
        case java.lang.Boolean.TRUE => implicitly[Receiver[T2]]
        case java.lang.Boolean.FALSE => implicitly[Receiver[T1]]
      }
    }
  }
  implicit def RightReceiver[T1: Receiver, T2: Receiver] =
    EitherReceiver[T1, T2].narrow[Right[T1, T2]]
  implicit def LeftReceiver[T1: Receiver, T2: Receiver] =
    EitherReceiver[T1, T2].narrow[Left[T1, T2]]
}
