package bench

import com.rallyhealth.weepickle.v1.WeePickle
import com.rallyhealth.weepickle.v1.WeePickle._
import com.rallyhealth.weepickle.v1.core.Visitor
import com.rallyhealth.weepickle.v1.implicits.dropDefault
import org.openjdk.jmh.annotations._
import org.openjdk.jmh.infra.Blackhole
import upickle.{core, default}

import java.util.concurrent.TimeUnit

/**
 * Benchmark to exercise the macro-generated logic (i.e., mapping to/from case classes),
 * where the implementation (and performance) differ quite a bit between Scala versions,
 * especially with respect to default value processing.
 *
 * ==Run with==
 * +bench / Jmh / run .ScalaVersionBench.*
 * +bench / Jmh / run .ScalaVersionDefaultBench.*
 *
 * Here are some example results from 8/3 - 8/4/2021 informal (-f 4) runs:
 *
 * Version  Benchmark                                    Mode  Cnt     Score     Error Units
 * 2.13.5: ScalaVersionBench.fromFlatPrimitives        thrpt   20  13202.890 ± 102.405  ops/ms
 * 2.13.5: ScalaVersionBench.toFlatPrimitives          thrpt   20    192.373 ±   1.867  ops/ms
 * 2.13.5: ScalaVersionBench.fromSample                thrpt   20   1006.202 ±  11.330  ops/s
 * 2.13.5: ScalaVersionBench.toSample                  thrpt   20    314.882 ±   7.125  ops/s
 * 2.13.5: ScalaVersionBench.toUpperbound              thrpt   20    713.025 ±  30.174  ops/s
 *
 * 2.13.5: ScalaVersionDefaultBench.fromFlatPrimitives thrpt   20  22038.074 ±  38.954  ops/ms
 * 2.13.5: ScalaVersionDefaultBench.toFlatPrimitives   thrpt   20  24854.633 ± 214.081  ops/ms
 * 2.13.5: ScalaVersionDefaultBench.fromSample         thrpt   20   4188.035 ±  85.052  ops/s
 * 2.13.5: ScalaVersionDefaultBench.toSample           thrpt   20   1546.885 ±  12.538  ops/s
 * 2.13.5: ScalaVersionDefaultBench.toUpperbound       thrpt   20   2936.679 ±  36.324  ops/s
 *
 * 3.0.1:  ScalaVersionBench.fromFlatPrimitives        thrpt   20   4653.536 ±  35.851  ops/ms // ~65% less
 * 3.0.1:  ScalaVersionBench.toFlatPrimitives          thrpt   20    186.453 ±   1.404  ops/ms // ~the same
 * 3.0.1:  ScalaVersionBench.fromSample                thrpt   20    733.122 ±   5.783  ops/s  // ~25% less
 * 3.0.1:  ScalaVersionBench.toSample                  thrpt   20    346.230 ±   9.235  ops/s  // ~10% more
 * 3.0.1:  ScalaVersionBench.toUpperbound              thrpt   20    717.988 ±  30.697  ops/s  // ~the same
 *
 * 3.0.1:  ScalaVersionDefaultBench.fromFlatPrimitives thrpt   20   4157.921 ±  37.427  ops/ms // ~80% less
 * 3.0.1:  ScalaVersionDefaultBench.toFlatPrimitives   thrpt   20   7424.267 ± 900.835  ops/ms // ~70% less
 * 3.0.1:  ScalaVersionDefaultBench.fromSample         thrpt   20   1728.697 ±  20.222  ops/s  // ~60% less
 * 3.0.1:  ScalaVersionDefaultBench.toSample           thrpt   20   1165.583 ±  14.725  ops/s  // ~25% less
 * 3.0.1:  ScalaVersionDefaultBench.toUpperbound       thrpt   20   2308.280 ±  19.467  ops/s  // ~20% less
 */


class SampleBench extends PickleFromToBench(ScalaVersionBench.benchmarkSampleData)

class SampleDefaultBench extends PickleFromToBench(ScalaVersionDefaultBench.benchmarkSampleData)

class FlatPrimitivesBench extends PickleFromToBench(ScalaVersionBench.benchmarkFlatPrimitives)

class FlatPrimitivesDefaultBench extends PickleFromToBench(ScalaVersionDefaultBench.benchmarkFlatPrimitives)

class FlatPrimitivesTruncDefaultBench extends PickleFromToBench(ScalaVersionDefaultBench.benchmarkFlatPrimitivesTrunc)

@OutputTimeUnit(TimeUnit.MILLISECONDS)
class HandrolledFlatPrimitivesBench extends PickleFromToBench(HandrolledFlatPrimitives())

@Warmup(iterations = 3, time = 5, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 5, timeUnit = TimeUnit.SECONDS)
@State(Scope.Benchmark)
@BenchmarkMode(Array(Mode.Throughput))
@Fork(
  jvmArgsAppend = Array(
    "-Xms350m",
    "-Xmx350m",
    "-XX:+HeapDumpOnOutOfMemoryError",
    // https://stackoverflow.com/questions/32047440/different-benchmarking-results-between-forks-in-jmh
    "-XX:-BackgroundCompilation",
    "-XX:-TieredCompilation"
  ),
  value = 10
)
abstract class PickleFromToBench[T: From : To : upickle.default.Reader : upickle.default.Writer](val data: T) {

