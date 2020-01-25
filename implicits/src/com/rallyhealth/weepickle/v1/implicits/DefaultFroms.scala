package com.rallyhealth.weepickle.v1.implicits

import java.net.URI
import java.util.UUID

import com.rallyhealth.weepickle.v1.core.Visitor

import scala.concurrent.duration.{Duration, FiniteDuration}

trait DefaultFroms
    extends com.rallyhealth.weepickle.v1.core.Types
    with Generated
    with MacroImplicits
    with LowPriFroms {
  implicit val FromString: From[String] = new From[String] {
    def transform0[R](v: String, out: Visitor[_, R]): R = out.visitString(v)
  }
  implicit val FromUnit: From[Unit] = new From[Unit] {
    def transform0[R](v: Unit, out: Visitor[_, R]): R = {
      out.visitObject(0).visitEnd()
    }
  }

  implicit val FromDouble: From[Double] = new From[Double] {
    def transform0[R](v: Double, out: Visitor[_, R]): R = out.visitFloat64(v)
  }
  implicit val FromInt: From[Int] = new From[Int] {
    def transform0[R](v: Int, out: Visitor[_, R]): R = out.visitInt32(v)
  }

  implicit val FromFloat: From[Float] = new From[Float] {
    def transform0[R](v: Float, out: Visitor[_, R]): R = out.visitFloat32(v)
  }
  implicit val FromShort: From[Short] = new From[Short] {
    def transform0[R](v: Short, out: Visitor[_, R]): R = out.visitInt32(v)
  }
  implicit val FromByte: From[Byte] = new From[Byte] {
    def transform0[R](v: Byte, out: Visitor[_, R]): R = out.visitInt32(v)
  }

  implicit val FromBoolean: From[Boolean] = new From[Boolean] {
    def transform0[R](v: Boolean, out: Visitor[_, R]): R = {
      if (v) out.visitTrue() else out.visitFalse()
    }
  }
  implicit val FromChar: From[Char] = new From[Char] {
    def transform0[R](v: Char, out: Visitor[_, R]): R = out.visitChar(v)
  }
  implicit val FromUUID: From[UUID] = FromString.comap[UUID](_.toString)
  implicit val LongFrom = new From[Long] {
    def transform0[R](v: Long, out: Visitor[_, R]): R = out.visitInt64(v)
  }
  implicit val FromBigInt: From[BigInt] = FromString.comap[BigInt](_.toString)
  implicit val FromBigDecimal: From[BigDecimal] = FromString.comap[BigDecimal](_.toString)
  implicit val FromSymbol: From[Symbol] = FromString.comap[Symbol](_.name)
  implicit val FromUri: From[URI] = FromString.comap[URI](_.toString)

  implicit def OptionFrom[T: From]: From[Option[T]] = implicitly[From[T]].comap[Option[T]] {
    case None    => null.asInstanceOf[T]
    case Some(x) => x
  }
  implicit def SomeFrom[T: From]: From[Some[T]] = OptionFrom[T].narrow[Some[T]]
  implicit def FromNone: From[None.type] = OptionFrom[Unit].narrow[None.type]

  implicit def ArrayFrom[T](implicit r: From[T]): From[Array[T]] = {
    if (r == FromByte) new From[Array[T]] {
      def transform0[R](v: Array[T], out: Visitor[_, R]): R = {
        out.visitBinary(v.asInstanceOf[Array[Byte]], 0, v.length)
      }
    } else
      new From[Array[T]] {
        def transform0[R](v: Array[T], out: Visitor[_, R]): R = {
          val ctx = out.visitArray(v.length).narrow
          var i = 0
          while (i < v.length) {
            ctx.visitValue(r.transform(v(i), ctx.subVisitor))
            i += 1
          }

          ctx.visitEnd()
        }
      }
  }
  def MapFrom0[M[A, B] <: collection.Map[A, B], K, V](
    implicit kw: From[K],
    vw: From[V]
  ): From[M[K, V]] = {
    if (kw eq FromString) new From[M[String, V]] {
      def transform0[R](v: M[String, V], out: Visitor[_, R]): R = {
        val ctx = out.visitObject(v.size).narrow
        for (pair <- v) {
          val (k1, v1) = pair
          val keyVisitor = ctx.visitKey()
          ctx.visitKeyValue(keyVisitor.visitString(k1))
          ctx.visitValue(vw.transform(v1, ctx.subVisitor))

        }
        ctx.visitEnd()
      }
    }.asInstanceOf[From[M[K, V]]]
    else SeqLikeFrom[Seq, (K, V)].comap[M[K, V]](_.toSeq)
  }
  implicit def MapFrom1[K, V](
    implicit kw: From[K],
    vw: From[V]
  ): From[collection.Map[K, V]] = {
    MapFrom0[collection.Map, K, V]
  }
  implicit def MapFrom2[K, V](
    implicit kw: From[K],
    vw: From[V]
  ): From[collection.immutable.Map[K, V]] = {
    MapFrom0[collection.immutable.Map, K, V]
  }
  implicit def MapFrom3[K, V](
    implicit kw: From[K],
    vw: From[V]
  ): From[collection.mutable.Map[K, V]] = {
    MapFrom0[collection.mutable.Map, K, V]
  }

  implicit val FromDuration: From[Duration] = new From[Duration] {
    def transform0[R](v: Duration, out: Visitor[_, R]): R = v match {
      case Duration.Inf                 => out.visitString("inf")
      case Duration.MinusInf            => out.visitString("-inf")
      case x if x eq Duration.Undefined => out.visitString("undef")
      case _                            => out.visitString(v.toNanos.toString)
    }
  }

  implicit val FromInfiniteDuration: From[Duration.Infinite] =
    FromDuration.narrow[Duration.Infinite]
  implicit val FromFiniteDuration: From[FiniteDuration] = FromDuration.narrow[FiniteDuration]

  implicit def EitherFrom[T1: From, T2: From]: From[Either[T1, T2]] =
    new From[Either[T1, T2]] {
      def transform0[R](v: Either[T1, T2], out: Visitor[_, R]): R = v match {
        case Left(t1) =>
          val ctx = out.visitArray(2).narrow
          ctx.visitValue(ctx.subVisitor.visitFloat64StringParts("0", -1, -1))

          ctx.visitValue(implicitly[From[T1]].transform(t1, ctx.subVisitor))

          ctx.visitEnd()
        case Right(t2) =>
          val ctx = out.visitArray(2).narrow
          ctx.visitValue(ctx.subVisitor.visitFloat64StringParts("1", -1, -1))

          ctx.visitValue(implicitly[From[T2]].transform(t2, ctx.subVisitor))

          ctx.visitEnd()
      }
    }
  implicit def RightFrom[T1: From, T2: From]: From[Right[T1, T2]] =
    EitherFrom[T1, T2].narrow[Right[T1, T2]]
  implicit def LeftFrom[T1: From, T2: From]: From[Left[T1, T2]] =
    EitherFrom[T1, T2].narrow[Left[T1, T2]]
}

/**
  * This needs to be split into a separate trait due to https://github.com/scala/bug/issues/11768
  */
trait LowPriFroms extends com.rallyhealth.weepickle.v1.core.Types {
  implicit def SeqLikeFrom[C[_] <: Iterable[_], T](implicit r: From[T]): From[C[T]] =
    new From[C[T]] {
      def transform0[R](v: C[T], out: Visitor[_, R]): R = {
        val ctx = out.visitArray(v.size).narrow
        val x = v.iterator
        while (x.nonEmpty) {
          val next = x.next().asInstanceOf[T]
          val written = r.transform(next, ctx.subVisitor)
          ctx.visitValue(written)
        }

        ctx.visitEnd()
      }
    }
}
