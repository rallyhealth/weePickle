package com.rallyhealth.weepickle.v1

import com.rallyhealth.weejson.v1.jackson.{FromJson, ToJson}
import com.rallyhealth.weepickle.v1.WeePickle.{FromScala, ToScala}
import com.rallyhealth.weepickle.v1.example.Suit
import utest._

object EnumTests extends TestSuite {

  def tests = Tests {

    test("name") {
      test("happy path") - repeatedly {
        FromScala(Suit.Spades).transform(ToJson.string) ==> """"Spades""""
        FromJson(""""Spades"""").transform(ToScala[Suit.Value]) ==> Suit.Spades
      }

      test("miss") {
        test("wrong type") - repeatedly {
          intercept[Exception] {
            FromJson("1").transform(ToScala[Suit.Value])
          }
        }

        test("invalid string") - repeatedly {
          intercept[Exception] {
            FromJson(""""does not exist"""").transform(ToScala[Suit.Value])
          }
        }
      }

      test("side effecting Enumeration") - {
        object Horrible extends Enumeration {

          def addEntry(s: String): Horrible.Value = Value(s) // never do this!

          implicit val pickler = WeePickle.fromToEnumerationName(this)

          val One = Value("One")
        }

        Horrible.pickler.visitString("One") ==> Horrible.One

        intercept[Exception](Horrible.pickler.visitString("Two"))

        val Two = Horrible.addEntry("Two")
        Horrible.pickler.visitString("Two") ==> Two
      }

      test("nameless") - {
        object Nameless extends Enumeration {

          val One = Value
          val Two = Value

          implicit val pickler = WeePickle.fromToEnumerationName(this)
        }

        Nameless.pickler.visitString("One") ==> Nameless.One
        Nameless.pickler.visitString("Two") ==> Nameless.Two
        intercept[Exception](Nameless.pickler.visitString("Three"))
      }
    }

    test("id") - {
      object SpecialNums extends Enumeration {

        val Min = Value(Int.MinValue)
        val Zero = Value(0)
        val Max = Value(Int.MaxValue)

        def addEntry(s: Int): Value = Value(s) // never do this!

        implicit val pickler = WeePickle.fromToEnumerationId(this)
      }

      SpecialNums.pickler.visitInt32(Int.MinValue) ==> SpecialNums.Min
      SpecialNums.pickler.visitInt32(Int.MaxValue) ==> SpecialNums.Max
      SpecialNums.pickler.visitInt32(0) ==> SpecialNums.Zero
      SpecialNums.pickler.visitInt64(0L) ==> SpecialNums.Zero

      intercept[Exception](SpecialNums.pickler.visitInt32(42))
      val FortyTwo = SpecialNums.addEntry(42)
      SpecialNums.pickler.visitInt32(42) ==> FortyTwo
    }
  }

  /**
    * Test idempotence by running a few times.
    */
  private def repeatedly(f: => Unit): Unit = for (_ <- 1 to 3) f
}
