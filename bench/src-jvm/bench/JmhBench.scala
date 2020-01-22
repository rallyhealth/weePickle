package bench

import java.io.{ByteArrayOutputStream, StringWriter}
import java.util.concurrent.TimeUnit

import com.rallyhealth.weejson.v1.jackson.{DefaultJsonFactory, FromJson, JsonRenderer}
import com.rallyhealth.weepack.v1.ToMsgPack
import com.rallyhealth.weepickle.v1.Common
import com.rallyhealth.weepickle.v1.WeePickle._
import org.openjdk.jmh.annotations._
import org.openjdk.jmh.infra.Blackhole

/**
  * ==Run with==
  * mill bench.jvm.runJmh JmhBench
  *
  * {{{
  * Benchmark                           Mode  Cnt    Score   Error  Units
  * JmhBench.bytesToCcWeeJackson       thrpt   20  227.206 ± 4.192  ops/s
  * JmhBench.bytesToCcWeeJacksonAsync  thrpt   20  244.961 ± 9.244  ops/s
  * JmhBench.bytesToCcWeeJson          thrpt   20  167.142 ± 2.077  ops/s
  * JmhBench.bytesToCcWeePack          thrpt   20  322.312 ± 3.798  ops/s
  *
  * JmhBench.ccToBytesWeeJackson       thrpt   20  257.773 ± 2.835  ops/s
  * JmhBench.ccToBytesWeeJson          thrpt   20   51.544 ± 0.552  ops/s
  * JmhBench.ccToBytesWeePack          thrpt   20  222.048 ± 4.454  ops/s
  *
  * JmhBench.ccToStringWeeJackson      thrpt   20  255.146 ± 1.703  ops/s
  * JmhBench.ccToStringWeeJson         thrpt   20  138.532 ± 3.551  ops/s
  *
  * JmhBench.stringToCcWeeJackson      thrpt   20  233.817 ± 3.403  ops/s
  * JmhBench.stringToCcWeeJson         thrpt   20  204.159 ± 3.363  ops/s
  * }}}
  */
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 2, time = 1, timeUnit = TimeUnit.SECONDS)
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
  value = 1
)
class JmhBench {

  @Benchmark
  def bytesToCcWeeJackson(bh: Blackhole): Unit = {
    bh.consume(FromJson(Common.benchmarkSampleJsonBytes).transform(reader[Seq[Common.Data]]))
  }

  @Benchmark
  def bytesToCcWeeJacksonAsync(bh: Blackhole): Unit = {
    bh.consume(FromJson(Common.benchmarkSampleJsonBytes).transform(reader[Seq[Common.Data]]))
  }

//  @Benchmark
//  def bytesToCcWeeJson(bh: Blackhole): Unit = {
//    // TODO replace with upstream uJson parser
////    bh.consume(parser.ByteArrayParser.transform(Common.benchmarkSampleJsonBytes, reader[Seq[Common.Data]]))
//  }

  @Benchmark
  def bytesToCcWeePack(bh: Blackhole): Unit = {
    bh.consume(FromJson(Common.benchmarkSampleMsgPack).transform(ToScala[Seq[Common.Data]]))
  }

  @Benchmark
  def ccToBytesWeeJackson(bh: Blackhole): Unit = {
    val out = new ByteArrayOutputStream()
    val visitor = JsonRenderer(DefaultJsonFactory.Instance.createGenerator(out))
    FromScala(Common.benchmarkSampleData).transform(visitor).close()
    bh.consume(out.toByteArray)
  }

//  @Benchmark
//  def ccToBytesWeeJson(bh: Blackhole): Unit = {
//    // TODO replace with upstream uJson parser
//    bh.consume(FromScala(Common.benchmarkSampleData).transform(parser.BytesRenderer()).toBytes)
//  }

  @Benchmark
  def ccToStringWeeJackson(bh: Blackhole): Unit = {
    val writer = new StringWriter()
    val visitor = JsonRenderer(DefaultJsonFactory.Instance.createGenerator(writer))
    FromScala(Common.benchmarkSampleData).transform(visitor).close()
    bh.consume(writer.toString)
  }

//  @Benchmark
//  def ccToStringWeeJson(bh: Blackhole): Unit = {
//    // TODO replace with upstream uJson parser
//    bh.consume(FromScala(Common.benchmarkSampleData).transform(parser.StringRenderer()).toString)
//  }

  @Benchmark
  def ccToBytesWeePack(bh: Blackhole): Unit = {
    bh.consume(FromScala(Common.benchmarkSampleData).transform(ToMsgPack.bytes))
  }

  @Benchmark
  def stringToCcWeeJackson(bh: Blackhole): Unit = {
    bh.consume(FromJson(Common.benchmarkSampleJson).transform(ToScala[Seq[Common.Data]]))
  }

//  @Benchmark
//  def stringToCcWeeJson(bh: Blackhole): Unit = {
//    // TODO replace with upstream uJson parser
//    bh.consume(parser.StringParser.transform(Common.benchmarkSampleJson, WeePickle.reader[Seq[Common.Data]]))
//  }
}
