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
 * where the implementation (and performance) differ quite a bit between Scala versions,
 * especially with respect to default value processing.
 *
 * ==Run with==
 * +bench / Jmh / run .ScalaVersionBench.*
 * +bench / Jmh / run .ScalaVersionDefaultBench.*
 *
 * Here are some example results from 8/3 - 8/4/2021 informal (-f 4) runs:
 *
 *  Version  Benchmark                                    Mode  Cnt     Score     Error Units
 *   2.13.5: ScalaVersionBench.fromFlatPrimitives        thrpt   20  13202.890 ± 102.405  ops/ms
 *   2.13.5: ScalaVersionBench.toFlatPrimitives          thrpt   20    192.373 ±   1.867  ops/ms
 *   2.13.5: ScalaVersionBench.fromSample                thrpt   20   1006.202 ±  11.330  ops/s
 *   2.13.5: ScalaVersionBench.toSample                  thrpt   20    314.882 ±   7.125  ops/s
 *   2.13.5: ScalaVersionBench.toUpperbound              thrpt   20    713.025 ±  30.174  ops/s
 *
 *   2.13.5: ScalaVersionDefaultBench.fromFlatPrimitives thrpt   20  22038.074 ±  38.954  ops/ms
 *   2.13.5: ScalaVersionDefaultBench.toFlatPrimitives   thrpt   20  24854.633 ± 214.081  ops/ms
 *   2.13.5: ScalaVersionDefaultBench.fromSample         thrpt   20   4188.035 ±  85.052  ops/s
 *   2.13.5: ScalaVersionDefaultBench.toSample           thrpt   20   1546.885 ±  12.538  ops/s
 *   2.13.5: ScalaVersionDefaultBench.toUpperbound       thrpt   20   2936.679 ±  36.324  ops/s
 *
 *   3.0.1:  ScalaVersionBench.fromFlatPrimitives        thrpt   20   4653.536 ±  35.851  ops/ms // ~65% less
 *   3.0.1:  ScalaVersionBench.toFlatPrimitives          thrpt   20    186.453 ±   1.404  ops/ms // ~the same
 *   3.0.1:  ScalaVersionBench.fromSample                thrpt   20    733.122 ±   5.783  ops/s  // ~25% less
 *   3.0.1:  ScalaVersionBench.toSample                  thrpt   20    346.230 ±   9.235  ops/s  // ~10% more
 *   3.0.1:  ScalaVersionBench.toUpperbound              thrpt   20    717.988 ±  30.697  ops/s  // ~the same
 *
 *   3.0.1:  ScalaVersionDefaultBench.fromFlatPrimitives thrpt   20   4157.921 ±  37.427  ops/ms // ~80% less
 *   3.0.1:  ScalaVersionDefaultBench.toFlatPrimitives   thrpt   20   7424.267 ± 900.835  ops/ms // ~70% less
 *   3.0.1:  ScalaVersionDefaultBench.fromSample         thrpt   20   1728.697 ±  20.222  ops/s  // ~60% less
 *   3.0.1:  ScalaVersionDefaultBench.toSample           thrpt   20   1165.583 ±  14.725  ops/s  // ~25% less
 *   3.0.1:  ScalaVersionDefaultBench.toUpperbound       thrpt   20   2308.280 ±  19.467  ops/s  // ~20% less
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

  import ScalaVersionBench.{Data, FlatPrimitives, benchmarkFlatPrimitives, benchmarkSampleData}

  private val flatPrimitivesSource: FromInput = FromScala(benchmarkFlatPrimitives).transform(ToValue)
  private val sampleDataSource: FromInput = FromScala(benchmarkSampleData).transform(ToValue)
  def visitor(bh: Blackhole) = new BlackholeVisitor(bh)

  @OutputTimeUnit(TimeUnit.MILLISECONDS)
  @Benchmark
  def fromFlatPrimitives(bh: Blackhole) = FromScala(benchmarkFlatPrimitives).transform(visitor(bh))

  @OutputTimeUnit(TimeUnit.MILLISECONDS)
  @Benchmark
  def toFlatPrimitives: FlatPrimitives = flatPrimitivesSource.transform(ToScala[FlatPrimitives])

  @Benchmark
  def toUpperbound(bh: Blackhole) = sampleDataSource.transform(visitor(bh))

  def testableFromSample[T](visitor: Visitor[Any, T]) = FromScala(benchmarkSampleData).transform(visitor)

  @Benchmark
  def fromSample(bh: Blackhole) = testableFromSample(visitor(bh))

  @Benchmark
  def toSample: Seq[Data] = sampleDataSource.transform(ToScala[Seq[Data]])
}

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
class ScalaVersionUBench {
  import ScalaVersionBench.{Data, FlatPrimitives, benchmarkFlatPrimitives, benchmarkSampleData}
  /*
   * Micropickle equivalents
   */
  private val flatPrimitivesSource: ujson.Value = upickle.default.transform(benchmarkFlatPrimitives).to[ujson.Value]
  private val sampleDataSource: ujson.Value = upickle.default.transform(benchmarkSampleData).to[ujson.Value]
  def uVisitor(bh: Blackhole): upickle.core.Visitor[_, _] = new BlackholeUVisitor(bh) // upickle.core.NoOpVisitor

