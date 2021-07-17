package com.rallyhealth.weepickle.v1

import utest._

case class MissingPicklers()

/*
 * TODO: Relegated here because, unfortunately, Scala 3 returns
 * something very different (which is also not very helpful):
 *
 *       com.rallyhealth.weepickle.v1.WeePickle.given_To_T[T](
 *             /* missing */
 *               summon[
 *                 deriving.Mirror{
 *                   MirroredType <: Singleton; MirroredMonoType <: Singleton;
 *                     MirroredElemTypes <: Tuple
 *                 }
 *               ]
 *           )
 *
 *   But no implicit values were found that match type deriving.Mirror{
 *         MirroredType <: Singleton; MirroredMonoType <: Singleton;
 *           MirroredElemTypes <: Tuple
 *       }.
 *
 *   The following import might make progress towards fixing the problem:
 *
 *     import com.rallyhealth.weepickle.v1.WeePickle.FromTo.join
 *
 */
object MoreMacroTests extends TestSuite {

  val tests = Tests {
    test("compile errors") {
      compileError("implicitly[WeePickle.To[MissingPicklers]]").msg ==> """Could not find an implicit WeePickle.To[com.rallyhealth.weepickle.v1.MissingPicklers]. Consider adding one with `object com.rallyhealth.weepickle.v1.MissingPicklers { implicit val pickleTo: WeePickle.To[com.rallyhealth.weepickle.v1.MissingPicklers] = macroTo }`"""
      compileError("implicitly[WeePickle.From[MissingPicklers]]").msg ==> """Could not find an implicit WeePickle.From[com.rallyhealth.weepickle.v1.MissingPicklers]. Consider adding one with `object com.rallyhealth.weepickle.v1.MissingPicklers { implicit val pickleFrom: WeePickle.From[com.rallyhealth.weepickle.v1.MissingPicklers] = macroFrom }`"""
      compileError("implicitly[WeePickle.FromTo[MissingPicklers]]").msg ==> """Could not find an implicit WeePickle.FromTo[com.rallyhealth.weepickle.v1.MissingPicklers]. Consider adding one with `object com.rallyhealth.weepickle.v1.MissingPicklers { implicit val pickler: WeePickle.FromTo[com.rallyhealth.weepickle.v1.MissingPicklers] = macroFromTo }`"""
    }
  }
}
