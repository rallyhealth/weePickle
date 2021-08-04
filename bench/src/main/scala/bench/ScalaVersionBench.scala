package bench

import com.rallyhealth.weepickle.v1.WeePickle._
import com.rallyhealth.weepickle.v1.core.{FromInput, NoOpVisitor, Visitor}
import com.rallyhealth.weepickle.v1.implicits.dropDefault
import org.openjdk.jmh.annotations._
import org.openjdk.jmh.infra.Blackhole

import java.nio.charset.StandardCharsets.UTF_8
import java.util.concurrent.TimeUnit

/**
 * Benchmark to exercise the macro-generated logic (i.e., mapping to/from case classes),
 * where the implementation (and performance) differ quite a bit between Scala versions.
 *
 * ==Run with==
 * bench / Jmh / run .*ScalaVersionBench.from
 *
 */
@Warmup(iterations = 3, time = 5, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 5, timeUnit = TimeUnit.SECONDS)
@State(Scope.Benchmark)
@BenchmarkMode(Array(Mode.Throughput))
@Fork(
  jvmArgsAppend = Array(
//        "-XX:+UnlockCommercialFeatures",
//        "-XX:+FlightRecorder",
//        "-XX:StartFlightRecording=delay=10s,duration=20s,filename=recording.jfr,settings=profile",
    "-Xms350m",
    "-Xmx350m",
    "-XX:+HeapDumpOnOutOfMemoryError",
    // https://stackoverflow.com/questions/32047440/different-benchmarking-results-between-forks-in-jmh
    "-XX:-BackgroundCompilation",
    "-XX:-TieredCompilation"
  ),
  value = 10
)
class ScalaVersionBench {

  import ScalaVersionBench.{Data, benchmarkFlatPrimitives, benchmarkSampleData}

  private val source: FromInput = FromScala(benchmarkSampleData).transform(ToValue)
  def visitor(bh: Blackhole) = new BlackholeVisitor(bh)

  @OutputTimeUnit(TimeUnit.MILLISECONDS)
  @Benchmark
  def fromFlatPrimitives(bh: Blackhole) = FromScala(benchmarkFlatPrimitives).transform(visitor(bh))

  @Benchmark
  def toUpperbound(bh: Blackhole) = source.transform(visitor(bh))

  def testableFromSample[T](visitor: Visitor[Any, T]) = FromScala(benchmarkSampleData).transform(visitor)

  @Benchmark
  def fromSample(bh: Blackhole) = testableFromSample(visitor(bh))

  @Benchmark
  def toSample: Seq[Data] = source.transform(ToScala[Seq[Data]])

  @Benchmark
  def toBlackhole(bh: Blackhole) = source.transform(visitor(bh))
}

object ScalaVersionBench {

  case class ADT0()
  object ADT0 {
    implicit val rw: FromTo[ADT0] = macroFromTo
  }
  case class ADTa(i: Int)
  object ADTa {
    implicit val rw: FromTo[ADTa] = macroFromTo
  }
  case class ADTc(i: Int = 2, s: String, t: (Double, Double) = (1, 2))
  object ADTc {
    implicit val rw: FromTo[ADTc] = macroFromTo
  }

  sealed trait A
  object A {
    implicit val rw: FromTo[A] = FromTo.merge(B.rw, C.rw)
  }
  case class B(i: Int) extends A
  object B {
    implicit val rw: FromTo[B] = macroFromTo
  }
  case class C(s1: String, s2: String) extends A
  object C {
    implicit val rw: FromTo[C] = macroFromTo
  }

  sealed trait LL
  object LL {
    implicit val rw: FromTo[LL] = FromTo.merge(macroFromTo[End.type], macroFromTo[Node])
  }
  case object End extends LL
  case class Node(c: Int, next: LL) extends LL
  object Node {
    implicit val rw: FromTo[Node] = macroFromTo
  }

  case class Data( // no type parameters - Scala 3 implementation can't support them yet
    a: Seq[(Int, Int)],
    b: String,
    c: A,
    d: LL, // no linked list - Scala 3 implementation can't support the recursion yet
    e: ADTc,
    f: ADT0
  )
  object Data {
    implicit val rw: FromTo[Data] = macroFromTo
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
      Node(-11, Node(-22, Node(-33, Node(-44, End)))): LL,
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
  }

  val benchmarkFlatPrimitives = FlatPrimitives(Int.MinValue, "", true, Long.MaxValue, Double.NaN, '!')

}


/**
 * Same, but with defaults dropped
 *
 */
@Warmup(iterations = 3, time = 5, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 5, timeUnit = TimeUnit.SECONDS)
@State(Scope.Benchmark)
@BenchmarkMode(Array(Mode.Throughput))
@Fork(
  jvmArgsAppend = Array(
    //        "-XX:+UnlockCommercialFeatures",
    //        "-XX:+FlightRecorder",
    //        "-XX:StartFlightRecording=delay=10s,duration=20s,filename=recording.jfr,settings=profile",
    "-Xms350m",
    "-Xmx350m",
    "-XX:+HeapDumpOnOutOfMemoryError",
    // https://stackoverflow.com/questions/32047440/different-benchmarking-results-between-forks-in-jmh
    "-XX:-BackgroundCompilation",
    "-XX:-TieredCompilation"
  ),
  value = 10
)
class ScalaVersionDefaultBench {

  import ScalaVersionDefaultBench.{Data, benchmarkSampleData, benchmarkSampleDataTrunc}

  private val source: FromInput = FromScala(benchmarkSampleData).transform(ToValue)
  private val sourceTrunc: FromInput = FromScala(benchmarkSampleDataTrunc).transform(ToValue)
  def visitor(bh: Blackhole): Visitor[Any, Null] = new BlackholeVisitor(bh)

  @Benchmark
  def toUpperbound(bh: Blackhole) = source.transform(visitor(bh))

  def testableFromSample[T](visitor: Visitor[Any, T]) = FromScala(benchmarkSampleData).transform(visitor)

  @Benchmark
  def fromSample(bh: Blackhole) = testableFromSample(visitor(bh))

  @Benchmark
  def toSample: Seq[Data] = sourceTrunc.transform(ToScala[Seq[Data]])

  @Benchmark
  def toBlackhole(bh: Blackhole) = source.transform(visitor(bh))
}

object ScalaVersionDefaultBench {

  import ScalaVersionBench.{ADT0, ADTc, A, C, LL, End}

  case class Data(
    @dropDefault
    a: Seq[(Int, Int)] = Nil,
    @dropDefault
    b: String = "",
    c: A,
    @dropDefault
    d: LL = End,
    e: ADTc,
    f: ADT0
  )
  object Data {
    implicit val rw: FromTo[Data] = macroFromTo
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
  }
  val benchmarkSampleDataTrunc: Seq[DataTrunc] = Seq.fill(1000)(
    DataTrunc(
      c = C("lol i am a noob", "haha you are a noob"): A,
      e = ADTc(i = 1234567890, s = "i am a strange loop"),
      f = ADT0()
    )
  )
}