  @OutputTimeUnit(TimeUnit.MILLISECONDS)
  @Benchmark
  def fromFlatPrimitives(bh: Blackhole) = upickle.default.transform(benchmarkFlatPrimitives).to(uVisitor(bh))

  @OutputTimeUnit(TimeUnit.MILLISECONDS)
  @Benchmark
  def toFlatPrimitives: FlatPrimitives = flatPrimitivesSource.transform(FlatPrimitives.upickler)

  @Benchmark
  def toUpperbound(bh: Blackhole) = sampleDataSource.transform(uVisitor(bh))

  @Benchmark
  def fromSample(bh: Blackhole) = upickle.default.transform(benchmarkSampleData).to(uVisitor(bh))

  @Benchmark
  def toSample: Seq[Data] = sampleDataSource.transform(implicitly[upickle.default.Reader[Seq[Data]]])
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

  import ScalaVersionDefaultBench.{Data, FlatPrimitives, benchmarkSampleData, benchmarkSampleDataTrunc, benchmarkFlatPrimitives, benchmarkFlatPrimitivesTrunc}

  private val sampleDataSource: FromInput = FromScala(benchmarkSampleData).transform(ToValue)
  private val sampleDataSourceTrunc: FromInput = FromScala(benchmarkSampleDataTrunc).transform(ToValue)
  private val flatPrimitivesSource: FromInput = FromScala(benchmarkFlatPrimitives).transform(ToValue)
  private val flatPrimitivesSourceTrunc: FromInput = FromScala(benchmarkFlatPrimitivesTrunc).transform(ToValue)
  def visitor(bh: Blackhole): Visitor[Any, Null] = new BlackholeVisitor(bh)

  @OutputTimeUnit(TimeUnit.MILLISECONDS)
  @Benchmark
  def fromFlatPrimitives(bh: Blackhole) = FromScala(benchmarkFlatPrimitives).transform(visitor(bh))

  @OutputTimeUnit(TimeUnit.MILLISECONDS)
  @Benchmark
  def toFlatPrimitives: FlatPrimitives = flatPrimitivesSourceTrunc.transform(ToScala[FlatPrimitives])

  @Benchmark
  def toUpperbound(bh: Blackhole) = sampleDataSource.transform(visitor(bh))

  def testableFromSample[T](visitor: Visitor[Any, T]) = FromScala(benchmarkSampleData).transform(visitor)

  @Benchmark
  def fromSample(bh: Blackhole) = testableFromSample(visitor(bh))

  @Benchmark
  def toSample: Seq[Data] = sampleDataSourceTrunc.transform(ToScala[Seq[Data]])
}

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
class ScalaVersionDefaultUBench {
  import ScalaVersionDefaultBench.{Data, FlatPrimitives, benchmarkSampleData, benchmarkSampleDataTrunc, benchmarkFlatPrimitives, benchmarkFlatPrimitivesTrunc}

  /*
   * Micropickle equivalents
   */
  private val sampleDataSource: ujson.Value = upickle.default.transform(benchmarkSampleData).to[ujson.Value]
  private val sampleDataSourceTrunc: ujson.Value = upickle.default.transform(benchmarkSampleDataTrunc).to[ujson.Value]
  private val flatPrimitivesSource: ujson.Value = upickle.default.transform(benchmarkFlatPrimitives).to[ujson.Value]
  private val flatPrimitivesSourceTrunc: ujson.Value = upickle.default.transform(benchmarkFlatPrimitivesTrunc).to[ujson.Value]
  def uVisitor(bh: Blackhole): upickle.core.Visitor[_, _] = new BlackholeUVisitor(bh) // upickle.core.NoOpVisitor // ?

  @OutputTimeUnit(TimeUnit.MILLISECONDS)
  @Benchmark
  def fromFlatPrimitives(bh: Blackhole) = upickle.default.transform(benchmarkFlatPrimitives).to(uVisitor(bh))

  @OutputTimeUnit(TimeUnit.MILLISECONDS)
  @Benchmark
  def toFlatPrimitives: FlatPrimitives = flatPrimitivesSourceTrunc.transform(FlatPrimitives.upickler)

  @Benchmark
  def toUpperbound(bh: Blackhole) = sampleDataSource.transform(uVisitor(bh))

  @Benchmark
  def fromSample(bh: Blackhole) = upickle.default.transform(benchmarkSampleData).to(uVisitor(bh))

  @Benchmark
  def toSample: Seq[Data] = sampleDataSourceTrunc.transform(implicitly[upickle.default.Reader[Seq[Data]]])
}

object ScalaVersionDefaultBench {

  import ScalaVersionBench.{ADT0, ADTc, A, C, LL, End}

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
    b: Boolean =  true,
    l: Long=  Long.MaxValue,
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
