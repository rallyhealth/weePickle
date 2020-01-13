package bench

import java.io.{ByteArrayOutputStream, StringWriter}
import java.util.concurrent.TimeUnit

import com.rallyhealth.weejson.v0.jackson.{DefaultJsonFactory, JsonGeneratorOutputStream, VisitorJsonGenerator, WeeJackson}
import com.rallyhealth.weepickle.v0.WeePickle._
import com.rallyhealth.weepickle.v0.{Common, WeePickle}
import com.rallyhealth.weepickle.v1.core.CallbackVisitor
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
@Warmup(iterations = 8, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 10, time = 1, timeUnit = TimeUnit.SECONDS)
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
  value = 2
)
class JmhBench {

  @Benchmark
  def bytesToCcWeeJackson(bh: Blackhole): Unit = {
    bh.consume(WeeJackson.parse(Common.benchmarkSampleJsonBytes).transform(reader[Seq[Common.Data]]))
  }

  @Benchmark
  def bytesToCcWeeJacksonAsync(bh: Blackhole): Unit = {
    var result: Option[Seq[Common.Data]] = None
    val out = new JsonGeneratorOutputStream(new VisitorJsonGenerator(new CallbackVisitor(reader[Seq[Common.Data]])(data => result = Some(data))))
    out.write(Common.benchmarkSampleJsonBytes)
    out.close()
    bh.consume(result)
  }

  @Benchmark
  def bytesToCcWeeJson(bh: Blackhole): Unit = {
    bh.consume(WeePickle.read[Seq[Common.Data]](Common.benchmarkSampleJsonBytes))
  }

  @Benchmark
  def bytesToCcWeePack(bh: Blackhole): Unit = {
    bh.consume(WeePickle.readMsgPack[Seq[Common.Data]](Common.benchmarkSampleMsgPack))
  }

  @Benchmark
  def ccToBytesWeeJackson(bh: Blackhole): Unit = {
    val out = new ByteArrayOutputStream()
    val visitor = WeeJackson.visitor(DefaultJsonFactory.Instance.createGenerator(out))
    WeePickle.transform(Common.benchmarkSampleData).transform(visitor).close()
    bh.consume(out.toByteArray)
  }

  @Benchmark
  def ccToBytesWeeJson(bh: Blackhole): Unit = {
    val out = new ByteArrayOutputStream()
    WeePickle.stream(Common.benchmarkSampleData).writeBytesTo(out)
    bh.consume(out.toByteArray)
  }

  @Benchmark
  def ccToStringWeeJackson(bh: Blackhole): Unit = {
    val writer = new StringWriter()
    val visitor = WeeJackson.visitor(DefaultJsonFactory.Instance.createGenerator(writer))
    WeePickle.transform(Common.benchmarkSampleData).to(visitor).close()
    bh.consume(writer.toString)
  }

  @Benchmark
  def ccToStringWeeJson(bh: Blackhole): Unit = {
    bh.consume(WeePickle.write(Common.benchmarkSampleData))
  }

  @Benchmark
  def ccToBytesWeePack(bh: Blackhole): Unit = {
    bh.consume(WeePickle.writeMsgPack(Common.benchmarkSampleData))
  }

  @Benchmark
  def stringToCcWeeJackson(bh: Blackhole): Unit = {
    bh.consume(WeeJackson.parse(Common.benchmarkSampleJson).transform(reader[Seq[Common.Data]]))
  }

  @Benchmark
  def stringToCcWeeJson(bh: Blackhole): Unit = {
    bh.consume(WeePickle.read[Seq[Common.Data]](Common.benchmarkSampleJson))
  }
}
