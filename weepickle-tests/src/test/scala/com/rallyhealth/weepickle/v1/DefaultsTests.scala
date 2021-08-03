package com.rallyhealth.weepickle.v1

import com.rallyhealth.weejson.v1.jackson.{FromJson, ToJson}
import com.rallyhealth.weejson.v1.{Num, Obj, Value}
import com.rallyhealth.weepickle.v1.WeePickle.{FromScala, ToScala, ToValue}
import com.rallyhealth.weepickle.v1.core.FromInput
import com.rallyhealth.weepickle.v1.implicits.dropDefault
import utest._

object IntDefaultTests extends TestSuite {

  case class A(a: Int = 0)
  implicit val pickler: WeePickle.FromTo[A] = WeePickle.macroFromTo[A]

  override val tests: Tests = Tests {
    test("write default")(FromScala(A()).transform(ToJson.string) ==> """{"a":0}""")
    test("write non-default")(FromScala(A(1)).transform(ToJson.string) ==> """{"a":1}""")
    test("read missing")(FromJson("""{}""").transform(ToScala[A]) ==> A())
    test("read present")(FromJson("""{"a":1}""").transform(ToScala[A]) ==> A(1))
  }
}

object FieldDropDefaultIntDefaultTests extends TestSuite {

  case class A(@dropDefault a: Int = 0)
  implicit val pickler: WeePickle.FromTo[A] = WeePickle.macroFromTo[A]

  override val tests: Tests = Tests {
    test("write default")(FromScala(A()).transform(ToJson.string) ==> """{}""")
    test("write non-default")(FromScala(A(1)).transform(ToJson.string) ==> """{"a":1}""")
    test("read missing")(FromJson("""{}""").transform(ToScala[A]) ==> A())
    test("read present")(FromJson("""{"a":1}""").transform(ToScala[A]) ==> A(1))
  }
}

object TopDropDefaultIntDefaultTests extends TestSuite {

  @dropDefault case class A(a: Int = 0)
  implicit val pickler: WeePickle.FromTo[A] = WeePickle.macroFromTo[A]

  override val tests: Tests = Tests {
    test("write default")(FromScala(A()).transform(ToJson.string) ==> """{}""")
    test("write non-default")(FromScala(A(1)).transform(ToJson.string) ==> """{"a":1}""")
    test("read missing")(FromJson("""{}""").transform(ToScala[A]) ==> A())
    test("read present")(FromJson("""{"a":1}""").transform(ToScala[A]) ==> A(1))
  }
}

object TopDropDefaultIntTests extends TestSuite {

  @dropDefault case class A(a: Int) // @dropDefault is meaningless here.
  implicit val pickler: WeePickle.FromTo[A] = WeePickle.macroFromTo[A]

  override val tests: Tests = Tests {
    test("write")(FromScala(A(1)).transform(ToJson.string) ==> """{"a":1}""")
    test("read missing")(intercept[Exception](FromJson("""{}""").transform(ToScala[A])))
    test("read present")(FromJson("""{"a":1}""").transform(ToScala[A]) ==> A(1))
  }
}

object StringTests extends TestSuite {

  case class A(d: String)
  implicit val pickler: WeePickle.FromTo[A] = WeePickle.macroFromTo[A]

  override val tests: Tests = Tests {
    test("write non-default")(FromScala(A("omg")).transform(ToJson.string) ==> """{"d":"omg"}""")
    test("write null")(FromScala(A(null)).transform(ToJson.string) ==> """{"d":null}""")
    test("read missing")(intercept[Exception](FromJson("{}").transform(ToScala[A])))
    test("read null")(intercept[Exception](FromJson("""{"d":null}""").transform(ToScala[A])))
  }
}
object StringTopDropDefaultTests extends TestSuite {

  @dropDefault case class A(d: String)
  implicit val pickler: WeePickle.FromTo[A] = WeePickle.macroFromTo[A]

