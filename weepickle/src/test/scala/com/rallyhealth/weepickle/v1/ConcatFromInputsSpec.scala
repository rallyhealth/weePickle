package com.rallyhealth.weepickle.v1

import com.rallyhealth.weejson.v1.jackson.ToJson
import com.rallyhealth.weejson.v1.{Arr, Obj, Value}
import com.rallyhealth.weepickle.v1.laws.LawsVisitor
import com.rallyhealth.weepickle.v1.WeePickle.FromScala
import com.rallyhealth.weepickle.v1.core.Abort
import com.rallyhealth.weepickle.v1.core.ops.FromInputOpsImplicits
import org.scalactic.TypeCheckedTripleEquals
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

class ConcatFromInputsSpec
  extends AnyFreeSpec
  with Matchers
  with TypeCheckedTripleEquals
  with FromInputOpsImplicits {

  "obj" - {
    "empty" in (concat(Obj()) shouldBe """{}""")
    "a" in (concat(Obj("a" -> 1)) shouldBe """{"a":1}""")
    "ab" in (concat(Obj("a" -> 1), Obj("b" -> 2)) shouldBe """{"a":1,"b":2}""")
    "aa" in (concat(Obj("a" -> 1), Obj("a" -> 2)) shouldBe """{"a":1,"a":2}""")
    "abc" in (concat(Obj("a" -> 1), Obj("b" -> 2), Obj("c" -> 3)) shouldBe """{"a":1,"b":2,"c":3}""")
    "no StackOverflowError" in {
      val obj = Obj("a" -> 1)
      (noException should be thrownBy (concat(
        IndexedSeq.fill(1000 * 1000)(obj)
      )))
    }
  }

  "arr" - {
    "a" in (concat(Arr(1)) shouldBe """[1]""")
    "ab" in (concat(Arr(1), Arr(2)) shouldBe """[1,2]""")
    "abc" in (concat(Arr(1, 2, 3)) shouldBe """[1,2,3]""")
    "no StackOverflowError" in {
      val arr = Arr(1)
      (noException should be thrownBy (concat(
        IndexedSeq.fill(1000 * 1000)(arr)
      )))
    }
  }

  "invalid" - {
    "arr obj" in (a[Abort] shouldBe thrownBy(
      concat(Arr(1), Obj())
    ))
    "arr int" in (a[Abort] shouldBe thrownBy(concat(Arr(1), 2)))
    "obj int" in (a[Abort] shouldBe thrownBy(concat(Obj("a" -> 1), 2)))
  }

  private def concat(
    objs: Value*
  ): String = {
    objs
      .map(FromScala(_))
      .reduce(_ ++ _)
      .transform(new LawsVisitor(ToJson.string))
  }
}
