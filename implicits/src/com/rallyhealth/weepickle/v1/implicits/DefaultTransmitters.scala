package com.rallyhealth.weepickle.v1.implicits

import java.net.URI
import java.util.UUID

import com.rallyhealth.weepickle.v1.core.Visitor

import scala.concurrent.duration.{Duration, FiniteDuration}

trait DefaultTransmitters extends com.rallyhealth.weepickle.v1.core.Types with Generated with MacroImplicits with LowPriTransmitters{
  implicit val StringWriter: Transmitter[String] = new Transmitter[String] {
    def transmit0[R](v: String, out: Visitor[_, R]): R = out.visitString(v)
  }
  implicit val UnitTransmitter: Transmitter[Unit] = new Transmitter[Unit] {
    def transmit0[R](v: Unit, out: Visitor[_, R]): R = {
      out.visitObject(0).visitEnd()
    }
  }

  implicit val DoubleTransmitter: Transmitter[Double] = new Transmitter[Double] {
    def transmit0[R](v: Double, out: Visitor[_, R]): R = out.visitFloat64(v)
  }
  implicit val IntTransmitter: Transmitter[Int] = new Transmitter[Int] {
    def transmit0[R](v: Int, out: Visitor[_, R]): R = out.visitInt32(v)
  }

  implicit val FloatTransmitter: Transmitter[Float] = new Transmitter[Float] {
    def transmit0[R](v: Float, out: Visitor[_, R]): R = out.visitFloat32(v)
  }
  implicit val ShortTransmitter: Transmitter[Short] = new Transmitter[Short] {
    def transmit0[R](v: Short, out: Visitor[_, R]): R = out.visitInt32(v)
  }
  implicit val ByteTransmitter: Transmitter[Byte] = new Transmitter[Byte] {
    def transmit0[R](v: Byte, out: Visitor[_, R]): R = out.visitInt32(v)
  }

  implicit val BooleanTransmitter: Transmitter[Boolean] = new Transmitter[Boolean] {
    def transmit0[R](v: Boolean, out: Visitor[_, R]): R = {
      if(v) out.visitTrue() else out.visitFalse()
    }
  }
  implicit val CharTransmitter: Transmitter[Char] = new Transmitter[Char] {
    def transmit0[R](v: Char, out: Visitor[_, R]): R = out.visitChar(v)
  }
  implicit val UUIDTransmitter: Transmitter[UUID] = StringWriter.comap[UUID](_.toString)
  implicit val LongTransmitter = new Transmitter[Long] {
    def transmit0[R](v: Long, out: Visitor[_, R]): R = out.visitInt64(v)
  }
  implicit val BigIntTransmitter: Transmitter[BigInt] = StringWriter.comap[BigInt](_.toString)
  implicit val BigDecimalTransmitter: Transmitter[BigDecimal] = StringWriter.comap[BigDecimal](_.toString)
  implicit val SymbolTransmitter: Transmitter[Symbol] = StringWriter.comap[Symbol](_.name)
  implicit val UriTransmitter: Transmitter[URI] = StringWriter.comap[URI](_.toString)

  implicit def OptionTransmitter[T: Transmitter]: Transmitter[Option[T]] = implicitly[Transmitter[T]].comap[Option[T]] {
    case None => null.asInstanceOf[T]
    case Some(x) => x
  }
  implicit def SomeTransmitter[T: Transmitter]: Transmitter[Some[T]] = OptionTransmitter[T].narrow[Some[T]]
  implicit def NoneTransmitter: Transmitter[None.type] = OptionTransmitter[Unit].narrow[None.type]

