package com.rallyhealth.weepickle.v1

import com.rallyhealth.weejson.v1.jackson.{FromJson, ToJson}
import com.rallyhealth.weepickle.v1.core.Abort
import com.rallyhealth.weepickle.v1.WeePickle.*
import utest.*

object EnumV3Tests extends TestSuite {

  enum Fruit derives FromTo {
    case Peach, Pear, Strawberry
  }

  enum Suit derives FromTo {
    case Spades, Hearts, Diamonds, Clubs
  }

  case class Card(rank: Int, suit: Suit) derives FromTo

  enum SuitNum {
    case Spades, Hearts, Diamonds, Clubs
  }
  object SuitNum extends FromToById[SuitNum]

  enum Charm(val color: String, val introduced: Int = 1964) derives FromTo {
    case Heart     extends Charm("pink")
    case Moon      extends Charm("yellow")
    case Star      extends Charm("orange")
    case Clover    extends Charm("green")
    case Diamond   extends Charm("blue", 1975)
    case Horseshoe extends Charm("purple", 1983)
    case Balloon   extends Charm("red", 1989)
    case Tree      extends Charm("green", 1991)
  }

  def tests = Tests {
    test("name") {
      test("happy path - Fruits (raw enums)") - repeatedly {
        FromJson(""""Peach"""").transform(ToScala[Fruit]) ==> Fruit.Peach
        FromScala(Fruit.Peach).transform(ToJson.string) ==> """"Peach""""
        FromScala(Fruit.values.head).transform(ToJson.string) ==> """"Peach""""
        FromJson(""""Pear"""").transform(ToScala[Fruit]) ==> Fruit.Pear
      }

      test("happy path - Cards (enums in case classes)") - repeatedly {
        FromScala(Card(3, Suit.Spades)).transform(ToJson.string) ==> """{"rank":3,"suit":"Spades"}"""
        FromJson("""{"rank":3,"suit": "Spades"}""").transform(ToScala[Card]) ==> Card(3, Suit.Spades)
      }

      test("happy path - Suits (enums with numeric encoding)") - repeatedly {
        FromScala(SuitNum.Spades).transform(ToJson.string) ==> """0"""
        FromJson("""1""").transform(ToScala[SuitNum]) ==> SuitNum.Hearts
      }

      test("happy path - Breakfast (enums with attributes)") - repeatedly {
        FromScala(Charm.values.toSeq.filter(_.introduced == 1964)).transform(ToJson.string) ==> """["Heart","Moon","Star","Clover"]"""
        FromScala(Charm.values.toSeq.filter(_.introduced > 1964)).transform(ToJson.string) ==> """["Diamond","Horseshoe","Balloon","Tree"]"""

        FromJson("""["Heart","Moon","Star","Clover"]""").transform(ToScala[Seq[Charm]]) ==> Charm.values.toSeq.filter(_.introduced == 1964)
        FromJson("""["Diamond","Horseshoe","Balloon","Tree"]""").transform(ToScala[Seq[Charm]]) ==> Charm.values.toSeq.filter(_.introduced > 1964)
      }

      test("override - use custom encoding") - {
        implicit val pickler: FromTo[Suit] = fromTo[Int].bimap[Suit](
          _.ordinal,
          Suit.fromOrdinal
        )

        FromScala(Suit.Spades).transform(ToJson.string) ==> "0"
        FromJson("0").transform(ToScala[Suit]) ==> Suit.Spades
      }

      test("miss") {
        test("wrong type") - repeatedly {
          intercept[Exception] {
            FromJson("1").transform(ToScala[Suit])
          }
        }

        test("invalid string") - repeatedly {
          intercept[Exception] {
            FromJson(""""does not exist"""").transform(ToScala[Suit])
          }
        }
      }
    }

    test("id") - {
      enum SpecialNums(val id: Int) {
        case Min extends SpecialNums(Int.MinValue)
        case Zero extends SpecialNums(0)
        case Max extends SpecialNums(Int.MaxValue)
      }
      object SpecialNums {
        implicit val pickler: FromTo[SpecialNums] = fromTo[Int].bimap[SpecialNums](
          _.id,
          i => SpecialNums.values.find(_.id == i).getOrElse(throw new Abort(s"not special: $i"))
        )
      }

      SpecialNums.pickler.visitInt32(Int.MinValue) ==> SpecialNums.Min
      SpecialNums.pickler.visitInt32(Int.MaxValue) ==> SpecialNums.Max
      SpecialNums.pickler.visitInt32(0) ==> SpecialNums.Zero
      SpecialNums.pickler.visitInt64(0L) ==> SpecialNums.Zero

      intercept[Exception](SpecialNums.pickler.visitInt32(42))
    }
  }

  /**
    * Test idempotence by running a few times.
    */
  private def repeatedly(f: => Unit): Unit = for (_ <- 1 to 3) f
}
