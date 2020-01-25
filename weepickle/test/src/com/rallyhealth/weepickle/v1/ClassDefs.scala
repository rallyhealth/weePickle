package com.rallyhealth.weepickle.v1

//import com.rallyhealth.weepickle.v1.ADTs.ADT0
import WeePickle.{FromTo => RW, To => R, From => W}
import com.rallyhealth.weepickle.v1.implicits.key
/*
 * A whole bunch of test data that can be used by client libraries to try out
 * their typeclass derivation to make sure it's doing the right thing. Contains
 * roughly the  whole range of interesting shapes of types supported by com.rallyhealth.weepickle.v1.
 */

object ADTs {
  case class ADT0()
  object ADT0 {
    implicit def rw: RW[ADT0] = WeePickle.macroFromTo
  }
  case class ADTa(i: Int)
  object ADTa {
    implicit def rw: RW[ADTa] = WeePickle.macroFromTo
  }

  case class ADTb(i: Int, s: String)
  object ADTb {
    implicit def rw: RW[ADTb] = WeePickle.macroFromTo
  }
  case class ADTc(i: Int, s: String, t: (Double, Double))
  object ADTc {
    implicit def rw: RW[ADTc] = WeePickle.macroFromTo
  }
  case class ADTd(i: Int, s: String, t: (Double, Double), a: ADTa)
  object ADTd {
    implicit def rw: RW[ADTd] = WeePickle.macroFromTo
  }
  case class ADTe(i: Int, s: String, t: (Double, Double), a: ADTa, q: Seq[Double])
  object ADTe {
    implicit def rw: RW[ADTe] = WeePickle.macroFromTo
  }
  case class ADTf(i: Int, s: String, t: (Double, Double), a: ADTa, q: Seq[Double], o: Option[Option[Boolean]])
  object ADTf {
    implicit def rw: RW[ADTf] = WeePickle.macroFromTo
  }
  case class ADTz(
    t1: Int,
    t2: String,
    t3: Int,
    t4: String,
    t5: Int,
    t6: String,
    t7: Int,
    t8: String,
    t9: Int,
    t10: String,
    t11: Int,
    t12: String,
    t13: Int,
    t14: String,
    t15: Int,
    t16: String,
    t17: Int,
    t18: String
  )
  object ADTz {
    implicit def rw: RW[ADTz] = WeePickle.macroFromTo
  }
}
object Hierarchy {
  sealed trait A
  object A {
    implicit def rw: com.rallyhealth.weepickle.v1.WeePickle.FromTo[A] = RW.merge(B.rw, C.rw)
  }
  case class B(i: Int) extends A
  object B {
    implicit def rw: com.rallyhealth.weepickle.v1.WeePickle.FromTo[B] = WeePickle.macroFromTo
  }
  case class C(s1: String, s2: String) extends A
  object C {
    implicit def rw: com.rallyhealth.weepickle.v1.WeePickle.FromTo[C] = WeePickle.macroFromTo
  }

  object Z {
    implicit def rw: com.rallyhealth.weepickle.v1.WeePickle.FromTo[Z] = RW.merge(
      implicitly[com.rallyhealth.weepickle.v1.WeePickle.FromTo[AnZ.type]]
    )
  }
  sealed trait Z //new line
  case object AnZ extends Z //new line
}

object DeepHierarchy {
  sealed abstract class A
  object A {
    implicit def rw: RW[A] = RW.merge(B.rw, C.rw)
  }
  case class B(i: Int) extends A

  object B {
    implicit def rw: RW[B] = WeePickle.macroFromTo
  }

  sealed trait C extends A

  object C {
    implicit def rw: RW[C] = RW.merge(D.rw, E.rw, F.rw)
  }
  case class D(s: String) extends C

  object D {
    implicit def rw: RW[D] = WeePickle.macroFromTo
  }
  case class E(b: Boolean) extends C

  object E {
    implicit def rw: RW[E] = WeePickle.macroFromTo
  }

  sealed trait Q //new line

  object Q {
    implicit def rw: RW[Q] = RW.merge(AnQ.rw)
  }
  case class AnQ(i: Int) extends Q //new line

  object AnQ {
    implicit def rw: RW[AnQ] = WeePickle.macroFromTo
  }
  case class F(q: Q) extends C //new line

