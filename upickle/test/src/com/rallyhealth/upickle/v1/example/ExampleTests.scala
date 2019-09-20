package com.rallyhealth.upickle.v1.example

import java.io.StringWriter

import acyclic.file
import com.rallyhealth.upickle.v1.{TestUtil, default}
import utest._
import com.rallyhealth.upickle.v1.default.{macroRW, ReadWriter => RW}
import com.rallyhealth.ujson.v1.{IncompleteParseException, ParseException, Readable}
import com.rallyhealth.ujson.v1.{BytesRenderer, Value, StringRenderer}
import com.rallyhealth.upickle.v1.core.{NoOpVisitor, Visitor}
object Simple {
  case class Thing(myFieldA: Int, myFieldB: String)
  object Thing{
    implicit val rw: RW[Thing] = macroRW
  }
  case class Big(i: Int, b: Boolean, str: String, c: Char, t: Thing)
  object Big{
    implicit val rw: RW[Big] = macroRW
  }
}
object Sealed{
  sealed trait IntOrTuple
  object IntOrTuple{
    implicit val rw: RW[IntOrTuple] = RW.merge(IntThing.rw, TupleThing.rw)
  }
  case class IntThing(i: Int) extends IntOrTuple
  object IntThing{
    implicit val rw: RW[IntThing] = macroRW
  }
  case class TupleThing(name: String, t: (Int, Int)) extends IntOrTuple
  object TupleThing{
    implicit val rw: RW[TupleThing] = macroRW
  }
}
object Recursive{
  case class Foo(i: Int)
  object Foo{
    implicit val rw: RW[Foo] = macroRW
  }
  case class Bar(name: String, foos: Seq[Foo])
  object Bar{
    implicit val rw: RW[Bar] = macroRW
  }
}
object Defaults{
  case class FooDefault(i: Int = 10, s: String = "lol")
  object FooDefault{
    implicit val rw: RW[FooDefault] = macroRW
  }
}
object Keyed{
  case class KeyBar(@com.rallyhealth.upickle.v1.implicits.key("hehehe") kekeke: Int)
  object KeyBar{
    implicit val rw: RW[KeyBar] = macroRW
  }
}
object KeyedTag{
  sealed trait A
  object A{
    implicit val rw: RW[A] = RW.merge(B.rw, macroRW[C.type])
  }
  @com.rallyhealth.upickle.v1.implicits.key("Bee") case class B(i: Int) extends A
  object B{
    implicit val rw: RW[B] = macroRW
  }
  case object C extends A
}
object Custom2{
  class CustomThing2(val i: Int, val s: String)
  object CustomThing2 {
    implicit val rw = com.rallyhealth.upickle.v1.default.readwriter[String].bimap[CustomThing2](
      x => x.i + " " + x.s,
      str => {
        val Array(i, s) = str.split(" ", 2)
        new CustomThing2(i.toInt, s)
      }
    )
  }
}

import KeyedTag._
import Keyed._
import Sealed._
import Simple._
import Recursive._
import Defaults._

object ExampleTests extends TestSuite {

