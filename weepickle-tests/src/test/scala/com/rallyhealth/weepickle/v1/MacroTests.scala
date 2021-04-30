package com.rallyhealth.weepickle.v1
import acyclic.file
import com.rallyhealth.weejson.v1.jackson.{FromJson, ToJson}
import utest._
import com.rallyhealth.weepickle.v1.TestUtil._
import com.rallyhealth.weepickle.v1.WeePickle.{FromScala, ToScala}
import com.rallyhealth.weepickle.v1.core.MutableCharSequenceVisitor
import com.rallyhealth.weepickle.v1.implicits.key

object Custom {
  trait ThingBase {
    val i: Int
    val s: String
    override def equals(o: Any) = {
      o.toString == this.toString
    }

    override def toString() = {
      s"Thing($i, $s)"
    }
  }

  class Thing2(val i: Int, val s: String) extends ThingBase

  abstract class ThingBaseCompanion[T <: ThingBase](f: (Int, String) => T) {
    implicit val thing2From = com.rallyhealth.weepickle.v1.WeePickle
      .fromTo[String]
      .bimap[T](
        t => t.i + " " + t.s,
        str => {
          val Array(i, s) = str.toString.split(" ", 2)
          f(i.toInt, s)
        }
      )
  }
  object Thing2 extends ThingBaseCompanion[Thing2](new Thing2(_, _))

  case class Thing3(i: Int, s: String) extends ThingBase

  object Thing3 extends ThingBaseCompanion[Thing3](new Thing3(_, _))
}

//// this can be un-sealed as long as `derivedSubclasses` is defined in the companion
sealed trait TypedFoo
object TypedFoo {
  import com.rallyhealth.weepickle.v1.WeePickle._
  implicit val readerFrom: FromTo[TypedFoo] = FromTo.merge(
    macroFromTo[Bar],
    macroFromTo[Baz],
    macroFromTo[Quz]
  )

  case class Bar(i: Int) extends TypedFoo
  case class Baz(s: String) extends TypedFoo
  case class Quz(b: Boolean) extends TypedFoo
}
// End TypedFoo

sealed trait Pony
@key("twi") case class Twilight() extends Pony
object Twilight {
  implicit val fromTwi = WeePickle.macroFrom[Twilight] // supporting comap requires breaking changes.
  implicit val toTwi = WeePickle.macroTo[Twilight].map(identity)
}
object Pony {
  implicit val pickler = WeePickle.macroFromTo[Pony]
}

object MacroTests extends TestSuite {

  // Doesn't work :(
//  case class A_(objects: Option[C_]); case class C_(nodes: Option[C_])

//  implicitly[To[A_]]
//  implicitly[com.rallyhealth.weepickle.v1.old.From[com.rallyhealth.weepickle.v1.MixedIn.Obj.ClsB]]
//  println(write(ADTs.ADTc(1, "lol", (1.1, 1.2))))
//  implicitly[com.rallyhealth.weepickle.v1.old.From[ADTs.ADTc]]