  object F {
    implicit def rw: RW[F] = WeePickle.macroFromTo
  }
}

object Singletons {
  sealed trait AA

  object AA {
    implicit def rw: RW[AA] = RW.merge(
      WeePickle.macroFromTo[BB.type],
      WeePickle.macroFromTo[CC.type]
    )
  }
  case object BB extends AA
  case object CC extends AA

  case object Standalone
}
object Generic {
  case class A[T](t: T)

  object A {
    implicit def rw[T: R: W]: RW[A[T]] = WeePickle.macroFromTo
  }
  case class ADT[A, B, C, D, E, F](a: A, b: B, c: C, d: D, e: E, f: F)
  object ADT {
    implicit def rw[A: R: W, B: R: W, C: R: W, D: R: W, E: R: W, F: R: W]: RW[ADT[A, B, C, D, E, F]] =
      WeePickle.macroFromTo
  }
}
object Recursive {
  sealed trait LL
  object LL {
    implicit def rw: RW[LL] = RW.merge(WeePickle.macroFromTo[End.type], WeePickle.macroFromTo[Node])
  }
  case object End extends LL
  case class Node(c: Int, next: LL) extends LL
  object Node {
    implicit def rw: RW[Node] = WeePickle.macroFromTo
  }
  case class IntTree(value: Int, children: List[IntTree])
  object IntTree {
    implicit def rw: RW[IntTree] = WeePickle.macroFromTo
  }
  sealed trait SingleTree
  object SingleTree {
    implicit def rw: RW[SingleTree] = RW.merge(WeePickle.macroFromTo[SingleNode])
  }
  case class SingleNode(value: Int, children: List[SingleTree]) extends SingleTree
  object SingleNode {
    implicit def rw: RW[SingleNode] = WeePickle.macroFromTo
  }
}
object Annotated {
  sealed trait A
  object A {
    implicit def rw: RW[A] = RW.merge(WeePickle.macroFromTo[B], WeePickle.macroFromTo[C])
  }
  @key("0") case class B(@key("omg") i: Int) extends A
  object B {
    implicit def rw: RW[B] = WeePickle.macroFromTo
  }
  @key("1") case class C(@key("lol") s1: String, @key("wtf") s2: String) extends A
  object C {
    implicit def rw: RW[C] = WeePickle.macroFromTo
  }
}
object Defaults {
  case class ADTa(i: Int = 0)
  object ADTa {
    implicit def rw: RW[ADTa] = WeePickle.macroFromTo
  }
  case class ADTb(i: Int = 1, s: String)
  object ADTb {
    implicit def rw: RW[ADTb] = WeePickle.macroFromTo
  }
  case class ADTc(i: Int = 2, s: String, t: (Double, Double) = (1, 2))
  object ADTc {
    implicit def rw: RW[ADTc] = WeePickle.macroFromTo
  }
}
trait MixedIn {
  trait Trt1 {
    case class ClsA(s: String)
    object ClsA {
      implicit def rw: RW[ClsA] = WeePickle.macroFromTo
    }
  }
  trait Trt2 extends Trt1 {
    case class ClsB(i: Int)
    object ClsB {
      implicit def rw: RW[ClsB] = WeePickle.macroFromTo
    }
  }
  object Obj extends Trt2
}

object MixedIn extends MixedIn

