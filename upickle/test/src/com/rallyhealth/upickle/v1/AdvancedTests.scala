package com.rallyhealth.upickle.v1
import utest._
import acyclic.file
import com.rallyhealth.upickle.v1.TestUtil.rw

object shared {
  object that {
    import common.Message
    case class That(common: Message)
    object That{
      implicit def rw: com.rallyhealth.upickle.v1.default.ReadWriter[That] = com.rallyhealth.upickle.v1.default.macroRW
    }
  }
  object other {
    import common.Message
    case class Other(common: Message)
    object Other{
      implicit def rw: com.rallyhealth.upickle.v1.default.ReadWriter[Other] = com.rallyhealth.upickle.v1.default.macroRW
    }
  }
  object common {
    case class Message(content: String)
    object Message{
      implicit def rw: com.rallyhealth.upickle.v1.default.ReadWriter[Message] = com.rallyhealth.upickle.v1.default.macroRW
    }
  }
}

object All {
  import shared.other._
  sealed trait Outers
  object Outers{
    implicit def rw: com.rallyhealth.upickle.v1.default.ReadWriter[Outers] = com.rallyhealth.upickle.v1.default.ReadWriter.merge(
      Out1.rw
    )
  }
  case class Out1(a: Other) extends Outers
  object Out1{
    implicit def rw: com.rallyhealth.upickle.v1.default.ReadWriter[Out1] = com.rallyhealth.upickle.v1.default.macroRW
  }

  import shared.that._
  import shared.common._
  sealed trait Inners extends Outers
  object Inners{
    implicit def rw: com.rallyhealth.upickle.v1.default.ReadWriter[Inners] = com.rallyhealth.upickle.v1.default.ReadWriter.merge(
      Inner1.rw,
      Inner2.rw
    )
  }
  case class Inner1(b: That) extends Inners
  object Inner1{
    implicit def rw: com.rallyhealth.upickle.v1.default.ReadWriter[Inner1] = com.rallyhealth.upickle.v1.default.macroRW
  }
  case class Inner2(a: Message) extends Inners
  object Inner2{
    implicit def rw: com.rallyhealth.upickle.v1.default.ReadWriter[Inner2] = com.rallyhealth.upickle.v1.default.macroRW
  }
}

