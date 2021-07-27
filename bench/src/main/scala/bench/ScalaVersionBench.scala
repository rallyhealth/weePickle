package bench

import com.rallyhealth.weepickle.v1.WeePickle._
import com.rallyhealth.weepickle.v1.core.{FromInput, NoOpVisitor}
import org.openjdk.jmh.annotations._

import java.nio.charset.StandardCharsets.UTF_8
import java.util.concurrent.TimeUnit

/*
 * Benchmark to exercise the macro-generated logic (i.e., mapping to/from case classes),
 * where the implementation (and performance) differ quite a bit between Scala versions.
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
    "-XX:+HeapDumpOnOutOfMemoryError"
  ),
  value = 10
)
class ScalaVersionBench {

  import ScalaVersionBench.{benchmarkSampleData, Data}

  private val source: FromInput = FromScala(benchmarkSampleData)
  private val returnDone: NoOpVisitor[String] = new NoOpVisitor("done")

  @Benchmark
  def fromSample: String = FromScala(benchmarkSampleData).transform(returnDone)

  @Benchmark
  def toSample: Seq[Data] = source.transform(ToScala[Seq[Data]])
}

object ScalaVersionBench {

  import com.rallyhealth.weepickle.v1.ADTs.ADT0
  import com.rallyhealth.weepickle.v1.Defaults.ADTc
  import com.rallyhealth.weepickle.v1.Hierarchy.{A, C}

  case class Data( // no type parameters - Scala 3 implementation can't support them yet
    a: Seq[(Int, Int)],
    b: String,
    c: A,
    // no linked list - Scala 3 implementation can't support the recursion yet
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
      ADTc(i = 1234567890, s = "i am a strange loop"),
      ADT0()
    )
  )
}