object Varargs {
  case class Sentence(a: String, bs: String*)
  object Sentence {
    implicit def rw: RW[Sentence] = WeePickle.macroFromTo
  }
}
//object Covariant{
//  case class Tree[+T](value: T)
//  object Tree{
//    implicit def rw[T: R: W]: RW[Tree[T]] = WeePickle.macroFromTo
//  }
//}
//
object Exponential {
  case class A1(x: A2, y: A2)
  object A1 {
    implicit def rw: RW[A1] = WeePickle.macroFromTo
  }
  case class A2(x: A3, y: A3)
  object A2 {
    implicit def rw: RW[A2] = WeePickle.macroFromTo
  }
  case class A3(x: A4, y: A4)
  object A3 {
    implicit def rw: RW[A3] = WeePickle.macroFromTo
  }
  case class A4(x: A5, y: A5)
  object A4 {
    implicit def rw: RW[A4] = WeePickle.macroFromTo
  }
  case class A5(x: A6, y: A6)
  object A5 {
    implicit def rw: RW[A5] = WeePickle.macroFromTo
  }
  case class A6(x: A7, y: A7)
  object A6 {
    implicit def rw: RW[A6] = WeePickle.macroFromTo
  }
  case class A7(x: A8, y: A8)
  object A7 {
    implicit def rw: RW[A7] = WeePickle.macroFromTo
  }
  case class A8(x: A9, y: A9)
  object A8 {
    implicit def rw: RW[A8] = WeePickle.macroFromTo
  }
  case class A9(x: A10, y: A10)
  object A9 {
    implicit def rw: RW[A9] = WeePickle.macroFromTo
  }
  case class A10(x: A11, y: A11)
  object A10 {
    implicit def rw: RW[A10] = WeePickle.macroFromTo
  }
  case class A11(x: A12, y: A12)
  object A11 {
    implicit def rw: RW[A11] = WeePickle.macroFromTo
  }
  case class A12(x: A13, y: A13)
  object A12 {
    implicit def rw: RW[A12] = WeePickle.macroFromTo
  }
  case class A13(x: A14, y: A14)
  object A13 {
    implicit def rw: RW[A13] = WeePickle.macroFromTo
  }
  case class A14(x: A15, y: A15)
  object A14 {
    implicit def rw: RW[A14] = WeePickle.macroFromTo
  }
  case class A15(x: A16, y: A16)
  object A15 {
    implicit def rw: RW[A15] = WeePickle.macroFromTo
  }
  case class A16(x: A17, y: A17)
  object A16 {
    implicit def rw: RW[A16] = WeePickle.macroFromTo
  }
  case class A17(x: A18, y: A18)
  object A17 {
    implicit def rw: RW[A17] = WeePickle.macroFromTo
  }
  case class A18()
  object A18 {
    implicit def rw: RW[A18] = WeePickle.macroFromTo
  }
}

object GenericADTs {
  sealed trait Small[A]
  object Small {
    implicit def rw[A: R: W]: RW[Small[A]] = RW.merge(Small1.rw[A])
  }
  case class Small1[A](key: A) extends Small[A]
  object Small1 {
    implicit def rw[A: R: W]: RW[Small1[A]] = WeePickle.macroFromTo
  }

  sealed trait Delta[+A, +B]
  object Delta {
    implicit def rw[A: R: W, B: R: W]: RW[Delta[A, B]] = RW.merge(
      Insert.rw[A, B],
      Remove.rw[A],
      Clear.rw
    )

    case class Insert[A, B](key: A, value: B) extends Delta[A, B]
    object Insert {
      implicit def rw[A: R: W, B: R: W]: RW[Insert[A, B]] = WeePickle.macroFromTo
    }
    case class Remove[A](key: A) extends Delta[A, Nothing]
    object Remove {
      implicit def rw[A: R: W]: RW[Remove[A]] = WeePickle.macroFromTo
    }
    case class Clear() extends Delta[Nothing, Nothing]
    object Clear {
      implicit def rw: RW[Clear] = WeePickle.macroFromTo
    }
  }
  sealed trait DeltaInvariant[A, B]
  object DeltaInvariant {
    implicit def rw[A: R: W, B: R: W]: RW[DeltaInvariant[A, B]] = RW.merge(
      Insert.rw[A, B],
      Remove.rw[A, B],
      Clear.rw[A, B]
    )
    case class Insert[A, B](key: A, value: B) extends DeltaInvariant[A, B]
    object Insert {
      implicit def rw[A: R: W, B: R: W]: RW[Insert[A, B]] = WeePickle.macroFromTo
    }
    case class Remove[A, B](key: A) extends DeltaInvariant[A, B]
    object Remove {
      implicit def rw[A: R: W, B]: RW[Remove[A, B]] = WeePickle.macroFromTo
    }
    case class Clear[A, B]() extends DeltaInvariant[A, B]
    object Clear {
      implicit def rw[A, B]: RW[Clear[A, B]] = WeePickle.macroFromTo
    }
  }
////  Not sure how to make these work...
////
////  sealed trait DeltaHardcoded[A, B]
////  object DeltaHardcoded {
////    implicit def rw[A: R: W, B: R: W]: RW[DeltaHardcoded[A, B]] = WeePickle.macroFromTo
////    case class Insert[A, B](key: A, value: B) extends DeltaHardcoded[A, B]
////    object Insert{
////      implicit def rw[A: R: W, B: R: W]: RW[Insert[A, B]] = WeePickle.macroFromTo
////    }
////    case class Remove[A](key: A) extends DeltaHardcoded[A, String]
////    object Remove{
////      implicit def rw[A: R: W]: RW[Remove[A]] = WeePickle.macroFromTo
////    }
////    case class Clear() extends DeltaHardcoded[Seq[Int], String]
////    object Clear{
////      implicit def rw: RW[Clear] = WeePickle.macroFromTo
////    }
////  }
}
//object Amorphous{
//  class A()
//  class B(i: Int){
//    val x = "lol"
//  }
//}
//// issue #95
//// For some reason this stuff must live top-level; the test fails to
//// go red when the case classes are moved inside a wrapper object even
//// when the fix is backed out
case class C1(name: String, types: List[String])
object C1 {
  implicit def rw: RW[C1] = WeePickle.macroFromTo
}
case class C2(results: List[C1])
object C2 {
  implicit def rw: RW[C2] = WeePickle.macroFromTo
}
case class Result2(name: String, whatever: String, types: List[String])
object Result2 {
  implicit def rw: RW[Result2] = WeePickle.macroFromTo
}

