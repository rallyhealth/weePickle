package bench

import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

import com.rallyhealth.weejson.v1.jackson.{FromJson, JsonRenderer, ToJson, VisitorJsonGenerator}
import com.rallyhealth.weejson.v1.jsoniter_scala.FromJsoniterScala
import com.rallyhealth.weepack.v1.{FromMsgPack, ToMsgPack}
import com.rallyhealth.weepickle.v1.Common
import com.rallyhealth.weepickle.v1.Common.Data
import com.rallyhealth.weepickle.v1.WeePickle._
import com.rallyhealth.weepickle.v1.core.{CallbackVisitor, FromInput, Visitor}
import org.msgpack.jackson.dataformat.MessagePackFactory
import org.openjdk.jmh.annotations._
import org.openjdk.jmh.infra.Blackhole

@Warmup(iterations = 3, time = 5, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 2, time = 5, timeUnit = TimeUnit.SECONDS)
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
  value = 1
)
abstract class BenchOptions

/**
  * mill bench.jvm.runJmh GeneratorBench
  *
  * java 8:
  * {{{
  * Benchmark                       Mode  Cnt    Score    Error  Units
  * GeneratorBench.jsonBytes       thrpt    9  273.819 ± 22.893  ops/s
  * GeneratorBench.jsonString      thrpt    9  255.608 ± 11.505  ops/s
  * GeneratorBench.msgpackJackson  thrpt    9  144.934 ±  2.270  ops/s
  * GeneratorBench.msgpackScala    thrpt    9  219.204 ±  4.257  ops/s
  * GeneratorBench.smile           thrpt    9  352.632 ± 14.197  ops/s
  * }}}
  *
  * java 11:
  * {{{
  * Benchmark                       Mode  Cnt    Score    Error  Units
  * GeneratorBench.jsonBytes       thrpt   15  238.335 ± 11.777  ops/s
  * GeneratorBench.jsonString      thrpt   15  240.125 ±  7.871  ops/s
  * GeneratorBench.msgpackJackson  thrpt   15  181.195 ±  5.774  ops/s
  * GeneratorBench.msgpackScala    thrpt   15  304.540 ±  2.225  ops/s
  * GeneratorBench.smile           thrpt   15  306.462 ±  3.134  ops/s
  * }}}
  */
class GeneratorBench extends BenchOptions {

  private val source: FromInput = FromScala(Common.benchmarkSampleData)

  @Benchmark
  def jsonBytes = {
    source.transform(ToJson.bytes)
  }

  @Benchmark
  def jsonString = {
    source.transform(ToJson.string)
  }

  @Benchmark
  def msgpackScala = {
    source.transform(ToMsgPack.bytes)
  }

  @Benchmark
  def msgpackJackson = {
    val baos = new ByteArrayOutputStream()
    val visitor = JsonRenderer(DefaultMessagePackFactory.Instance.createGenerator(baos))
    source.transform(visitor)
    visitor.close()
    baos.toByteArray
  }

  @Benchmark
  def smile = {
    source.transform(ToSmile.bytes)
  }
}

/**
  * mill bench.jvm.runJmh ParserBench
  *
  * java 8:
  * {{{
  * Benchmark                    Mode  Cnt    Score   Error  Units
  * ParserBench.jsonBytes       thrpt    9  258.390 ± 2.847  ops/s
  * ParserBench.jsonString      thrpt    9  239.949 ± 6.586  ops/s
  * ParserBench.msgpackJackson  thrpt    9  210.577 ± 1.618  ops/s
  * ParserBench.msgpackScala    thrpt    9  329.216 ± 6.331  ops/s
  * ParserBench.smile           thrpt    9  309.749 ± 5.861  ops/s
  * }}}
  *
  * java 11:
  * {{{
  * Benchmark                    Mode  Cnt    Score    Error  Units
  * ParserBench.jsonBytes       thrpt   15  245.665 ±  3.202  ops/s
  * ParserBench.jsonString      thrpt   15  213.312 ±  5.250  ops/s
  * ParserBench.msgpackJackson  thrpt   15  205.738 ±  2.789  ops/s
  * ParserBench.msgpackScala    thrpt   15  422.313 ± 17.172  ops/s
  * ParserBench.smile           thrpt   15  271.947 ±  1.116  ops/s
  * }}}
  */
class ParserBench extends BenchOptions {

  private val toCaseClass: Visitor[_, Seq[Data]] = ToScala[Seq[Data]]

  @Benchmark
  def jsonBytes = {
    FromJson(Common.benchmarkSampleJsonBytes).transform(toCaseClass)
  }

  @Benchmark
  def jsonBytesJis = {
    FromJsoniterScala(Common.benchmarkSampleJsonBytes).transform(toCaseClass)
  }

  @Benchmark
  def jsonString = {
    FromJson(Common.benchmarkSampleJson).transform(toCaseClass)
  }

  @Benchmark
  def jsonStringJis = {
    FromJsoniterScala(Common.benchmarkSampleJson).transform(toCaseClass)
  }

  @Benchmark
  def msgpackJackson = {
    val parser = DefaultMessagePackFactory.Instance.createParser(Common.benchmarkSampleMsgPack)
    // Can't use JsonFromInput because the parser throws on EOF instead of returning -1. Picky!

    var result: Option[Seq[Data]] = None
    val generator = new VisitorJsonGenerator(
      new CallbackVisitor(toCaseClass)((data: Seq[Data]) => result = Some(data))
    )

    parser.nextToken()
    generator.copyCurrentStructure(parser)
    parser.close()
    generator.close()

    result.get
  }

  @Benchmark
  def msgpackScala = {
    FromMsgPack(Common.benchmarkSampleMsgPack).transform(toCaseClass)
  }

  @Benchmark
  def smile = {
    val result = FromSmile(SmileBytes.smileBytes).transform(toCaseClass)
    result
  }
}

object DefaultMessagePackFactory {

  val Instance = new MessagePackFactory
}

object SmileBytes {

  val smileBytes = FromJson(Common.benchmarkSampleJsonBytes).transform(ToSmile.bytes)
}
