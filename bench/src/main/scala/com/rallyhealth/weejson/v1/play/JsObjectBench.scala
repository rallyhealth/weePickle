package com.rallyhealth.weejson.v1.play

import com.rallyhealth.weepickle.v1.core.Visitor
import org.openjdk.jmh.annotations._
import play.api.libs.json.{JsArray, JsObject, JsValue}

import java.util.concurrent.TimeUnit

/**
  * Simulates `.transform(PlayJson).as[T]` for various object sizes.
  *
  * ==Quick Run==
  * bench / Jmh / run -f1 -wi 2 -i 3 -p visitor=PlayJson,UnorderedPlayJson -p size=0,1,4,256 .*JsValueBench
  *
  * ==Profile with Flight Recorder==
  * bench / Jmh / run -prof jfr -f1 .*JsValueBench
  *
  * ==Jmh Visualizer Report==
  * bench / Jmh / run -prof gc -rf json -rff JsValueBench-results.json .*JsValueBench
  *
  * {{{
  * Benchmark            (size)          (visitor)   Mode  Cnt       Score      Error   Units
  * JsValueBench.jsArray      0           PlayJson  thrpt    6  148997.034 ± 3110.524  ops/ms
  * JsValueBench.jsArray      0  UnorderedPlayJson  thrpt    6  148811.991 ± 2403.647  ops/ms
  * JsValueBench.jsArray      1           PlayJson  thrpt    6   14538.941 ±  958.018  ops/ms
  * JsValueBench.jsArray      1  UnorderedPlayJson  thrpt    6   14872.016 ±  200.060  ops/ms
  * JsValueBench.jsArray     10           PlayJson  thrpt    6    6454.029 ±  111.689  ops/ms
  * JsValueBench.jsArray     10  UnorderedPlayJson  thrpt    6    6459.146 ±  181.786  ops/ms
  * JsValueBench.jsObject     0           PlayJson  thrpt    6  152411.656 ± 2513.943  ops/ms
  * JsValueBench.jsObject     0  UnorderedPlayJson  thrpt    6  151380.994 ± 3999.946  ops/ms
  * JsValueBench.jsObject     1           PlayJson  thrpt    6    8474.548 ±  149.389  ops/ms
  * JsValueBench.jsObject     1  UnorderedPlayJson  thrpt    6    8510.589 ±  245.733  ops/ms
  * JsValueBench.jsObject    10           PlayJson  thrpt    6    1987.599 ±   32.466  ops/ms
  * JsValueBench.jsObject    10  UnorderedPlayJson  thrpt    6    1560.828 ±   30.813  ops/ms
  * }}}
  * @see https://github.com/ktoso/sbt-jmh
  */
@Warmup(iterations = 3, time = 5, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 3, time = 5, timeUnit = TimeUnit.SECONDS)
@State(Scope.Benchmark)
@BenchmarkMode(Array(Mode.Throughput))
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Fork(jvmArgsAppend = Array("-Xmx350m", "-XX:+HeapDumpOnOutOfMemoryError", "-XX:-BackgroundCompilation", "-XX:-TieredCompilation"), value = 1)
class JsValueBench {

  @Param(Array("PlayJson", "UnorderedPlayJson"))
  var visitor: String = _
  var v: Visitor[JsValue, JsValue] = _

  @Param(Array("0", "1", "4", "8", "32", "64", "256", "1024"))
  var size: Int = _
  var strings: Array[String] = _

  @Setup def setup: Unit = {
    v = visitor match {
      case "PlayJson" => PlayJson
      case "UnorderedPlayJson" => UnorderedPlayJson
      case o => throw new IllegalArgumentException(o)
    }
    strings = (0 until size).map(_.toString).toArray // cached String.hashcode
  }

  @Benchmark def jsObject: JsValue = {
    var i = 0
    val o = v.visitObject(-1).narrow
    while (i < size) {
      o.visitKeyValue(o.visitKey().visitString(strings(i)))
      o.visitValue(o.subVisitor.visitNull())
      i += 1
    }
    val jsValue = o.visitEnd()
    simulateCaseClassMapping(jsValue)
    jsValue
  }

  @Benchmark def jsArray: JsValue = {
    var i = 0
    val o = v.visitArray(-1).narrow
    while (i < size) {
      o.visitValue(o.subVisitor.visitNull())
      i += 1
    }
    val jsValue = o.visitEnd()
    simulateCaseClassMapping(jsValue)
    jsValue
  }

  private def simulateCaseClassMapping(
    jsValue: JsValue
  ): Unit = {
    jsValue match {
      case JsObject(underlying) =>
        underlying.keysIterator.foreach { key =>
          // Reads macro uses underlying.get(key), not (obj \ "key")!
          // https://github.com/playframework/play-json/pull/674
          underlying.get(key) match {
            case Some(jsValue) => simulateCaseClassMapping(jsValue)
            case _ =>
          }
        }
      case JsArray(value) =>
        value.foreach(simulateCaseClassMapping)
      case _ => ()
    }
  }
}