  val tests = Tests {
    test("mixedIn") {
      import MixedIn._
      test - rw(Obj.ClsB(1), """{"i":1}""")
      test - rw(Obj.ClsA("omg"), """{"s":"omg"}""")
    }
//
//    /*
//    // TODO Currently not supported
//    test("declarationWithinFunction"){
//      sealed trait Base
//      case object Child extends Base
//      case class Wrapper(base: Base)
//      test - com.rallyhealth.weepickle.v1.write(Wrapper(Child))
//    }
//

//    */
    test("exponential") {

      // Doesn't even need to execute, as long as it can compile
      val ww1 = implicitly[com.rallyhealth.weepickle.v1.WeePickle.From[Exponential.A1]]
    }

    test("commonCustomStructures") {
      test("simpleAdt") {

        test - rw(ADTs.ADT0(), """{}""")
        test - rw(ADTs.ADTa(1), """{"i":1}""")
        test - rw(ADTs.ADTb(1, "lol"), """{"i":1,"s":"lol"}""")

        test - rw(ADTs.ADTc(1, "lol", (1.1, 1.2)), """{"i":1,"s":"lol","t":[1.1,1.2]}""")
        test - rw(
          ADTs.ADTd(1, "lol", (1.1, 1.2), ADTs.ADTa(1)),
          """{"i":1,"s":"lol","t":[1.1,1.2],"a":{"i":1}}"""
        )

        test - rw(
          ADTs.ADTe(1, "lol", (1.1, 1.2), ADTs.ADTa(1), List(1.2, 2.1, 3.14)),
          """{"i":1,"s":"lol","t":[1.1,1.2],"a":{"i":1},"q":[1.2,2.1,3.14]}"""
        )

        // This use case is not currently supported in the rallyhealth/weepickle fork.
        // We'd like for the idiomatic scala Option[String] to map to
        // the idiomatic OpenAPI schema for a non-required string.
        // I can't think of a use case where we'd ever use Option[Option[T]],
        // so we're defaulting to the use case we have at the expense of not being able
        // to roundtrip over this use case we don't have.
        // In the future, maybe we can special case the handling of Option[Option[T]].
        //
        // test - rw(
        //   ADTs.ADTf(1, "lol", (1.1, 1.2), ADTs.ADTa(1), List(1.2, 2.1, 3.14), Some(None)),
        //   """{"i":1,"s":"lol","t":[1.1,1.2],"a":{"i":1},"q":[1.2,2.1,3.14],"o":[[]]}"""
        // )
        val chunks = for (i <- 1 to 18) yield {
          val rhs = if (i % 2 == 1) "1" else "\"1\""
          val lhs = '"' + s"t$i" + '"'
          s"$lhs:$rhs"
        }

        val expected = s"""{${chunks.mkString(",")}}"""
        test - rw(
          ADTs.ADTz(1, "1", 1, "1", 1, "1", 1, "1", 1, "1", 1, "1", 1, "1", 1, "1", 1, "1"),
          expected
        )
      }

      test("sealedHierarchy") {
        // objects in sealed case class hierarchies should always read and write
        // the same way (with a tag) regardless of what their static type is when
        // written. This is feasible because sealed hierarchies can only have a
        // finite number of cases, so we can just check them all and decide which
        // class the instance belongs to.
        import Hierarchy._
        test("shallow") {
          test - rw(B(1), """{"$type": "com.rallyhealth.weepickle.v1.Hierarchy.B", "i":1}""")
          test - rw(C("a", "b"), """{"$type": "com.rallyhealth.weepickle.v1.Hierarchy.C", "s1":"a","s2":"b"}""")
          test - rw(AnZ: Z, """{"$type": "com.rallyhealth.weepickle.v1.Hierarchy.AnZ"}""")
          test - rw(AnZ, """{"$type": "com.rallyhealth.weepickle.v1.Hierarchy.AnZ"}""")
          test - rw(Hierarchy.B(1): Hierarchy.A, """{"$type": "com.rallyhealth.weepickle.v1.Hierarchy.B", "i":1}""")
          test - rw(C("a", "b"): A, """{"$type": "com.rallyhealth.weepickle.v1.Hierarchy.C", "s1":"a","s2":"b"}""")

        }
        test("tagLast") {
          // Make sure the tagged dictionary parser is able to parse cases where
          // the $type-tag appears later in the dict. It does this by a totally
          // different code-path than for tag-first dicts, using an intermediate
          // AST, so make sure that code path works too.
          test - rw(C("a", "b"), """{"s1":"a","s2":"b", "$type": "com.rallyhealth.weepickle.v1.Hierarchy.C"}""")
          test("mutable") {
            // Make sure that the buffering done by the macro captures immutable values.
            val r = new WeePickle.To.Delegate(new MutableCharSequenceVisitor(WeePickle.to[C]))
            val w = WeePickle.from[C]
            rw(C("a", "b"), """{"s1":"a","s2":"b", "$type": "com.rallyhealth.weepickle.v1.Hierarchy.C"}""")(r, w)
          }

          test - rw(B(1), """{"i":1, "$type": "com.rallyhealth.weepickle.v1.Hierarchy.B"}""")
          test - rw(C("a", "b"): A, """{"s1":"a","s2":"b", "$type": "com.rallyhealth.weepickle.v1.Hierarchy.C"}""")
        }
        test("deep") {
          import DeepHierarchy._

          test - rw(B(1), """{"$type": "com.rallyhealth.weepickle.v1.DeepHierarchy.B", "i":1}""")
          test - rw(B(1): A, """{"$type": "com.rallyhealth.weepickle.v1.DeepHierarchy.B", "i":1}""")
          test - rw(AnQ(1): Q, """{"$type": "com.rallyhealth.weepickle.v1.DeepHierarchy.AnQ", "i":1}""")
          test - rw(AnQ(1), """{"$type": "com.rallyhealth.weepickle.v1.DeepHierarchy.AnQ","i":1}""")

          test - rw(
            F(AnQ(1)),
            """{"$type": "com.rallyhealth.weepickle.v1.DeepHierarchy.F","q":{"$type":"com.rallyhealth.weepickle.v1.DeepHierarchy.AnQ", "i":1}}"""
          )
          test - rw(
            F(AnQ(2)): A,
            """{"$type": "com.rallyhealth.weepickle.v1.DeepHierarchy.F","q":{"$type":"com.rallyhealth.weepickle.v1.DeepHierarchy.AnQ", "i":2}}"""
          )
          test - rw(
            F(AnQ(3)): C,
            """{"$type": "com.rallyhealth.weepickle.v1.DeepHierarchy.F","q":{"$type":"com.rallyhealth.weepickle.v1.DeepHierarchy.AnQ", "i":3}}"""
          )
          test - rw(D("1"), """{"$type": "com.rallyhealth.weepickle.v1.DeepHierarchy.D", "s":"1"}""")
          test - rw(D("1"): C, """{"$type": "com.rallyhealth.weepickle.v1.DeepHierarchy.D", "s":"1"}""")
          test - rw(D("1"): A, """{"$type": "com.rallyhealth.weepickle.v1.DeepHierarchy.D", "s":"1"}""")
          test - rw(E(true), """{"$type": "com.rallyhealth.weepickle.v1.DeepHierarchy.E", "b":true}""")
          test - rw(E(true): C, """{"$type": "com.rallyhealth.weepickle.v1.DeepHierarchy.E","b":true}""")
          test - rw(E(true): A, """{"$type": "com.rallyhealth.weepickle.v1.DeepHierarchy.E", "b":true}""")
        }
      }
      test("singleton") {
        import Singletons._

        rw(BB, """{"$type":"com.rallyhealth.weepickle.v1.Singletons.BB"}""")
        rw(CC, """{"$type":"com.rallyhealth.weepickle.v1.Singletons.CC"}""")
        rw(BB: AA, """{"$type":"com.rallyhealth.weepickle.v1.Singletons.BB"}""")
        rw(CC: AA, """{"$type":"com.rallyhealth.weepickle.v1.Singletons.CC"}""")
      }
    }
    test("robustnessAgainstVaryingSchemas") {
      test("renameKeysViaAnnotations") {
        import Annotated._

        test - rw(B(1), """{"$type": "0", "omg":1}""")
        test - rw(C("a", "b"), """{"$type": "1", "lol":"a","wtf":"b"}""")

        test - rw(B(1): A, """{"$type": "0", "omg":1}""")
        test - rw(C("a", "b"): A, """{"$type": "1", "lol":"a","wtf":"b"}""")
      }
      test("useDefaults") {
        // Ignore the values which match the default when writing and
        // substitute in defaults when reading if the key is missing
        import Defaults._
        test - rw(ADTa(), "{}")
        test - rw(ADTa(321), """{"i":321}""")
        test - rw(ADTb(s = "123"), """{"s":"123"}""")
        test - rw(ADTb(i = 234, s = "567"), """{"i":234,"s":"567"}""")
        test - rw(ADTc(s = "123"), """{"s":"123"}""")
        test - rw(ADTc(i = 234, s = "567"), """{"i":234,"s":"567"}""")
        test - rw(ADTc(t = (12.3, 45.6), s = "789"), """{"s":"789","t":[12.3,45.6]}""")
        test - rw(ADTc(t = (12.3, 45.6), s = "789", i = 31337), """{"i":31337,"s":"789","t":[12.3,45.6]}""")
      }
      test("ignoreExtraFieldsWhenDeserializing") {
        import ADTs._
        val r1 = FromJson("""{"i":123, "j":false, "k":"haha"}""").transform(ToScala[ADTa])
        assert(r1 == ADTa(123))
        val r2 =
          FromJson("""{"i":123, "j":false, "k":"haha", "s":"kk", "l":true, "z":[1, 2, 3]}""").transform(ToScala[ADTb])
        assert(r2 == ADTb(123, "kk"))
      }
    }

    test("custom") {
      test("clsFromTo") {
        rw(new Custom.Thing2(1, "s"), """ "1 s" """)
        rw(new Custom.Thing2(10, "sss"), """ "10 sss" """)
      }
      test("caseClsFromTo") {
        rw(new Custom.Thing3(1, "s"), """ "1 s" """)
        rw(new Custom.Thing3(10, "sss"), """ "10 sss" """)
      }
    }
    test("varargs") {
      rw(Varargs.Sentence("a", "b", "c"), """{"a":"a","bs":["b","c"]}""")
      rw(Varargs.Sentence("a"), """{"a":"a","bs":[]}""")
    }

    // format: off
    test("lots of fields"){
      val b63 = Big63(
        0, 1, 2, 3, 4, 5, 6, 7,
        8, 9, 10, 11, 12, 13, 14,
        15, 16, 17, 18, 19, 20, 21,
        22, 23, 24, 25, 26, 27, 28,
        29, 30, 31, 32, 33, 34, 35,
        36, 37, 38, 39, 40, 41, 42,
        43, 44, 45, 46, 47, 48, 49,
        50, 51, 52, 53, 54, 55, 56,
        57, 58, 59, 60, 61, 62
      )
      val b64 = Big64(
        0, 1, 2, 3, 4, 5, 6, 7,
        8, 9, 10, 11, 12, 13, 14,
        15, 16, 17, 18, 19, 20, 21,
        22, 23, 24, 25, 26, 27, 28,
        29, 30, 31, 32, 33, 34, 35,
        36, 37, 38, 39, 40, 41, 42,
        43, 44, 45, 46, 47, 48, 49,
        50, 51, 52, 53, 54, 55, 56,
        57, 58, 59, 60, 61, 62, 63
      )
      val b65 = Big65(
        0, 1, 2, 3, 4, 5, 6, 7,
        8, 9, 10, 11, 12, 13, 14,
        15, 16, 17, 18, 19, 20, 21,
        22, 23, 24, 25, 26, 27, 28,
        29, 30, 31, 32, 33, 34, 35,
        36, 37, 38, 39, 40, 41, 42,
        43, 44, 45, 46, 47, 48, 49,
        50, 51, 52, 53, 54, 55, 56,
        57, 58, 59, 60, 61, 62, 63,
        64
      )
      // format: on
      implicit val b63rw: com.rallyhealth.weepickle.v1.WeePickle.FromTo[Big63] =
        com.rallyhealth.weepickle.v1.WeePickle.macroFromTo
      implicit val b64rw: com.rallyhealth.weepickle.v1.WeePickle.FromTo[Big64] =
        com.rallyhealth.weepickle.v1.WeePickle.macroFromTo
      implicit val b65rw: com.rallyhealth.weepickle.v1.WeePickle.FromTo[Big65] =
        com.rallyhealth.weepickle.v1.WeePickle.macroFromTo
      val written63 = FromScala(b63).transform(ToJson.string)
      assert(FromJson(written63).transform(ToScala[Big63]) == b63)
      val written64 = FromScala(b64).transform(ToJson.string)
      assert(FromJson(written64).transform(ToScala[Big64]) == b64)
      val written65 = FromScala(b65).transform(ToJson.string)
      assert(FromJson(written65).transform(ToScala[Big65]) == b65)
    }

    test("map") {
      rw[Pony](Twilight(), """{"$type": "twi"}""")
    }
  }
}