  override val tests: Tests = Tests {
    test("write non-default")(FromScala(A("omg")).transform(ToJson.string) ==> """{"d":"omg"}""")
    test("write null")(FromScala(A(null)).transform(ToJson.string) ==> """{"d":null}""")
    test("read missing")(intercept[Exception](FromJson("{}").transform(ToScala[A])))
    test("read null")(intercept[Exception](FromJson("""{"d":null}""").transform(ToScala[A])))
  }
}

object DefaultStringTopDropDefaultTests extends TestSuite {

  @dropDefault case class A(d: String = "lol")
  implicit val pickler: WeePickle.FromTo[A] = WeePickle.macroFromTo[A]

  override val tests: Tests = Tests {
    test("write default")(FromScala(A()).transform(ToJson.string) ==> """{}""")
    test("write non-default")(FromScala(A("omg")).transform(ToJson.string) ==> """{"d":"omg"}""")
    test("write null")(FromScala(A(null)).transform(ToJson.string) ==> """{"d":null}""")
    test("read missing")(FromJson("""{}""").transform(ToScala[A]) ==> A("lol"))
    test("read present")(FromJson("""{"d":"omg"}""").transform(ToScala[A]) ==> A("omg"))
    test("read null")(intercept[Exception](FromJson("""{"d":null}""").transform(ToScala[A])))
  }
}

object OptionTopDropDefaultTests extends TestSuite {

  @dropDefault case class A(d: Option[String] = None)
  implicit val pickler: WeePickle.FromTo[A] = WeePickle.macroFromTo[A]

  override val tests: Tests = Tests {
    test("write default")(FromScala(A()).transform(ToJson.string) ==> """{}""")
    test("write non-default")(FromScala(A(Some("omg"))).transform(ToJson.string) ==> """{"d":"omg"}""")
    test("write empty")(FromScala(A(None)).transform(ToJson.string) ==> """{}""")
    test("read missing")(FromJson("{}").transform(ToScala[A]) ==> A(None))
    test("read null")(FromJson("""{"d":null}""").transform(ToScala[A]) ==> A(None))
  }
}

// Exercises com.rallyhealth.weepickle.v1.LowPriorityImplicits.FromFromInput
object FromInputStringTests extends TestSuite {

  case class A(d: String)
  implicit val pickler: WeePickle.FromTo[A] = WeePickle.macroFromTo[A]

  override val tests: Tests = Tests {
    test("write default FromInput")(
      {
        val fromInput: FromInput = FromScala(A(null))
        FromScala(fromInput).transform(ToJson.string)
      } ==> """{"d":null}""")
    test("write non-default FromInput")(
      {
        val fromInput: FromInput = FromScala(A("omg"))
        FromScala(fromInput).transform(ToJson.string)
      } ==> """{"d":"omg"}""")
    test("write null FromInput")(
      {
        val fromInput: FromInput = FromScala(A(null))
        FromScala(fromInput).transform(ToJson.string)
      } ==> """{"d":null}""")
  }
}

object ChangingDefaultTests extends TestSuite {

  var currentTimeMillis = 0
  case class Timestamp(@dropDefault ts: Long = currentTimeMillis)
  implicit val pickler: WeePickle.FromTo[Timestamp] = WeePickle.macroFromTo[Timestamp]

  override val tests: Tests = Tests {
    test("from") {
      currentTimeMillis = 0
      val timestamp1 = FromScala(Timestamp(0)).transform(ToValue) ==> Obj()
      currentTimeMillis = 1
      val timestamp2 = FromScala(Timestamp(0)).transform(ToValue) ==> Obj("ts" -> Num(0))
    }

    test("to") {
      currentTimeMillis = 2
      val timestamp1 = FromScala(Obj()).transform(ToScala[Timestamp]).ts ==> 2
      currentTimeMillis = 3
      val timestamp2 = FromScala(Obj()).transform(ToScala[Timestamp]).ts ==> 3
    }
  }
}
