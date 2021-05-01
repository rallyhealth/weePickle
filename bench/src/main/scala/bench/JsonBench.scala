package bench

import java.util.concurrent.TimeUnit

import com.rallyhealth.weejson.v1.jackson.{FromJson, ToJson}
import com.rallyhealth.weepickle.v1.Common
import com.rallyhealth.weepickle.v1.core.NoOpVisitor
import org.openjdk.jmh.annotations._
import org.openjdk.jmh.infra.Blackhole

/**
  * Tests Json parsing only.
  *
  * ==Run with==
  * mill bench.jvm.runJmh ParseBytesBench
  *
  * java 8:
  * {{{
  * Benchmark                   Mode  Cnt    Score   Error  Units
  * ParseBytesBench.uJson      thrpt       250.238          ops/s
  * ParseBytesBench.weePickle  thrpt       392.815          ops/s
  * }}}
  *
  * java 11:
  * {{{
  * Benchmark                   Mode  Cnt    Score    Error  Units
  * ParseBytesBench.uJson      thrpt   15  292.643 ± 13.337  ops/s
  * ParseBytesBench.weePickle  thrpt   15  376.108 ± 21.080  ops/s
  * }}}
  */
@Warmup(iterations = 3, time = 5, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 3, time = 5, timeUnit = TimeUnit.SECONDS)
@State(Scope.Benchmark)
@BenchmarkMode(Array(Mode.Throughput))
@Fork(
  jvmArgsAppend = Array(
    //    "-XX:+UnlockCommercialFeatures",
    //    "-XX:+FlightRecorder",
    //    "-XX:StartFlightRecording=delay=8s,duration=30s,filename=recording.jfr,settings=profile",
    "-Xmx350m",
    "-XX:+HeapDumpOnOutOfMemoryError"
  ),
  value = 5
)
class ParseBytesBench {

  @Benchmark
  def uJson: Unit = {
    ujson.validate(Common.benchmarkSampleJsonBytes)
  }

  @Benchmark
  def weePickle: Unit = {
    FromJson(Common.benchmarkSampleJsonBytes).transform(NoOpVisitor)
  }
}

/**
  * Parses and generates a json String.
  *
  * ==Run with==
  * mill bench.jvm.runJmh ParseBytesToStringBench
  *
  * java 8:
  * {{{
  * Benchmark                           Mode  Cnt    Score   Error  Units
  * ParseBytesToStringBench.uJson      thrpt       106.882          ops/s
  * ParseBytesToStringBench.weePickle  thrpt       220.347          ops/s
  * }}}
  *
  * java 11:
  * {{{
  * Benchmark                           Mode  Cnt    Score   Error  Units
  * ParseBytesToStringBench.uJson      thrpt   15  121.047 ± 2.615  ops/s
  * ParseBytesToStringBench.weePickle  thrpt   15  195.739 ± 8.495  ops/s
  * }}}
  */
@Warmup(iterations = 3, time = 5, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 3, time = 5, timeUnit = TimeUnit.SECONDS)
@State(Scope.Benchmark)
@BenchmarkMode(Array(Mode.Throughput))
@Fork(
  jvmArgsAppend = Array(
    //    "-XX:+UnlockCommercialFeatures",
    //    "-XX:+FlightRecorder",
    //    "-XX:StartFlightRecording=delay=8s,duration=30s,filename=recording.jfr,settings=profile",
    "-Xmx350m",
    "-XX:+HeapDumpOnOutOfMemoryError"
  ),
  value = 5
)
class ParseBytesToStringBench {

  @Benchmark
  def uJson(bh: Blackhole): Unit = {
    bh.consume(ujson.transform(Common.benchmarkSampleJsonBytes, ujson.StringRenderer()).toString)
  }

  @Benchmark
  def weePickle(bh: Blackhole): Unit = {
    bh.consume(FromJson(Common.benchmarkSampleJsonBytes).transform(ToJson.string))
  }
}



