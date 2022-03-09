package bench

import com.rallyhealth.weejson.v1.BufferedValue
import com.rallyhealth.weejson.v1.wee_jsoniter_scala.FromJsoniterScala
import org.openjdk.jmh.annotations._

import java.util.concurrent.TimeUnit

/**
  * Quick and dirty test to see how badly we're butchering performance of floats.
  *
  * ==Quick Run==
  * bench / Jmh / run .*JsoniterScalaBench
  *
  * ==Profile with Flight Recorder==
  * bench / Jmh / run -prof jfr .*JsoniterScalaBench
  *
  * ==Jmh Visualizer Report==
  * bench / Jmh / run -prof gc -rf json -rff JsoniterScalaBench-results.json .*JsoniterScalaBench
  *
  * @see https://github.com/ktoso/sbt-jmh
  */
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 15, time = 1, timeUnit = TimeUnit.SECONDS)
@State(Scope.Benchmark)
@BenchmarkMode(Array(Mode.Throughput))
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Fork(
  jvmArgsAppend =
    Array("-Xmx350m", "-XX:+HeapDumpOnOutOfMemoryError", "-XX:-BackgroundCompilation", "-XX:-TieredCompilation"),
  value = 1
)
class JsoniterScalaBench {

  /**
    * Values that end with a number throw an expensive exception internally when reaching EOF.
    * The only time this would happen in the wild would be when parsing a JSON text of a single number.
    * To make this more realistic, we're intentionally adding a whitespace suffix here.
    */
  private val piBytes = "-3.14 ".getBytes()

  @Benchmark
  def pi = FromJsoniterScala(piBytes).transform(BufferedValue.Builder)

}