// format: off
case class Big63(_0: Byte, _1: Byte, _2: Byte, _3: Byte, _4: Byte, _5: Byte, _6: Byte, _7: Byte,
  _8: Byte, _9: Byte, _10: Byte, _11: Byte, _12: Byte, _13: Byte, _14: Byte,
  _15: Byte, _16: Byte, _17: Byte, _18: Byte, _19: Byte, _20: Byte, _21: Byte,
  _22: Byte, _23: Byte, _24: Byte, _25: Byte, _26: Byte, _27: Byte, _28: Byte,
  _29: Byte, _30: Byte, _31: Byte, _32: Byte, _33: Byte, _34: Byte, _35: Byte,
  _36: Byte, _37: Byte, _38: Byte, _39: Byte, _40: Byte, _41: Byte, _42: Byte,
  _43: Byte, _44: Byte, _45: Byte, _46: Byte, _47: Byte, _48: Byte, _49: Byte,
  _50: Byte, _51: Byte, _52: Byte, _53: Byte, _54: Byte, _55: Byte, _56: Byte,
  _57: Byte, _58: Byte, _59: Byte, _60: Byte, _61: Byte, _62: Byte)
case class Big64(_0: Byte, _1: Byte, _2: Byte, _3: Byte, _4: Byte, _5: Byte, _6: Byte, _7: Byte,
  _8: Byte, _9: Byte, _10: Byte, _11: Byte, _12: Byte, _13: Byte, _14: Byte,
  _15: Byte, _16: Byte, _17: Byte, _18: Byte, _19: Byte, _20: Byte, _21: Byte,
  _22: Byte, _23: Byte, _24: Byte, _25: Byte, _26: Byte, _27: Byte, _28: Byte,
  _29: Byte, _30: Byte, _31: Byte, _32: Byte, _33: Byte, _34: Byte, _35: Byte,
  _36: Byte, _37: Byte, _38: Byte, _39: Byte, _40: Byte, _41: Byte, _42: Byte,
  _43: Byte, _44: Byte, _45: Byte, _46: Byte, _47: Byte, _48: Byte, _49: Byte,
  _50: Byte, _51: Byte, _52: Byte, _53: Byte, _54: Byte, _55: Byte, _56: Byte,
  _57: Byte, _58: Byte, _59: Byte, _60: Byte, _61: Byte, _62: Byte, _63: Byte)
case class Big65(_0: Byte, _1: Byte, _2: Byte, _3: Byte, _4: Byte, _5: Byte, _6: Byte, _7: Byte,
  _8: Byte, _9: Byte, _10: Byte, _11: Byte, _12: Byte, _13: Byte, _14: Byte,
  _15: Byte, _16: Byte, _17: Byte, _18: Byte, _19: Byte, _20: Byte, _21: Byte,
  _22: Byte, _23: Byte, _24: Byte, _25: Byte, _26: Byte, _27: Byte, _28: Byte,
  _29: Byte, _30: Byte, _31: Byte, _32: Byte, _33: Byte, _34: Byte, _35: Byte,
  _36: Byte, _37: Byte, _38: Byte, _39: Byte, _40: Byte, _41: Byte, _42: Byte,
  _43: Byte, _44: Byte, _45: Byte, _46: Byte, _47: Byte, _48: Byte, _49: Byte,
  _50: Byte, _51: Byte, _52: Byte, _53: Byte, _54: Byte, _55: Byte, _56: Byte,
  _57: Byte, _58: Byte, _59: Byte, _60: Byte, _61: Byte, _62: Byte, _63: Byte,
  _64: Byte)
// format: on