  @Benchmark
  def fromWeePickle(bh: Blackhole) = FromScala(data).transform(new BlackholeVisitor(bh))

  @Benchmark
  def fromUPickle(bh: Blackhole) = upickle.default.transform(data).to(new BlackholeUVisitor(bh))

  @Benchmark
  def toWeePickle = FromScala(data).transform(to[T])

  @Benchmark
  def toUPickle = upickle.default.transform(data).to(upickle.default.reader[T])
}

object ScalaVersionBench {

  case class ADT0()

  object ADT0 {

    implicit val rw: FromTo[ADT0] = macroFromTo
    implicit val urw: upickle.default.ReadWriter[ADT0] = upickle.default.macroRW
  }

  case class ADTa(i: Int)

  object ADTa {

    implicit val rw: FromTo[ADTa] = macroFromTo
    implicit val urw: upickle.default.ReadWriter[ADTa] = upickle.default.macroRW
  }

  case class ADTc(i: Int = 2, s: String, t: (Double, Double) = (1, 2))

  object ADTc {

    implicit val rw: FromTo[ADTc] = macroFromTo
    implicit val urw: upickle.default.ReadWriter[ADTc] = upickle.default.macroRW
  }

  sealed trait A

  object A {

    implicit val rw: FromTo[A] = FromTo.merge(B.rw, C.rw)
    implicit val urw: upickle.default.ReadWriter[A] = upickle.default.ReadWriter.merge(B.urw, C.urw)
  }

  case class B(i: Int) extends A

  object B {

    implicit val rw: FromTo[B] = macroFromTo
    implicit val urw: upickle.default.ReadWriter[B] = upickle.default.macroRW
  }

  case class C(s1: String, s2: String) extends A

  object C {

    implicit val rw: FromTo[C] = macroFromTo
    implicit val urw: upickle.default.ReadWriter[C] = upickle.default.macroRW
  }

  sealed trait LL

  object LL {

    implicit val rw: FromTo[LL] = FromTo.merge(macroFromTo[End.type], macroFromTo[Node])
    implicit val urw: upickle.default.ReadWriter[LL] = upickle.default.ReadWriter.merge(upickle.default.macroRW[End.type], upickle.default.macroRW[Node])
  }

  case object End extends LL

  case class Node(c: Int, next: LL) extends LL

  object Node {

    implicit val rw: FromTo[Node] = macroFromTo
    implicit val urw: upickle.default.ReadWriter[Node] = upickle.default.macroRW
  }

  case class Data( // no type parameters - Scala 3 implementation can't support them yet
    a: Seq[(Int, Int)],
    b: String,
    c: A,
    //d: LL, // no linked list - uPickle's Scala 3 implementation can't support the recursion yet (WeePickle does)
    e: ADTc,
    f: ADT0
  )

  object Data {

    implicit val rw: FromTo[Data] = macroFromTo
    implicit val urw: upickle.default.ReadWriter[Data] = upickle.default.macroRW
  }

  val benchmarkSampleData: Seq[Data] = Seq.fill(1000)(
    Data(
      Vector((1, 2), (3, 4), (4, 5), (6, 7), (8, 9), (10, 11), (12, 13)),
      """
        |I am cow, hear me moo
        |I weigh twice as much as you
        |And I look good on the barbecueeeee
    """.stripMargin,
      C("lol i am a noob", "haha you are a noob"): A,
      //Node(-11, Node(-22, Node(-33, Node(-44, End)))): LL,
      ADTc(i = 1234567890, s = "i am a strange loop"),
      ADT0()
    )
  )

  case class FlatPrimitives(
    i: Int,
    s: String,
    b: Boolean,
    l: Long,
    d: Double,
    c: Char
  )

  object FlatPrimitives {

    implicit val pickler: FromTo[FlatPrimitives] = macroFromTo
    implicit val upickler: upickle.default.ReadWriter[FlatPrimitives] = upickle.default.macroRW
  }

  val benchmarkFlatPrimitives = FlatPrimitives(Int.MinValue, "", true, Long.MaxValue, Double.MaxValue, '!')
}

/**
 * Same, but with defaults dropped
 *
 */
object ScalaVersionDefaultBench {

  import ScalaVersionBench.{A, ADT0, ADTc, C}

  case class Data(
    @dropDefault
    a: Seq[(Int, Int)] = Nil,
    @dropDefault
    b: String = "",
    c: A,
    //@dropDefault
    //d: LL = End, // no linked list - uPickle's Scala 3 implementation can't support the recursion yet (WeePickle does)
    e: ADTc,
    f: ADT0
  )

  object Data {

    implicit val rw: FromTo[Data] = macroFromTo
    implicit val urw: upickle.default.ReadWriter[Data] = upickle.default.macroRW
  }