object AdvancedTests extends TestSuite {
  import All._
  val tests = Tests {
    "complexTraits" - {
      val reader = implicitly[com.rallyhealth.upickle.v1.default.Reader[Outers]]
      val writer = implicitly[com.rallyhealth.upickle.v1.default.Writer[Outers]]
      assert(reader != null)
      assert(writer != null)
    }
    test("GenericDataTypes"){
      test("simple"){
        import Generic.A
        test - rw(A(1), """{"t":1}""")
        test - rw(A("1"), """{"t":"1"}""")
        test - rw(A(Seq("1", "2", "3")), """{"t":["1","2","3"]}""")
        test - rw(A(A(A(A(A(A(A(1))))))), """{"t":{"t":{"t":{"t":{"t":{"t":{"t":1}}}}}}}""")
      }
      test("large"){
        import Generic.ADT
        rw(ADT(1, 2, 3, 4, 5, 6), """{"a":1,"b":2,"c":3,"d":4,"e":5,"f":6}""")
        rw(
          ADT(
            ADT(1, 2, 3, 4, 5, 6),
            ADT(1, 2, 3, 4, 5, 6),
            ADT(1, 2, 3, 4, 5, 6),
            ADT(1, 2, 3, 4, 5, 6),
            ADT(1, 2, 3, 4, 5, 6),
            ADT(1, 2, 3, 4, 5, 6)
          ),
          """{"a":{"a":1,"b":2,"c":3,"d":4,"e":5,"f":6},"b":{"a":1,"b":2,"c":3,"d":4,"e":5,"f":6},"c":{"a":1,"b":2,"c":3,"d":4,"e":5,"f":6},"d":{"a":1,"b":2,"c":3,"d":4,"e":5,"f":6},"e":{"a":1,"b":2,"c":3,"d":4,"e":5,"f":6},"f":{"a":1,"b":2,"c":3,"d":4,"e":5,"f":6}}"""
        )
      }
      test("ADT"){
        import GenericADTs._
        test - {
          val pref1 = "com.rallyhealth.upickle.v1.GenericADTs.Delta"
          val D1 = Delta
          type D1[+A, +B] = Delta[A, B]
          rw(D1.Insert(1, 1), s"""{"$$type":"$pref1.Insert","key":1,"value":1}""")
          rw(D1.Insert(1, 1): D1[Int, Int], s"""{"$$type":"$pref1.Insert","key":1,"value":1}""")
          rw(D1.Remove(1), s"""{"$$type":"$pref1.Remove","key":1}""")
          rw(D1.Remove(1): D1[Int, Int], s"""{"$$type":"$pref1.Remove","key":1}""")
          rw(D1.Clear(), s"""{"$$type":"$pref1.Clear"}""")
          rw(D1.Clear(): D1[Int, Int], s"""{"$$type":"$pref1.Clear"}""")
        }
        test - {
          val pref2 = "com.rallyhealth.upickle.v1.GenericADTs.DeltaInvariant"
          val D2 = DeltaInvariant
          type D2[A, B] = DeltaInvariant[A, B]
          rw(D2.Insert(1, 1), s"""{"$$type":"$pref2.Insert","key":1,"value":1}""")
          rw(D2.Insert(1, 1): D2[Int, Int], s"""{"$$type":"$pref2.Insert","key":1,"value":1}""")
          rw(D2.Remove(1), s"""{"$$type":"$pref2.Remove","key":1}""")
          rw(D2.Remove(1): D2[Int, Int], s"""{"$$type":"$pref2.Remove","key":1}""")
          rw(D2.Clear(), s"""{"$$type":"$pref2.Clear"}""")
          rw(D2.Clear(): D2[Int, Int], s"""{"$$type":"$pref2.Clear"}""")
        }
      }
    }

    test("recursiveDataTypes"){
      import Recursive._
      rw(
        IntTree(123, List(IntTree(456, Nil), IntTree(789, Nil))),
        """{
          "value": 123,
          "children": [
            {"value":456,"children":[]},
            {"value":789,"children":[]}
          ]
        }"""
      )
      rw(
        SingleNode(123, List(SingleNode(456, Nil), SingleNode(789, Nil))),
        """{
          "$type": "com.rallyhealth.upickle.v1.Recursive.SingleNode",
          "value": 123,
          "children": [
            {
              "$type": "com.rallyhealth.upickle.v1.Recursive.SingleNode",
              "value": 456,
              "children": []
            },
            {
              "$type": "com.rallyhealth.upickle.v1.Recursive.SingleNode",
              "value":789,
              "children":[]
            }
          ]
        }"""
      )
      rw(
        SingleNode(123, List(SingleNode(456, Nil), SingleNode(789, Nil))): SingleTree,
        """{
          "$type": "com.rallyhealth.upickle.v1.Recursive.SingleNode",
          "value": 123,
          "children": [
            {
              "$type": "com.rallyhealth.upickle.v1.Recursive.SingleNode",
              "value": 456,
              "children": []
            },
            {
              "$type": "com.rallyhealth.upickle.v1.Recursive.SingleNode",
              "value":789,
              "children":[]
            }
          ]
        }"""
      )
      rw(End: LL, """{"$type":"com.rallyhealth.upickle.v1.Recursive.End"}""")
      rw(Node(3, End): LL,
        """{
          "$type": "com.rallyhealth.upickle.v1.Recursive.Node",
          "c": 3,
          "next": {"$type":"com.rallyhealth.upickle.v1.Recursive.End"}
        }""")
      rw(Node(6, Node(3, End)),
        """{
          "$type": "com.rallyhealth.upickle.v1.Recursive.Node",
          "c": 6,
          "next": {
            "$type": "com.rallyhealth.upickle.v1.Recursive.Node",
            "c":3,
            "next":{"$type":"com.rallyhealth.upickle.v1.Recursive.End"}
          }
        }""")

    }

    test("issues"){
      test("issue95"){
        rw(
          Tuple1(List(C1("hello", List("world")))),
          """[[{"name": "hello", "types": ["world"]}]]"""
        )
        rw(
          C2(List(C1("hello", List("world")))),
          """{"results": [{"name": "hello", "types": ["world"]}]}"""
        )

        rw(
          GeoCoding2(List(Result2("a", "b", List("c"))), "d"),
          """{"results": [{"name": "a", "whatever": "b", "types": ["c"]}], "status": "d"}"""
        )
      }
      test("scalatex"){
        val block = Ast.Block(1, Seq(Ast.Block.Text(2, "hello")))
        val blockText = """{
            "$type":"com.rallyhealth.upickle.v1.Ast.Block",
            "offset":1,
            "parts":[
              {
                "$type": "com.rallyhealth.upickle.v1.Ast.Block.Text",
                "offset":2,
                "txt":"hello"
              }
            ]
          }"""
        rw(block: Ast, blockText)
        rw(block: Ast.Block, blockText)
        rw(block: Ast.Block.Sub, blockText)
        rw(block: Ast.Chain.Sub, blockText)

        val header = Ast.Header(0, "Hello", block)
        val headerText = s"""{
          "$$type": "com.rallyhealth.upickle.v1.Ast.Header",
          "offset": 0,
          "front": "Hello",
          "block": $blockText
        }"""
        rw(header: Ast, headerText)
        rw(header: Ast.Header, headerText)
        rw(header: Ast.Block.Sub, headerText)
        rw(header: Ast.Chain.Sub, headerText)
      }
      //      test("companionImplicitPickedUp"){
      //        assert(implicitly[com.rallyhealth.upickle.v1.default.Reader[TypedFoo]] eq TypedFoo.readWriter)
      //        assert(implicitly[com.rallyhealth.upickle.v1.default.Writer[TypedFoo]] eq TypedFoo.readWriter)
      //        assert(implicitly[com.rallyhealth.upickle.v1.default.ReadWriter[TypedFoo]] eq TypedFoo.readWriter)
      //      }
      //      test("companionImplicitWorks"){
      //
      //        rw(TypedFoo.Bar(1): TypedFoo, """{"$type": "com.rallyhealth.upickle.v1.TypedFoo.Bar", "i": 1}""")
      //        rw(TypedFoo.Baz("lol"): TypedFoo, """{"$type": "com.rallyhealth.upickle.v1.TypedFoo.Baz", "s": "lol"}""")
      //        rw(TypedFoo.Quz(true): TypedFoo, """{"$type": "com.rallyhealth.upickle.v1.TypedFoo.Quz", "b": true}""")
      //      }
    }

  }
}