case class GeoCoding2(results: List[Result2], status: String)
object GeoCoding2 {
  implicit def rw: RW[GeoCoding2] = WeePickle.macroFromTo
}

sealed trait Ast {
  def offset: Int
}

/**
  * Sample AST taken from the Scalatex project
  *
  * https://github.com/lihaoyi/Scalatex/
  *
  * It's a use case where each case class inherits from multiple distinct
  * sealed traits, which aren't a strict hierarchy
  */
object Ast {
  implicit def rw: RW[Ast] = RW.merge(Block.rw, Header.rw)

  /**
    * @param parts The various bits of text and other things which make up this block
    * @param offset
    */
  case class Block(offset: Int, parts: Seq[Block.Sub]) extends Chain.Sub with Block.Sub
  object Block {
    implicit def rw: RW[Block] = WeePickle.macroFromTo
    sealed trait Sub extends Ast
    object Sub {
      implicit def rw: RW[Sub] = RW.merge(Text.rw, For.rw, IfElse.rw, Block.rw, Header.rw)
    }
    case class Text(offset: Int, txt: String) extends Block.Sub
    object Text {
      implicit def rw: RW[Text] = WeePickle.macroFromTo
    }
    case class For(offset: Int, generators: String, block: Block) extends Block.Sub
    object For {
      implicit def rw: RW[For] = WeePickle.macroFromTo
    }
    case class IfElse(offset: Int, condition: String, block: Block, elseBlock: Option[Block]) extends Block.Sub
    object IfElse {
      implicit def rw: RW[IfElse] = WeePickle.macroFromTo
    }
  }
  case class Header(offset: Int, front: String, block: Block) extends Block.Sub with Chain.Sub
  object Header {
    implicit def rw: RW[Header] = WeePickle.macroFromTo
  }

  /**
    * @param lhs The first expression in this method-chain
    * @param parts A list of follow-on items chained to the first
    * @param offset
    */
  case class Chain(offset: Int, lhs: String, parts: Seq[Chain.Sub]) extends Block.Sub
  object Chain {
    implicit def rw: RW[Chain] = WeePickle.macroFromTo
    sealed trait Sub extends Ast
    object Sub {
      implicit def rw: RW[Sub] = RW.merge(Prop.rw, TypeArgs.rw, Args.rw, Block.rw, Header.rw)
    }
    case class Prop(offset: Int, str: String) extends Sub
    object Prop {
      implicit def rw: RW[Prop] = WeePickle.macroFromTo
    }
    case class TypeArgs(offset: Int, str: String) extends Sub
    object TypeArgs {
      implicit def rw: RW[TypeArgs] = WeePickle.macroFromTo
    }
    case class Args(offset: Int, str: String) extends Sub
    object Args {
      implicit def rw: RW[Args] = WeePickle.macroFromTo
    }
  }
}

case class CaseClassWithJson(json: com.rallyhealth.weejson.v1.Value)
