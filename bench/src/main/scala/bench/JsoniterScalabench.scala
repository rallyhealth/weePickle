package bench

import com.rallyhealth.weejson.v1.wee_jsoniter_scala.FromJsoniterScala
import com.rallyhealth.weepickle.v1.WeePickle.ToScala
import org.openjdk.jmh.annotations._

import java.util.concurrent.TimeUnit

/**
  * ==Quick Run==
  * bench / Jmh / run -f1 -wi 2 -i 3 .*JsoniterScalabench
  *
  * ==Profile with Flight Recorder==
  * bench / Jmh / run -prof jfr -f1 .*JsoniterScalabench
  *
  * ==Jmh Visualizer Report==
  * bench / Jmh / run -prof gc -rf json -rff JsoniterScalabench-results.json .*JsoniterScalabench
  *
  * ==Sample Results==
  * {{{
  * TODO
  * }}}
  *
  * @see https://github.com/ktoso/sbt-jmh
  */
@Warmup(iterations = 15, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 15, time = 1, timeUnit = TimeUnit.SECONDS)
@State(Scope.Benchmark)
@BenchmarkMode(Array(Mode.Throughput)) // or @BenchmarkMode(Array(Mode.AverageTime))
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Fork(jvmArgsAppend =
        Array("-Xmx350m", "-XX:+HeapDumpOnOutOfMemoryError", "-XX:-BackgroundCompilation", "-XX:-TieredCompilation"),
      value = 1)
class JsoniterScalabench {
  private val input = "348249875e105".getBytes()

  @Benchmark
  def parseDouble = {
    FromJsoniterScala(input).transform(ToScala[Double])
  }

}
