package com.rallyhealth.weepickle.v1

import com.rallyhealth.weejson.v1.jackson.{FromJson, ToJson}
import com.rallyhealth.weepickle.v1.WeePickle.{FromScala, ToScala}
import com.rallyhealth.weepickle.v1.implicits.dropDefault
import utest._

object IntDefaultTests extends TestSuite {

  case class A(a: Int = 0)
  implicit val pickler = WeePickle.macroFromTo[A]

  override val tests: Tests = Tests {
    test("write default")(FromScala(A()).transform(ToJson.string) ==> """{"a":0}""")
    test("write non-default")(FromScala(A(1)).transform(ToJson.string) ==> """{"a":1}""")
    test("read missing")(FromJson("""{}""").transform(ToScala[A]) ==> A())
    test("read present")(FromJson("""{"a":1}""").transform(ToScala[A]) ==> A(1))
  }
}

object FieldDropDefaultIntDefaultTests extends TestSuite {

  case class A(@dropDefault a: Int = 0)
  implicit val pickler = WeePickle.macroFromTo[A]

  override val tests: Tests = Tests {
    test("write default")(FromScala(A()).transform(ToJson.string) ==> """{}""")
    test("write non-default")(FromScala(A(1)).transform(ToJson.string) ==> """{"a":1}""")
    test("read missing")(FromJson("""{}""").transform(ToScala[A]) ==> A())
    test("read present")(FromJson("""{"a":1}""").transform(ToScala[A]) ==> A(1))
  }
}

object TopDropDefaultIntDefaultTests extends TestSuite {

  @dropDefault case class A(a: Int = 0)
  implicit val pickler = WeePickle.macroFromTo[A]

  override val tests: Tests = Tests {
    test("write default")(FromScala(A()).transform(ToJson.string) ==> """{}""")
    test("write non-default")(FromScala(A(1)).transform(ToJson.string) ==> """{"a":1}""")
    test("read missing")(FromJson("""{}""").transform(ToScala[A]) ==> A())
    test("read present")(FromJson("""{"a":1}""").transform(ToScala[A]) ==> A(1))
  }
}

object TopDropDefaultIntTests extends TestSuite {

  @dropDefault case class A(a: Int) // @dropDefault is meaningless here.
  implicit val pickler = WeePickle.macroFromTo[A]

  override val tests: Tests = Tests {
    test("write")(FromScala(A(1)).transform(ToJson.string) ==> """{"a":1}""")
    test("read missing")(intercept[Exception](FromJson("""{}""").transform(ToScala[A])))
    test("read present")(FromJson("""{"a":1}""").transform(ToScala[A]) ==> A(1))
  }
}

object StringTests extends TestSuite {

  case class A(d: String)
  implicit val pickler = WeePickle.macroFromTo[A]

  override val tests: Tests = Tests {
    test("write default")(FromScala(A(null)).transform(ToJson.string) ==> """{"d":null}""")
    test("write non-default")(FromScala(A("omg")).transform(ToJson.string) ==> """{"d":"omg"}""")
    test("write null")(FromScala(A(null)).transform(ToJson.string) ==> """{"d":null}""")
    test("read missing")(intercept[Exception](FromJson("{}").transform(ToScala[A])))
    test("read null")(FromJson("""{"d":null}""").transform(ToScala[A]) ==> A(null))
  }
}
object StringTopDropDefaultTests extends TestSuite {

  @dropDefault case class A(d: String)
  implicit val pickler = WeePickle.macroFromTo[A]

  override val tests: Tests = Tests {
    test("write non-default")(FromScala(A("omg")).transform(ToJson.string) ==> """{"d":"omg"}""")
    test("write null")(FromScala(A(null)).transform(ToJson.string) ==> """{"d":null}""")
    test("read missing")(intercept[Exception](FromJson("{}").transform(ToScala[A])))
    test("read null")(FromJson("""{"d":null}""").transform(ToScala[A]) ==> A(null))
  }
}

object DefaultStringTopDropDefaultTests extends TestSuite {

  @dropDefault case class A(d: String = "lol")
  implicit val pickler = WeePickle.macroFromTo[A]

  override val tests: Tests = Tests {
    test("write default")(FromScala(A()).transform(ToJson.string) ==> """{}""")
    test("write non-default")(FromScala(A("omg")).transform(ToJson.string) ==> """{"d":"omg"}""")
    test("write null")(FromScala(A(null)).transform(ToJson.string) ==> """{"d":null}""")
    test("read missing")(FromJson("""{}""").transform(ToScala[A]) ==> A("lol"))
    test("read present")(FromJson("""{"d":"omg"}""").transform(ToScala[A]) ==> A("omg"))
    test("read null")(FromJson("""{"d":null}""").transform(ToScala[A]) ==> A(null))
  }
}

object OptionTopDropDefaultTests extends TestSuite {

  @dropDefault case class A(d: Option[String] = None)
  implicit val pickler = WeePickle.macroFromTo[A]

  override val tests: Tests = Tests {
    test("write default")(FromScala(A()).transform(ToJson.string) ==> """{}""")
    test("write non-default")(FromScala(A(Some("omg"))).transform(ToJson.string) ==> """{"d":"omg"}""")
    test("write null")(FromScala(A(null)).transform(ToJson.string) ==> """{"d":null}""")
    test("read missing")(FromJson("{}").transform(ToScala[A]) ==> A(None))
    test("read null")(FromJson("""{"d":null}""").transform(ToScala[A]) ==> A(None))
  }
}

