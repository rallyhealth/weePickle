package bench

import java.util.concurrent.TimeUnit

import com.rallyhealth.weepickle.v1.WeePickle
import com.rallyhealth.weepickle.v1.WeePickle._
import com.rallyhealth.weepickle.v1.core.StringVisitor
import org.openjdk.jmh.annotations._


object FastSuit extends Enumeration {

  val Spades = Value("Spades")
  val Hearts = Value("Hearts")
  val Clubs = Value("Clubs")
  val Diamonds = Value("Diamonds")

  implicit val pickler = WeePickle.fromToEnumerationName(this)
}

/**
  * ==Run with==
  * mill bench.jvm.runJmh FastEnumBench
  *
  * java 8:
  * {{{
  * Benchmark             Mode  Cnt     Score    Error  Units
  * FastEnumBench.from    avgt    3     9.455 ±  2.638  ns/op
  * FastEnumBench.toHit   avgt    3     7.935 ±  0.641  ns/op
  * FastEnumBench.toMiss  avgt    3  1780.004 ± 91.908  ns/op
  * }}}
  */
class FastEnumBench extends EnumBench(FastSuit.Diamonds)

object SlowSuit extends Enumeration {

  // toString() has to synchronize {} and do a map lookup

  val Spades = Value
  val Hearts = Value
  val Clubs = Value
  val Diamonds = Value

  implicit val pickler = WeePickle.fromToEnumerationName(this)
}

/**
  * ==Run with==
  * * mill bench.jvm.runJmh SlowEnumBench
  *
  * java 8:
  * {{{
  * Benchmark             Mode  Cnt     Score    Error  Units
  * SlowEnumBench.from    avgt    3    93.727 ±  6.155  ns/op
  * SlowEnumBench.toHit   avgt    3     7.693 ±  0.864  ns/op
  * SlowEnumBench.toMiss  avgt    3  1830.540 ± 10.293  ns/op
  * }}}
  */
class SlowEnumBench extends EnumBench(SlowSuit.Diamonds)

@Warmup(iterations = 2, time = 5, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 3, time = 5, timeUnit = TimeUnit.SECONDS)
@State(Scope.Benchmark)
@BenchmarkMode(Array(Mode.AverageTime))
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Fork(
  jvmArgsAppend = Array(
    //    "-XX:+UnlockCommercialFeatures",
    //    "-XX:+FlightRecorder",
    //    "-XX:StartFlightRecording=delay=8s,duration=30s,filename=recording.jfr,settings=profile",
    "-Xmx350m",
    "-XX:+HeapDumpOnOutOfMemoryError"
  ),
  value = 1
)
abstract class EnumBench[Value](value: Value)(implicit pickler: FromTo[Value]) {

  private val string = value.toString

  @Benchmark def toHit = pickler.visitString(string)

  @Benchmark def toMiss = try {
    pickler.visitString("not a real enum value")
  } catch {
    case t: Throwable => t
  }

  @Benchmark def from = pickler.transform(value, StringVisitor)
}
