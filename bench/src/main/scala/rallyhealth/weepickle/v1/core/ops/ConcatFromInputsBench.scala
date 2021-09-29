package rallyhealth.weepickle.v1.core.ops

import com.rallyhealth.weepickle.v1.core.ops.ConcatFromInputs
import com.rallyhealth.weepickle.v1.core.{FromInput, Visitor}
import org.openjdk.jmh.annotations._
import rallyhealth.weepickle.v1.CardinalityVisitor

import java.util.concurrent.TimeUnit

/**
  * ==Quick Run==
  * bench / Jmh / run -f1 -wi 2 -i 3 .*ConcatFromInputsBench
  *
  * ==Profile with Flight Recorder==
  * bench / Jmh / run -prof jfr -f1 .*ConcatFromInputsBench
  *
  * ==Jmh Visualizer Report==
  * bench / Jmh / run -prof gc -rf json -rff ConcatFromInputsBench-results.json .*ConcatFromInputsBench
  *
  * ==Sample Results==
  * bench / Jmh / run -f3 -wi 3 -i 3 .*ConcatFromInputsBench
  * {{{
  * Benchmark                    Mode  Cnt   Score    Error  Units
  * ConcatFromInputsBench.arrs  thrpt    9  96.627 ±  6.636  ops/s
  * ConcatFromInputsBench.objs  thrpt    9  72.950 ± 13.792  ops/s
  * }}}
  *
  * @see https://github.com/ktoso/sbt-jmh
  */
@Warmup(iterations = 3, time = 5, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 3, time = 5, timeUnit = TimeUnit.SECONDS)
@State(Scope.Benchmark)
@BenchmarkMode(Array(Mode.Throughput))
@OutputTimeUnit(TimeUnit.SECONDS)
@Fork(jvmArgsAppend = Array("-Xmx350m", "-XX:+HeapDumpOnOutOfMemoryError", "-XX:-BackgroundCompilation", "-XX:-TieredCompilation"), value = 1)
class ConcatFromInputsBench {

  @Benchmark
  def objs: Int = ConcatFromInputs.from(manyObjs).get.transform(sink)

  @Benchmark
  def arrs: Int = ConcatFromInputs.from(manyArrs).get.transform(sink)

  private val manyObjs = {
    // not using FromScala(Obj(...)) to avoid profiler noise from singleObj.size(), etc.
    val singleObj = new FromInput {
      override def transform[T](
        to: Visitor[_, T]
      ): T = {
        val obj = to.visitObject(1).narrow
        obj.visitKeyValue(obj.visitKey().visitString("key"))
        obj.visitValue(obj.subVisitor.visitTrue())
        obj.visitEnd()
      }
    }
    Seq.fill(1000 * 1000)(singleObj)
  }

  private val manyArrs = {
    // not using FromScala(Arr(...)) to avoid profiler noise from singleArr.size(), etc.
    val singleArr = new FromInput {
      override def transform[T](
        to: Visitor[_, T]
      ): T = {
        val arr = to.visitArray(1).narrow
        arr.visitValue(arr.subVisitor.visitTrue())
        arr.visitEnd()
      }
    }
    Seq.fill(1000 * 1000)(singleArr)
  }

  private def sink = new CardinalityVisitor
}