  implicit def ArrayTransmitter[T](implicit r: Transmitter[T]): Transmitter[Array[T]] = {
    if (r == ByteTransmitter) new Transmitter[Array[T]] {
      def transmit0[R](v: Array[T], out: Visitor[_, R]): R = {
        out.visitBinary(v.asInstanceOf[Array[Byte]], 0, v.length)
      }
    }
    else new Transmitter[Array[T]] {
      def transmit0[R](v: Array[T], out: Visitor[_, R]): R = {
        val ctx = out.visitArray(v.length).narrow
        var i = 0
        while (i < v.length) {
          ctx.visitValue(r.transmit(v(i), ctx.subVisitor))
          i += 1
        }

        ctx.visitEnd()
      }
    }
  }
  def MapTransmitter0[M[A, B] <: collection.Map[A, B], K, V]
                (implicit kw: Transmitter[K], vw: Transmitter[V]): Transmitter[M[K, V]] = {
    if (kw eq StringWriter) new Transmitter[M[String, V]]{
      def transmit0[R](v: M[String, V], out: Visitor[_, R]): R = {
        val ctx = out.visitObject(v.size).narrow
        for(pair <- v){
          val (k1, v1) = pair
          val keyVisitor = ctx.visitKey()
          ctx.visitKeyValue(keyVisitor.visitString(k1))
          ctx.visitValue(vw.transmit(v1, ctx.subVisitor))

        }
        ctx.visitEnd()
      }
    }.asInstanceOf[Transmitter[M[K, V]]]
    else SeqLikeTransmitter[Seq, (K, V)].comap[M[K, V]](_.toSeq)
  }
  implicit def MapTransmitter1[K, V](implicit kw: Transmitter[K], vw: Transmitter[V]): Transmitter[collection.Map[K, V]] = {
    MapTransmitter0[collection.Map, K, V]
  }
  implicit def MapTransmitter2[K, V](implicit kw: Transmitter[K], vw: Transmitter[V]): Transmitter[collection.immutable.Map[K, V]] = {
    MapTransmitter0[collection.immutable.Map, K, V]
  }
  implicit def MapTransmitter3[K, V](implicit kw: Transmitter[K], vw: Transmitter[V]): Transmitter[collection.mutable.Map[K, V]] = {
    MapTransmitter0[collection.mutable.Map, K, V]
  }

  implicit val DurationTransmitter: Transmitter[Duration] = new Transmitter[Duration]{
    def transmit0[R](v: Duration, out: Visitor[_, R]): R = v match{
      case Duration.Inf => out.visitString("inf")
      case Duration.MinusInf => out.visitString("-inf")
      case x if x eq Duration.Undefined => out.visitString("undef")
      case _ => out.visitString(v.toNanos.toString)
    }
  }

  implicit val InfiniteDurationTransmitter: Transmitter[Duration.Infinite] = DurationTransmitter.narrow[Duration.Infinite]
  implicit val FiniteDurationTransmitter: Transmitter[FiniteDuration] = DurationTransmitter.narrow[FiniteDuration]

  implicit def EitherTransmitter[T1: Transmitter, T2: Transmitter]: Transmitter[Either[T1, T2]] = new Transmitter[Either[T1, T2]]{
    def transmit0[R](v: Either[T1, T2], out: Visitor[_, R]): R = v match{
      case Left(t1) =>
        val ctx = out.visitArray(2).narrow
        ctx.visitValue(ctx.subVisitor.visitFloat64StringParts("0", -1, -1))

        ctx.visitValue(implicitly[Transmitter[T1]].transmit(t1, ctx.subVisitor))

        ctx.visitEnd()
      case Right(t2) =>
        val ctx = out.visitArray(2).narrow
        ctx.visitValue(ctx.subVisitor.visitFloat64StringParts("1", -1, -1))

        ctx.visitValue(implicitly[Transmitter[T2]].transmit(t2, ctx.subVisitor))

        ctx.visitEnd()
    }
  }
  implicit def RightTransmitter[T1: Transmitter, T2: Transmitter]: Transmitter[Right[T1, T2]] =
    EitherTransmitter[T1, T2].narrow[Right[T1, T2]]
  implicit def LeftTransmitter[T1: Transmitter, T2: Transmitter]: Transmitter[Left[T1, T2]] =
    EitherTransmitter[T1, T2].narrow[Left[T1, T2]]
}

/**
  * This needs to be split into a separate trait due to https://github.com/scala/bug/issues/11768
  */
trait LowPriTransmitters extends com.rallyhealth.weepickle.v1.core.Types{
  implicit def SeqLikeTransmitter[C[_] <: Iterable[_], T](implicit r: Transmitter[T]): Transmitter[C[T]] = new Transmitter[C[T]] {
    def transmit0[R](v: C[T], out: Visitor[_, R]): R = {
      val ctx = out.visitArray(v.size).narrow
      val x = v.iterator
      while(x.nonEmpty){
        val next = x.next().asInstanceOf[T]
        val written = r.transmit(next, ctx.subVisitor)
        ctx.visitValue(written)
      }

      ctx.visitEnd()
    }
  }
}