  val benchmarkSampleData: Seq[Data] = Seq.fill(1000)(
    Data(
      c = C("lol i am a noob", "haha you are a noob"): A,
      e = ADTc(i = 1234567890, s = "i am a strange loop"),
      f = ADT0()
    )
  )

  case class DataTrunc(
    c: A,
    e: ADTc,
    f: ADT0
  )

  object DataTrunc {

    implicit val rw: FromTo[DataTrunc] = macroFromTo
    implicit val urw: upickle.default.ReadWriter[DataTrunc] = upickle.default.macroRW
  }

  val benchmarkSampleDataTrunc: Seq[DataTrunc] = Seq.fill(1000)(
    DataTrunc(
      c = C("lol i am a noob", "haha you are a noob"): A,
      e = ADTc(i = 1234567890, s = "i am a strange loop"),
      f = ADT0()
    )
  )

  @dropDefault
  case class FlatPrimitives(
    i: Int,
    s: String = "",
    b: Boolean = true,
    l: Long = Long.MaxValue,
    d: Double = Double.NaN,
    c: Char = '!'
  )

  object FlatPrimitives {

    implicit val pickler: FromTo[FlatPrimitives] = macroFromTo
    implicit val upickler: upickle.default.ReadWriter[FlatPrimitives] = upickle.default.macroRW
  }

  val benchmarkFlatPrimitives = FlatPrimitives(Int.MinValue, "", true, Long.MaxValue, Double.MaxValue, '!')

  case class FlatPrimitivesTrunc(
    i: Int
  )

  object FlatPrimitivesTrunc {

    implicit val pickler: FromTo[FlatPrimitivesTrunc] = macroFromTo
    implicit val upickler: upickle.default.ReadWriter[FlatPrimitivesTrunc] = upickle.default.macroRW
  }

  val benchmarkFlatPrimitivesTrunc = FlatPrimitivesTrunc(Int.MinValue)
}

case class HandrolledFlatPrimitives(
  i: Int = 42,
  s: String = "",
  b: Boolean = true,
  l: Long = Long.MaxValue,
  d: Double = Double.NaN,
  c: Char = '!'
)

object HandrolledFlatPrimitives {

  implicit val pickleTo: WeePickle.To[HandrolledFlatPrimitives] = WeePickle.macroTo[HandrolledFlatPrimitives]
  implicit val picklerFrom: WeePickle.From[HandrolledFlatPrimitives] = new From[HandrolledFlatPrimitives] {
    override def transform0[Out](
      in: HandrolledFlatPrimitives,
      out: Visitor[_, Out]
    ): Out = {
      val obj = out.visitObject(6).narrow

      obj.visitKeyValue(obj.visitKey().visitString("i"))
      obj.visitValue(obj.subVisitor.visitInt32(in.i))

      obj.visitKeyValue(obj.visitKey().visitString("s"))
      obj.visitValue(obj.subVisitor.visitString(in.s))

      obj.visitKeyValue(obj.visitKey().visitString("b"))
      obj.visitValue(if (in.b) obj.subVisitor.visitTrue() else obj.subVisitor.visitFalse())

      obj.visitKeyValue(obj.visitKey().visitString("l"))
      obj.visitValue(obj.subVisitor.visitInt64(in.l))

      obj.visitKeyValue(obj.visitKey().visitString("d"))
      obj.visitValue(obj.subVisitor.visitFloat64(in.d))

      obj.visitKeyValue(obj.visitKey().visitString("c"))
      obj.visitValue(obj.subVisitor.visitChar(in.c))

      obj.visitEnd()
    }
  }
  implicit val writer: default.Reader[HandrolledFlatPrimitives] = upickle.default.macroR[HandrolledFlatPrimitives]
  implicit val reader: default.Writer[HandrolledFlatPrimitives] = new upickle.default.Writer[HandrolledFlatPrimitives] {
    override def write0[V](out: core.Visitor[_, V], v: HandrolledFlatPrimitives): V = {
      val obj = out.visitObject(6, true, -1).narrow

      obj.visitKeyValue(obj.visitKey(-1).visitString("i", -1))
      obj.visitValue(obj.subVisitor.visitInt32(Int.MinValue, -1), -1)

      obj.visitKeyValue(obj.visitKey(-1).visitString("s", -1))
      obj.visitValue(obj.subVisitor.visitString("", -1), -1)

      obj.visitKeyValue(obj.visitKey(-1).visitString("b", -1))
      obj.visitValue(obj.subVisitor.visitTrue(-1), -1)

      obj.visitKeyValue(obj.visitKey(-1).visitString("l", -1))
      obj.visitValue(obj.subVisitor.visitInt64(Long.MaxValue, -1), -1)

      obj.visitKeyValue(obj.visitKey(-1).visitString("d", -1))
      obj.visitValue(obj.subVisitor.visitFloat64(Double.MaxValue, -1), -1)

      obj.visitKeyValue(obj.visitKey(-1).visitString("c", -1))
      obj.visitValue(obj.subVisitor.visitChar('!', -1), -1)

      obj.visitEnd(-1)
    }
  }
}