  import TestUtil._
  val tests = Tests {
    test("simple"){
      import com.rallyhealth.upickle.v1.default._

      write(1)                          ==> "1"

      write(Seq(1, 2, 3))               ==> "[1,2,3]"

      read[Seq[Int]]("[1,2,3]")         ==> List(1, 2, 3)

      write((1, "omg", true))           ==> """[1,"omg",true]"""

      read[(Int, String, Boolean)]("""[1,"omg",true]""") ==> (1, "omg", true)
    }
    test("binary"){
      import com.rallyhealth.upickle.v1.default._

      writeBinary(1)                          ==> Array(1)

      writeBinary(Seq(1, 2, 3))               ==> Array(0x93.toByte, 1, 2, 3)

      readBinary[Seq[Int]](Array[Byte](0x93.toByte, 1, 2, 3))  ==> List(1, 2, 3)

      val serializedTuple = Array[Byte](0x93.toByte, 1, 0xa3.toByte, 111, 109, 103, 0xc3.toByte)

      writeBinary((1, "omg", true))           ==> serializedTuple

      readBinary[(Int, String, Boolean)](serializedTuple) ==> (1, "omg", true)
    }
    test("more"){
      import com.rallyhealth.upickle.v1.default._
      test("booleans"){
        write(true: Boolean)              ==> "true"
        write(false: Boolean)             ==> "false"
      }
      test("numbers"){
        write(12: Int)                    ==> "12"
        write(12: Short)                  ==> "12"
        write(12: Byte)                   ==> "12"
        write(Int.MaxValue)               ==> "2147483647"
        write(Int.MinValue)               ==> "-2147483648"
        write(12.5f: Float)               ==> "12.5"
        write(12.5: Double)               ==> "12.5"
      }
      test("longs"){
        write(12: Long)                   ==> "12"
        write(4000000000000L: Long)       ==> "4000000000000"
        // large longs are written as strings, to avoid floating point rounding
        write(9223372036854775807L: Long) ==> "\"9223372036854775807\""
      }
      test("specialNumbers"){
        write(1.0/0: Double)              ==> "\"Infinity\""
        write(Float.PositiveInfinity)     ==> "\"Infinity\""
        write(Float.NegativeInfinity)     ==> "\"-Infinity\""
      }
      test("charStrings"){
        write('o')                        ==> "\"o\""
        write("omg")                      ==> "\"omg\""
      }
      test("seqs"){
        write(Array(1, 2, 3))             ==> "[1,2,3]"

        // You can pass in an `indent` parameter to format it nicely
        write(Array(1, 2, 3), indent = 4)  ==>
          """[
            |    1,
            |    2,
            |    3
            |]""".stripMargin

        write(Seq(1, 2, 3))               ==> "[1,2,3]"
        write(Vector(1, 2, 3))            ==> "[1,2,3]"
        write(List(1, 2, 3))              ==> "[1,2,3]"
        import collection.immutable.SortedSet
        write(SortedSet(1, 2, 3))         ==> "[1,2,3]"
      }
      test("options"){
        write(Some(1))                    ==> "[1]"
        write(None)                       ==> "[]"
      }
      test("tuples"){
        write((1, "omg"))                 ==> """[1,"omg"]"""
        write((1, "omg", true))           ==> """[1,"omg",true]"""
      }

      test("caseClass"){
        import com.rallyhealth.upickle.v1._
        write(Thing(1, "gg"))             ==> """{"myFieldA":1,"myFieldB":"gg"}"""
        read[Thing]("""{"myFieldA":1,"myFieldB":"gg"}""") ==> Thing(1, "gg")
        write(Big(1, true, "lol", 'Z', Thing(7, ""))) ==>
          """{"i":1,"b":true,"str":"lol","c":"Z","t":{"myFieldA":7,"myFieldB":""}}"""

        write(Big(1, true, "lol", 'Z', Thing(7, "")), indent = 4) ==>
          """{
            |    "i": 1,
            |    "b": true,
            |    "str": "lol",
            |    "c": "Z",
            |    "t": {
            |        "myFieldA": 7,
            |        "myFieldB": ""
            |    }
            |}""".stripMargin
        }


      test("sealed"){
        write(IntThing(1)) ==> """{"$type":"com.rallyhealth.upickle.v1.example.Sealed.IntThing","i":1}"""

        write(TupleThing("naeem", (1, 2))) ==>
          """{"$type":"com.rallyhealth.upickle.v1.example.Sealed.TupleThing","name":"naeem","t":[1,2]}"""

        // You can read tagged value without knowing its
        // type in advance, just use type of the sealed trait
        read[IntOrTuple]("""{"$type":"com.rallyhealth.upickle.v1.example.Sealed.IntThing","i":1}""") ==> IntThing(1)

      }
      test("recursive"){
        write((((1, 2), (3, 4)), ((5, 6), (7, 8)))) ==>
          """[[[1,2],[3,4]],[[5,6],[7,8]]]"""

        write(Seq(Thing(1, "g"), Thing(2, "k"))) ==>
          """[{"myFieldA":1,"myFieldB":"g"},{"myFieldA":2,"myFieldB":"k"}]"""

        write(Bar("bearrr", Seq(Foo(1), Foo(2), Foo(3)))) ==>
          """{"name":"bearrr","foos":[{"i":1},{"i":2},{"i":3}]}"""

      }
      test("null"){
        write(Bar(null, Seq(Foo(1), null, Foo(3)))) ==>
          """{"name":null,"foos":[{"i":1},null,{"i":3}]}"""
      }
    }
    test("defaults"){
      import com.rallyhealth.upickle.v1.default._
      test("reading"){
        read[FooDefault]("{}")                ==> FooDefault(10, "lol")
        read[FooDefault]("""{"i": 123}""")    ==> FooDefault(123,"lol")
      }
      test("writing"){
        write(FooDefault(i = 11, s = "lol"))  ==> """{"i":11}"""
        write(FooDefault(i = 10, s = "lol"))  ==> """{}"""
        write(FooDefault())                   ==> """{}"""
      }
    }

    test("sources"){
      import com.rallyhealth.upickle.v1.default._
      val original = """{"myFieldA":1,"myFieldB":"gg"}"""
      read[Thing](original) ==> Thing(1, "gg")
      read[Thing](original: CharSequence) ==> Thing(1, "gg")
      read[Thing](original.getBytes) ==> Thing(1, "gg")
    }
    test("mapped"){
      test("simple"){
        import com.rallyhealth.upickle.v1.default._
        case class Wrap(i: Int)
        implicit val fooReadWrite: ReadWriter[Wrap] =
          readwriter[Int].bimap[Wrap](_.i, Wrap(_))

        write(Seq(Wrap(1), Wrap(10), Wrap(100))) ==> "[1,10,100]"
        read[Seq[Wrap]]("[1,10,100]") ==> Seq(Wrap(1), Wrap(10), Wrap(100))
      }
      test("Value"){
        import com.rallyhealth.upickle.v1.default._
        case class Bar(i: Int, s: String)
        implicit val fooReadWrite: ReadWriter[Bar] =
          readwriter[com.rallyhealth.ujson.v1.Value].bimap[Bar](
            x => com.rallyhealth.ujson.v1.Arr(x.s, x.i),
            json => new Bar(json(1).num.toInt, json(0).str)
          )

        write(Bar(123, "abc")) ==> """["abc",123]"""
        read[Bar]("""["abc",123]""") ==> Bar(123, "abc")
      }
    }
    test("keyed"){
      import com.rallyhealth.upickle.v1.default._
      test("attrs"){
        write(KeyBar(10))                     ==> """{"hehehe":10}"""
        read[KeyBar]("""{"hehehe": 10}""")    ==> KeyBar(10)
      }
      test("tag"){
        write(B(10))                          ==> """{"$type":"Bee","i":10}"""
        read[B]("""{"$type":"Bee","i":10}""") ==> B(10)
      }
      test("snakeCase"){
        object SnakePickle extends com.rallyhealth.upickle.v1.AttributeTagged{
          def camelToSnake(s: String) = {
            s.split("(?=[A-Z])", -1).map(_.toLowerCase).mkString("_")
          }
          def snakeToCamel(s: String) = {
            val res = s.split("_", -1).map(x => x(0).toUpper + x.drop(1)).mkString
            s(0).toLower + res.drop(1)
          }

          override def objectAttributeKeyReadMap(s: CharSequence) =
            snakeToCamel(s.toString)
          override def objectAttributeKeyWriteMap(s: CharSequence) =
            camelToSnake(s.toString)

          override def objectTypeKeyReadMap(s: CharSequence) =
            snakeToCamel(s.toString)
          override def objectTypeKeyWriteMap(s: CharSequence) =
            camelToSnake(s.toString)
        }

        // Default read-writing
        com.rallyhealth.upickle.v1.default.write(Thing(1, "gg")) ==>
          """{"myFieldA":1,"myFieldB":"gg"}"""

        com.rallyhealth.upickle.v1.default.read[Thing]("""{"myFieldA":1,"myFieldB":"gg"}""") ==>
          Thing(1, "gg")

        implicit def thingRW: SnakePickle.ReadWriter[Thing] = SnakePickle.macroRW

        // snake_case_keys read-writing
        SnakePickle.write(Thing(1, "gg")) ==>
          """{"my_field_a":1,"my_field_b":"gg"}"""

        SnakePickle.read[Thing]("""{"my_field_a":1,"my_field_b":"gg"}""") ==>
          Thing(1, "gg")
      }

      test("stringLongs"){
        com.rallyhealth.upickle.v1.default.write(123: Long) ==> "123"
        com.rallyhealth.upickle.v1.default.write(Long.MaxValue) ==> "\"9223372036854775807\""

        object StringLongs extends com.rallyhealth.upickle.v1.AttributeTagged{
          override implicit val LongWriter = new Writer[Long] {
            def write0[V](out: Visitor[_, V], v: Long) = out.visitString(v.toString, -1)
          }
        }

        StringLongs.write(123: Long) ==> "\"123\""
        StringLongs.write(Long.MaxValue) ==> "\"9223372036854775807\""

        object NumLongs extends com.rallyhealth.upickle.v1.AttributeTagged{
          override implicit val LongWriter = new Writer[Long] {
            def write0[V](out: Visitor[_, V], v: Long) = out.visitFloat64String(v.toString, -1)
          }
        }

        NumLongs.write(123: Long) ==> "123"
        NumLongs.write(Long.MaxValue) ==> "9223372036854775807"

      }
    }

    test("transform"){
      com.rallyhealth.upickle.v1.default.transform(Foo(123)).to[Foo] ==> Foo(123)
      val big = Big(1, true, "lol", 'Z', Thing(7, ""))
      com.rallyhealth.upickle.v1.default.transform(big).to[Big] ==> big
    }
    test("msgConstruction"){
      val msg = com.rallyhealth.upack.v1.Arr(
        com.rallyhealth.upack.v1.Obj(com.rallyhealth.upack.v1.Str("myFieldA") -> com.rallyhealth.upack.v1.Int32(1), com.rallyhealth.upack.v1.Str("myFieldB") -> com.rallyhealth.upack.v1.Str("g")),
        com.rallyhealth.upack.v1.Obj(com.rallyhealth.upack.v1.Str("myFieldA") -> com.rallyhealth.upack.v1.Int32(2), com.rallyhealth.upack.v1.Str("myFieldB") -> com.rallyhealth.upack.v1.Str("k"))
      )

      val binary: Array[Byte] = com.rallyhealth.upack.v1.write(msg)

      val read = com.rallyhealth.upack.v1.read(binary)
      assert(msg == read)
    }

    test("msgReadWrite"){
      val big = Big(1, true, "lol", 'Z', Thing(7, ""))
      val msg: com.rallyhealth.upack.v1.Msg = com.rallyhealth.upickle.v1.default.writeMsg(big)
      com.rallyhealth.upickle.v1.default.readBinary[Big](msg) ==> big
    }

    test("msgInsideValue"){
      val msgSeq = Seq[com.rallyhealth.upack.v1.Msg](
        com.rallyhealth.upack.v1.Str("hello world"),
        com.rallyhealth.upack.v1.Arr(com.rallyhealth.upack.v1.Int32(1), com.rallyhealth.upack.v1.Int32(2))
      )

      val binary: Array[Byte] = com.rallyhealth.upickle.v1.default.writeBinary(msgSeq)

      com.rallyhealth.upickle.v1.default.readBinary[Seq[com.rallyhealth.upack.v1.Msg]](binary) ==> msgSeq
    }

    test("msgToValueon"){
      val msg = com.rallyhealth.upack.v1.Arr(
        com.rallyhealth.upack.v1.Obj(com.rallyhealth.upack.v1.Str("myFieldA") -> com.rallyhealth.upack.v1.Int32(1), com.rallyhealth.upack.v1.Str("myFieldB") -> com.rallyhealth.upack.v1.Str("g")),
        com.rallyhealth.upack.v1.Obj(com.rallyhealth.upack.v1.Str("myFieldA") -> com.rallyhealth.upack.v1.Int32(2), com.rallyhealth.upack.v1.Str("myFieldB") -> com.rallyhealth.upack.v1.Str("k"))
      )

      val binary: Array[Byte] = com.rallyhealth.upack.v1.write(msg)

      // Can pretty-print starting from either the com.rallyhealth.upack.v1.Msg structs,
      // or the raw binary data
      com.rallyhealth.upack.v1.transform(msg, new com.rallyhealth.ujson.v1.StringRenderer()).toString ==>
        """[{"myFieldA":1,"myFieldB":"g"},{"myFieldA":2,"myFieldB":"k"}]"""

      com.rallyhealth.upack.v1.transform(binary, new com.rallyhealth.ujson.v1.StringRenderer()).toString ==>
        """[{"myFieldA":1,"myFieldB":"g"},{"myFieldA":2,"myFieldB":"k"}]"""

      // Some messagepack structs cannot be converted to valid JSON, e.g.
      // they may have maps with non-string keys. These can still be pretty-printed:
      val msg2 = com.rallyhealth.upack.v1.Obj(com.rallyhealth.upack.v1.Arr(com.rallyhealth.upack.v1.Int32(1), com.rallyhealth.upack.v1.Int32(2)) -> com.rallyhealth.upack.v1.Int32(1))
      com.rallyhealth.upack.v1.transform(msg2, new com.rallyhealth.ujson.v1.StringRenderer()).toString ==> """{[1,2]:1}"""
    }
    test("json"){
      test("construction"){
        import com.rallyhealth.ujson.v1.Value

        val json0 = com.rallyhealth.ujson.v1.Arr(
          com.rallyhealth.ujson.v1.Obj("myFieldA" -> com.rallyhealth.ujson.v1.Num(1), "myFieldB" -> com.rallyhealth.ujson.v1.Str("g")),
          com.rallyhealth.ujson.v1.Obj("myFieldA" -> com.rallyhealth.ujson.v1.Num(2), "myFieldB" -> com.rallyhealth.ujson.v1.Str("k"))
        )

        val json = com.rallyhealth.ujson.v1.Arr( // The `com.rallyhealth.ujson.v1.Num` and `com.rallyhealth.ujson.v1.Str` calls are optional
          com.rallyhealth.ujson.v1.Obj("myFieldA" -> 1, "myFieldB" -> "g"),
          com.rallyhealth.ujson.v1.Obj("myFieldA" -> 2, "myFieldB" -> "k")
        )

        json0 ==> json
        json.toString ==> """[{"myFieldA":1,"myFieldB":"g"},{"myFieldA":2,"myFieldB":"k"}]"""

        val json2 = com.rallyhealth.ujson.v1.Obj(
          "hello" -> (0 until 5),
          "world" -> (0 until 5).map(i => (i.toString, i))
        )

        json2.toString ==> """{"hello":[0,1,2,3,4],"world":{"0":0,"1":1,"2":2,"3":3,"4":4}}"""
      }
      test("simple"){
        val str = """[{"myFieldA":1,"myFieldB":"g"},{"myFieldA":2,"myFieldB":"k"}]"""
        val json = com.rallyhealth.ujson.v1.read(str)
        json(0)("myFieldA").num   ==> 1
        json(0)("myFieldB").str   ==> "g"
        json(1)("myFieldA").num   ==> 2
        json(1)("myFieldB").str   ==> "k"

        com.rallyhealth.ujson.v1.write(json)         ==> str
      }
      test("mutable"){
        val str = """[{"myFieldA":1,"myFieldB":"g"},{"myFieldA":2,"myFieldB":"k"}]"""
        val json: com.rallyhealth.ujson.v1.Value = com.rallyhealth.ujson.v1.read(str)

        json.arr.remove(1)
        json(0)("myFieldA") = 1337
        json(0)("myFieldB") = json(0)("myFieldB").str + "lols"

        com.rallyhealth.ujson.v1.write(json) ==> """[{"myFieldA":1337,"myFieldB":"glols"}]"""
      }
      test("update"){
        val str = """[{"myFieldA":1,"myFieldB":"g"},{"myFieldA":2,"myFieldB":"k"}]"""
        val json: com.rallyhealth.ujson.v1.Value = com.rallyhealth.ujson.v1.read(str)

        json(0)("myFieldA") = _.num + 100
        json(1)("myFieldB") = _.str + "lol"

        val expected = """[{"myFieldA":101,"myFieldB":"g"},{"myFieldA":2,"myFieldB":"klol"}]"""
        com.rallyhealth.ujson.v1.write(json) ==> expected
      }
      test("intermediate"){
        val data = Seq(Thing(1, "g"), Thing(2, "k"))
        val json = com.rallyhealth.upickle.v1.default.writeJs(data)

        json.arr.remove(1)
        json(0)("myFieldA") = 1337

        com.rallyhealth.upickle.v1.default.read[Seq[Thing]](json)   ==> Seq(Thing(1337, "g"))
      }
      test("copy"){
        val data = com.rallyhealth.ujson.v1.Obj(
          "hello" -> 1,
          "world" -> 2
        )

        val data2 = com.rallyhealth.ujson.v1.copy(data)

        data("hello") = 3
        data2("hello").num ==> 1
      }
    }
    test("transforms"){
      test("json"){
        import com.rallyhealth.upickle.v1.default._
        transform(1).to[com.rallyhealth.ujson.v1.Value] ==> com.rallyhealth.ujson.v1.Num(1)
        transform("hello").to[com.rallyhealth.ujson.v1.Value] ==> com.rallyhealth.ujson.v1.Str("hello")
        transform(("hello", 9)).to[com.rallyhealth.ujson.v1.Value] ==> com.rallyhealth.ujson.v1.Arr("hello", 9)
        transform(Thing(3, "3")).to[com.rallyhealth.ujson.v1.Value] ==>
          com.rallyhealth.ujson.v1.Obj("myFieldA" -> 3, "myFieldB" -> "3")

        transform(com.rallyhealth.ujson.v1.Num(1)).to[Int] ==> 1
        transform(com.rallyhealth.ujson.v1.Str("hello")).to[String] ==> "hello"
        transform(com.rallyhealth.ujson.v1.Arr("hello", 9)).to[(String, Int)] ==> ("hello", 9)
        transform(com.rallyhealth.ujson.v1.Obj("myFieldA" -> 3, "myFieldB" -> "3")).to[Thing] ==>
          Thing(3, "3")
      }

      test("defaultTransform"){

        // com.rallyhealth.upickle.v1.default.transform can be used to convert between
        // JSON-equivalent data-structures without an intermediate AST
        com.rallyhealth.upickle.v1.default.transform(Seq(1, 2, 3)).to[(Int, Int, Int)] ==> (1, 2, 3)

        val bar = Bar("omg", Seq(Foo(1), Foo(2)))

        com.rallyhealth.upickle.v1.default.transform(bar).to[Map[String, com.rallyhealth.ujson.v1.Value]] ==>
          Map[String, com.rallyhealth.ujson.v1.Value](
            "name" -> "omg",
            "foos" -> com.rallyhealth.ujson.v1.Arr(
              com.rallyhealth.ujson.v1.Obj("i" -> 1),
              com.rallyhealth.ujson.v1.Obj("i" -> 2)
            )
          )

      }
      test("misc"){
        // It can be used for parsing JSON into an AST
        val exampleAst = com.rallyhealth.ujson.v1.Arr(1, 2, 3)

        com.rallyhealth.ujson.v1.transform("[1, 2, 3]", Value) ==> exampleAst

        // Rendering the AST to a string
        com.rallyhealth.ujson.v1.transform(exampleAst, StringRenderer()).toString ==> "[1,2,3]"

        // Or to a byte array
        com.rallyhealth.ujson.v1.transform(exampleAst, BytesRenderer()).toBytes ==> "[1,2,3]".getBytes

        // Re-formatting JSON, either compacting it
        com.rallyhealth.ujson.v1.transform("[1, 2, 3]", StringRenderer()).toString ==> "[1,2,3]"

        // or indenting it
        com.rallyhealth.ujson.v1.transform("[1, 2, 3]", StringRenderer(indent = 4)).toString ==>
          """[
            |    1,
            |    2,
            |    3
            |]""".stripMargin

        // `transform` takes any `Transformable`, including byte arrays and files
        com.rallyhealth.ujson.v1.transform("[1, 2, 3]".getBytes, StringRenderer()).toString ==> "[1,2,3]"

      }
      test("validate"){
        com.rallyhealth.ujson.v1.transform("[1, 2, 3]", NoOpVisitor)

        intercept[IncompleteParseException](
          com.rallyhealth.ujson.v1.transform("[1, 2, 3", NoOpVisitor)
        )
        intercept[ParseException](
          com.rallyhealth.ujson.v1.transform("[1, 2, 3]]", NoOpVisitor)
        )
      }
      test("com.rallyhealth.upickle.v1Default"){
        com.rallyhealth.ujson.v1.transform("[1, 2, 3]", com.rallyhealth.upickle.v1.default.reader[Seq[Int]]) ==>
          Seq(1, 2, 3)

        com.rallyhealth.ujson.v1.transform(com.rallyhealth.upickle.v1.default.transform(Seq(1, 2, 3)), StringRenderer()).toString ==>
          "[1,2,3]"
      }
    }
    test("byteArrays"){
      import com.rallyhealth.upickle.v1.default._
      write(Array[Byte](1, 2, 3, 4)) ==> "[1,2,3,4]"
      read[Array[Byte]]("[1,2,3,4]") ==> Array(1, 2, 3, 4)

      writeBinary(Array[Byte](1, 2, 3, 4)) ==> Array(0xc4.toByte, 4, 1, 2, 3, 4)
      readBinary[Array[Byte]](Array[Byte](0xc4.toByte, 4, 1, 2, 3, 4)) ==> Array(1, 2, 3, 4)
    }
  }
}


