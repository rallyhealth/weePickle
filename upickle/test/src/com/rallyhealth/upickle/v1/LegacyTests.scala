package com.rallyhealth.upickle.v1
import utest._
import LegacyTestUtil.rw

import com.rallyhealth.upickle.v1.legacy.{ReadWriter => RW, Reader => R, Writer => W}
object LegacyTests extends TestSuite {

  val tests = Tests {
    test("simpleAdt"){
      implicit def ADT0rw: RW[ADTs.ADT0] = com.rallyhealth.upickle.v1.legacy.macroRW
      implicit def ADTarw: RW[ADTs.ADTa] = com.rallyhealth.upickle.v1.legacy.macroRW
      implicit def ADTbrw: RW[ADTs.ADTb] = com.rallyhealth.upickle.v1.legacy.macroRW
      implicit def ADTcrw: RW[ADTs.ADTc] = com.rallyhealth.upickle.v1.legacy.macroRW
      implicit def ADTdrw: RW[ADTs.ADTd] = com.rallyhealth.upickle.v1.legacy.macroRW
      implicit def ADTerw: RW[ADTs.ADTe] = com.rallyhealth.upickle.v1.legacy.macroRW
      implicit def ADTfrw: RW[ADTs.ADTf] = com.rallyhealth.upickle.v1.legacy.macroRW
      implicit def ADTzrw: RW[ADTs.ADTz] = com.rallyhealth.upickle.v1.legacy.macroRW

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

      test - rw(
        ADTs.ADTf(1, "lol", (1.1, 1.2), ADTs.ADTa(1), List(1.2, 2.1, 3.14), Some(None)),
        """{"i":1,"s":"lol","t":[1.1,1.2],"a":{"i":1},"q":[1.2,2.1,3.14],"o":[[]]}"""
      )
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

    test("sealedHierarchy"){
      // objects in sealed case class hierarchies should always read and write
      // the same way (with a tag) regardless of what their static type is when
      // written. This is feasible because sealed hierarchies can only have a
      // finite number of cases, so we can just check them all and decide which
      // class the instance belongs to.
      import Hierarchy._
      implicit def Brw: RW[B] = com.rallyhealth.upickle.v1.legacy.macroRW
      implicit def Crw: RW[C] = com.rallyhealth.upickle.v1.legacy.macroRW
      implicit def Arw: RW[A] = com.rallyhealth.upickle.v1.legacy.ReadWriter.merge(Crw, Brw)

      implicit def Zrw: RW[Z] = com.rallyhealth.upickle.v1.legacy.macroRW
      test("shallow"){
        test - rw(B(1), """["com.rallyhealth.upickle.v1.Hierarchy.B",{"i":1}]""")
        test - rw(C("a", "b"), """["com.rallyhealth.upickle.v1.Hierarchy.C",{"s1":"a","s2":"b"}]""")

        test - rw(AnZ: Z, """["com.rallyhealth.upickle.v1.Hierarchy.AnZ",{}]""")
        test - rw(AnZ, """["com.rallyhealth.upickle.v1.Hierarchy.AnZ",{}]""")

        test - rw(Hierarchy.B(1): Hierarchy.A, """["com.rallyhealth.upickle.v1.Hierarchy.B", {"i":1}]""")
        test - rw(C("a", "b"): A, """["com.rallyhealth.upickle.v1.Hierarchy.C",{"s1":"a","s2":"b"}]""")
      }

      test("deep"){
        import DeepHierarchy._
        implicit def Arw: RW[A] = com.rallyhealth.upickle.v1.legacy.macroRW
        implicit def Brw: RW[B] = com.rallyhealth.upickle.v1.legacy.macroRW
        implicit def Crw: RW[C] = com.rallyhealth.upickle.v1.legacy.macroRW
        implicit def AnQrw: RW[AnQ] = com.rallyhealth.upickle.v1.legacy.macroRW
        implicit def Qrw: RW[Q] = com.rallyhealth.upickle.v1.legacy.macroRW
        implicit def Drw: RW[D] = com.rallyhealth.upickle.v1.legacy.macroRW
        implicit def Erw: RW[E] = com.rallyhealth.upickle.v1.legacy.macroRW
        implicit def Frw: RW[F] = com.rallyhealth.upickle.v1.legacy.macroRW
        test - rw(B(1), """["com.rallyhealth.upickle.v1.DeepHierarchy.B",{"i":1}]""")
        test - rw(B(1): A, """["com.rallyhealth.upickle.v1.DeepHierarchy.B",{"i":1}]""")
        test - rw(AnQ(1): Q, """["com.rallyhealth.upickle.v1.DeepHierarchy.AnQ",{"i":1}]""")
        test - rw(AnQ(1), """["com.rallyhealth.upickle.v1.DeepHierarchy.AnQ",{"i":1}]""")

        test - rw(F(AnQ(1)), """["com.rallyhealth.upickle.v1.DeepHierarchy.F",{"q":["com.rallyhealth.upickle.v1.DeepHierarchy.AnQ",{"i":1}]}]""")
        test - rw(F(AnQ(2)): A, """["com.rallyhealth.upickle.v1.DeepHierarchy.F",{"q":["com.rallyhealth.upickle.v1.DeepHierarchy.AnQ",{"i":2}]}]""")
        test - rw(F(AnQ(3)): C, """["com.rallyhealth.upickle.v1.DeepHierarchy.F",{"q":["com.rallyhealth.upickle.v1.DeepHierarchy.AnQ",{"i":3}]}]""")
        test - rw(D("1"), """["com.rallyhealth.upickle.v1.DeepHierarchy.D",{"s":"1"}]""")
        test - rw(D("1"): C, """["com.rallyhealth.upickle.v1.DeepHierarchy.D",{"s":"1"}]""")
        test - rw(D("1"): A, """["com.rallyhealth.upickle.v1.DeepHierarchy.D",{"s":"1"}]""")
        test - rw(E(true), """["com.rallyhealth.upickle.v1.DeepHierarchy.E",{"b":true}]""")
        test - rw(E(true): C, """["com.rallyhealth.upickle.v1.DeepHierarchy.E",{"b":true}]""")
        test - rw(E(true): A, """["com.rallyhealth.upickle.v1.DeepHierarchy.E",{"b":true}]""")
      }
    }
    test("singleton"){
      import Singletons._

      implicit def AArw: RW[AA] = legacy.macroRW
      rw(BB, """["com.rallyhealth.upickle.v1.Singletons.BB",{}]""")
      rw(CC, """["com.rallyhealth.upickle.v1.Singletons.CC",{}]""")
      rw(BB: AA, """["com.rallyhealth.upickle.v1.Singletons.BB",{}]""")
      rw(CC: AA, """["com.rallyhealth.upickle.v1.Singletons.CC",{}]""")
    }
    test("robustnessAgainstVaryingSchemas"){
      test("renameKeysViaAnnotations"){
        import Annotated._
        implicit def Arw: RW[A] = com.rallyhealth.upickle.v1.legacy.macroRW
        implicit def Brw: RW[B] = com.rallyhealth.upickle.v1.legacy.macroRW
        implicit def Crw: RW[C] = com.rallyhealth.upickle.v1.legacy.macroRW
        test - rw(B(1), """["0", {"omg":1}]""")
        test - rw(C("a", "b"), """["1", {"lol":"a","wtf":"b"}]""")

        test - rw(B(1): A, """["0", {"omg":1}]""")
        test - rw(C("a", "b"): A, """["1", {"lol":"a","wtf":"b"}]""")
      }
      test("useDefaults"){
        // Ignore the values which match the default when writing and
        // substitute in defaults when reading if the key is missing
        import Defaults._
        implicit def Arw: RW[ADTa] = com.rallyhealth.upickle.v1.legacy.macroRW
        implicit def Brw: RW[ADTb] = com.rallyhealth.upickle.v1.legacy.macroRW
        implicit def Crw: RW[ADTc] = com.rallyhealth.upickle.v1.legacy.macroRW
        test - rw(ADTa(), "{}")
        test - rw(ADTa(321), """{"i":321}""")
        test - rw(ADTb(s = "123"), """{"s":"123"}""")
        test - rw(ADTb(i = 234, s = "567"), """{"i":234,"s":"567"}""")
        test - rw(ADTc(s = "123"), """{"s":"123"}""")
        test - rw(ADTc(i = 234, s = "567"), """{"i":234,"s":"567"}""")
        test - rw(ADTc(t = (12.3, 45.6), s = "789"), """{"s":"789","t":[12.3,45.6]}""")
        test - rw(ADTc(t = (12.3, 45.6), s = "789", i = 31337), """{"i":31337,"s":"789","t":[12.3,45.6]}""")
      }
      test("ignoreExtraFieldsWhenDeserializing"){
        import ADTs._
        implicit def ADTarw: RW[ADTs.ADTa] = com.rallyhealth.upickle.v1.legacy.macroRW
        implicit def ADTbrw: RW[ADTs.ADTb] = com.rallyhealth.upickle.v1.legacy.macroRW

        val r1 = com.rallyhealth.upickle.v1.legacy.read[ADTa]( """{"i":123, "j":false, "k":"haha"}""")
        assert(r1 == ADTa(123))
        val r2 = com.rallyhealth.upickle.v1.legacy.read[ADTb]( """{"i":123, "j":false, "k":"haha", "s":"kk", "l":true, "z":[1, 2, 3]}""")
        assert(r2 == ADTb(123, "kk"))
      }
    }

    test("generics"){
      import GenericADTs._
      test - {
        val pref1 = "com.rallyhealth.upickle.v1.GenericADTs.Delta"
        val D1 = Delta
        implicit def D1rw[A: R: W, B: R: W]: RW[D1[A, B]] = com.rallyhealth.upickle.v1.legacy.macroRW
        implicit def Insertrw[A: R: W, B: R: W]: RW[D1.Insert[A, B]] = com.rallyhealth.upickle.v1.legacy.macroRW
        implicit def Removerw[A: R: W]: RW[D1.Remove[A]] = com.rallyhealth.upickle.v1.legacy.macroRW
        implicit def Clearrw: RW[D1.Clear] = com.rallyhealth.upickle.v1.legacy.macroRW
        type D1[+A, +B] = Delta[A, B]
        rw(D1.Insert(1, 1), s"""["$pref1.Insert",{"key":1,"value":1}]""")
        rw(D1.Insert(1, 1): D1[Int, Int], s"""["$pref1.Insert",{"key":1,"value":1}]""")
        rw(D1.Remove(1), s"""["$pref1.Remove",{"key":1}]""")
        rw(D1.Remove(1): D1[Int, Int], s"""["$pref1.Remove",{"key":1}]""")
        rw(D1.Clear(), s"""["$pref1.Clear",{}]""")
        rw(D1.Clear(): D1[Int, Int], s"""["$pref1.Clear",{}]""")
      }
      test - {
        val pref2 = "com.rallyhealth.upickle.v1.GenericADTs.DeltaInvariant"
        val D2 = DeltaInvariant
        type D2[A, B] = DeltaInvariant[A, B]
        implicit def D2rw[A: R: W, B: R: W]: RW[D2[A, B]] = com.rallyhealth.upickle.v1.legacy.macroRW
        implicit def Insertrw[A: R: W, B: R: W]: RW[D2.Insert[A, B]] = com.rallyhealth.upickle.v1.legacy.macroRW
        implicit def Removerw[A: R: W, B]: RW[D2.Remove[A, B]] = com.rallyhealth.upickle.v1.legacy.macroRW
        implicit def Clearrw[A, B]: RW[D2.Clear[A, B]] = com.rallyhealth.upickle.v1.legacy.macroRW
        rw(D2.Insert(1, 1), s"""["$pref2.Insert",{"key":1,"value":1}]""")
        rw(D2.Insert(1, 1): D2[Int, Int], s"""["$pref2.Insert",{"key":1,"value":1}]""")
        rw(D2.Remove(1), s"""["$pref2.Remove",{"key":1}]""")
        rw(D2.Remove(1): D2[Int, Int], s"""["$pref2.Remove",{"key":1}]""")
        rw(D2.Clear(), s"""["$pref2.Clear",{}]""")
        rw(D2.Clear(): D2[Int, Int], s"""["$pref2.Clear",{}]""")
      }
    }
    test("recursiveDataTypes"){
      import Recursive._
      implicit def IntTreerw: RW[IntTree] = com.rallyhealth.upickle.v1.legacy.macroRW
      implicit def SingleNoderw: RW[SingleNode] = com.rallyhealth.upickle.v1.legacy.macroRW

      implicit def SingleTreerw: RW[SingleTree] = com.rallyhealth.upickle.v1.legacy.macroRW

      implicit def Noderw: RW[Node] = com.rallyhealth.upickle.v1.legacy.macroRW


      implicit def LLrw: RW[LL] = com.rallyhealth.upickle.v1.legacy.macroRW
      rw(
        IntTree(123, List(IntTree(456, Nil), IntTree(789, Nil))),
        """{"value":123,"children":[{"value":456,"children":[]},{"value":789,"children":[]}]}"""
      )
      rw(
        SingleNode(123, List(SingleNode(456, Nil), SingleNode(789, Nil))),
        """["com.rallyhealth.upickle.v1.Recursive.SingleNode",{"value":123,"children":[["com.rallyhealth.upickle.v1.Recursive.SingleNode",{"value":456,"children":[]}],["com.rallyhealth.upickle.v1.Recursive.SingleNode",{"value":789,"children":[]}]]}]"""
      )
      rw(
        SingleNode(123, List(SingleNode(456, Nil), SingleNode(789, Nil))): SingleTree,
        """["com.rallyhealth.upickle.v1.Recursive.SingleNode",{"value":123,"children":[["com.rallyhealth.upickle.v1.Recursive.SingleNode",{"value":456,"children":[]}],["com.rallyhealth.upickle.v1.Recursive.SingleNode",{"value":789,"children":[]}]]}]"""
      )
      rw(End: LL, """["com.rallyhealth.upickle.v1.Recursive.End",{}]""")
      rw(Node(3, End): LL, """["com.rallyhealth.upickle.v1.Recursive.Node",{"c":3,"next":["com.rallyhealth.upickle.v1.Recursive.End",{}]}]""")
      rw(Node(6, Node(3, End)), """["com.rallyhealth.upickle.v1.Recursive.Node",{"c":6,"next":["com.rallyhealth.upickle.v1.Recursive.Node",{"c":3,"next":["com.rallyhealth.upickle.v1.Recursive.End",{}]}]}]""")

    }
    test("varargs"){
      implicit def IntTreerw: RW[Varargs.Sentence] = com.rallyhealth.upickle.v1.legacy.macroRW
      rw(Varargs.Sentence("a", "b", "c"), """{"a":"a","bs":["b","c"]}""")
      rw(Varargs.Sentence("a"), """{"a":"a","bs":[]}""")
    }


    test("issues"){
      test("issue95"){
        implicit def rw1: RW[C1] = legacy.macroRW
        implicit def rw2: RW[C2] = legacy.macroRW
        implicit def rw3: RW[GeoCoding2] = legacy.macroRW
        implicit def rw4: RW[Result2] = legacy.macroRW
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
        implicit def rw1: RW[Ast] = legacy.macroRW
        implicit def rw2: RW[Ast.Block] = legacy.macroRW
        implicit def rw3: RW[Ast.Block.Sub] = legacy.macroRW
        implicit def rw4: RW[Ast.Block.Text] = legacy.macroRW
        implicit def rw5: RW[Ast.Block.For] = legacy.macroRW
        implicit def rw6: RW[Ast.Block.IfElse] = legacy.macroRW
        implicit def rw7: RW[Ast.Header] = legacy.macroRW
        implicit def rw8: RW[Ast.Chain] = legacy.macroRW
        implicit def rw9: RW[Ast.Chain.Sub] = legacy.macroRW
        implicit def rw10: RW[Ast.Chain.Prop] = legacy.macroRW
        implicit def rw11: RW[Ast.Chain.TypeArgs] = legacy.macroRW
        implicit def rw12: RW[Ast.Chain.Args] = legacy.macroRW
        val block = Ast.Block(1, Seq(Ast.Block.Text(2, "hello")))
        val blockText = """[
          "com.rallyhealth.upickle.v1.Ast.Block",
          {
            "offset":1,
            "parts":[
              [
                "com.rallyhealth.upickle.v1.Ast.Block.Text",
                {
                  "offset":2,
                  "txt":"hello"
                }
              ]
            ]
          }
        ]"""
        rw(block: Ast, blockText)
        rw(block: Ast.Block, blockText)
        rw(block: Ast.Block.Sub, blockText)
        rw(block: Ast.Chain.Sub, blockText)

        val header = Ast.Header(0, "Hello", block)
        val headerText = s"""[
          "com.rallyhealth.upickle.v1.Ast.Header",
          {
            "offset": 0,
            "front": "Hello",
            "block": $blockText
          }
        ]"""
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
  //    test("companionImplicitWorks"){
  //
  //      rw(TypedFoo.Bar(1): TypedFoo, """{"$type": "com.rallyhealth.upickle.v1.TypedFoo.Bar", "i": 1}""")
  //      rw(TypedFoo.Baz("lol"): TypedFoo, """{"$type": "com.rallyhealth.upickle.v1.TypedFoo.Baz", "s": "lol"}""")
  //      rw(TypedFoo.Quz(true): TypedFoo, """{"$type": "com.rallyhealth.upickle.v1.TypedFoo.Quz", "b": true}""")
  //    }
    }
    test("jsonInCaseClass"){

      implicit def arw: RW[CaseClassWithJson] = com.rallyhealth.upickle.v1.legacy.macroRW
      rw(new CaseClassWithJson(com.rallyhealth.ujson.v1.Num(7)), """{"json":7}""")
      rw(new CaseClassWithJson(com.rallyhealth.ujson.v1.Arr(com.rallyhealth.ujson.v1.Num(7), com.rallyhealth.ujson.v1.Str("lol"))), """{"json":[7,"lol"]}""")
    }
    test("traitFromOtherPackage"){
      implicit val BaseRW: RW[subpackage.Base] = com.rallyhealth.upickle.v1.legacy.macroRW
      implicit val WrapperRW: RW[subpackage.Wrapper] = com.rallyhealth.upickle.v1.legacy.macroRW
      com.rallyhealth.upickle.v1.legacy.write(subpackage.Wrapper(subpackage.Base.Child))
    }
  }
}
