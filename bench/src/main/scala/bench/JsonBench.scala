package bench

import com.rallyhealth.weejson.v1.jackson.{FromJson, ToJson}
import com.rallyhealth.weepickle.v1.Common
import com.rallyhealth.weepickle.v1.core.NoOpVisitor
import org.openjdk.jmh.annotations._
import ujson.transform

import java.util.concurrent.TimeUnit

/**
 * Tests Json parsing only.
 *
 * ==Run with==
 * bench / Jmh / run .*ParseBytesBench
 *
 *
 * 11.0.11.hs-adpt:
 * {{{
 * [info] Benchmark                              Mode  Cnt    Score    Error  Units
 * [info] ParseBytesBench.uJsonNoTrace          thrpt   15  423.969 ± 57.024  ops/s
 * [info] ParseBytesBench.uJsonTrace            thrpt   15  261.736 ± 19.183  ops/s
 * [info] ParseBytesBench.weePickle             thrpt   15  355.395 ± 11.672  ops/s
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
    "-Xmx350m",
    "-XX:+HeapDumpOnOutOfMemoryError"
  ),
  value = 5
)
class ParseBytesBench {

  @Benchmark
  def uJsonNoTrace: Unit = ujson.validate(Common.benchmarkSampleJsonBytes)

  @Benchmark
  def uJsonTrace: Unit = upickle.core.TraceVisitor.withTrace(true, upickle.core.NoOpVisitor)(transform(Common.benchmarkSampleJsonBytes, _))

  @Benchmark
  def weePickle: Unit = {
    FromJson(Common.benchmarkSampleJsonBytes).transform(NoOpVisitor)
  }
}

/**
 * Parses and generates a json String.
 *
 * ==Run with==
 * bench / Jmh / run .*ParseBytesToStringBench
 *
 * 11.0.11.hs-adpt:
 * {{{
 * [info] Benchmark                              Mode  Cnt    Score    Error  Units
 * [info] ParseBytesToStringBench.uJsonNoTrace  thrpt   15  193.519 ± 16.052  ops/s
 * [info] ParseBytesToStringBench.uJsonTrace    thrpt   15  159.625 ±  7.865  ops/s
 * [info] ParseBytesToStringBench.weePickle     thrpt   15  195.580 ±  1.651  ops/s
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
    "-Xmx350m",
    "-XX:+HeapDumpOnOutOfMemoryError"
  ),
  value = 5
)
class ParseBytesToStringBench {

  @Benchmark
  def uJsonTrace: String = {
    upickle.core.TraceVisitor.withTrace(true, ujson.StringRenderer())(transform(Common.benchmarkSampleJsonBytes, _)).toString
  }

  @Benchmark
  def uJsonNoTrace: String = ujson.reformat(Common.benchmarkSampleJsonBytes)

  @Benchmark
  def weePickle: String = FromJson(Common.benchmarkSampleJsonBytes).transform(ToJson.string)
}



