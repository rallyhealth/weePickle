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

  implicit val pickler: WeePickle.FromTo[FastSuit.Value] = WeePickle.fromToEnumerationName(this)
}

/**
  * ==Run with==
  * bench / Jmh / run .*FastEnumBench
  *
  * java 8:
  * {{{
  * [info] Benchmark             Mode  Cnt    Score    Error  Units
  * [info] FastEnumBench.from    avgt    3   12.936 ±  1.386  ns/op
  * [info] FastEnumBench.toHit   avgt    3   10.402 ±  3.830  ns/op
  * [info] FastEnumBench.toMiss  avgt    3  441.950 ± 72.002  ns/op
  * }}}
  */
class FastEnumBench extends EnumBench(FastSuit.Diamonds)

object SlowSuit extends Enumeration {

  // toString() has to synchronize {} and do a map lookup

  val Spades = Value
  val Hearts = Value
  val Clubs = Value
  val Diamonds = Value

  implicit val pickler: WeePickle.FromTo[SlowSuit.Value] = WeePickle.fromToEnumerationName(this)
}

/**
  * ==Run with==
  * bench / Jmh / run .*SlowEnumBench
  *
  * java 8:
  * {{{
  * [info] Benchmark             Mode  Cnt    Score    Error  Units
  * [info] SlowEnumBench.from    avgt    3   12.764 ±  0.808  ns/op
  * [info] SlowEnumBench.toHit   avgt    3   10.262 ±  6.775  ns/op
  * [info] SlowEnumBench.toMiss  avgt    3  421.431 ± 34.023  ns/op
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
