package com.rallyhealth.weejson.v1.xml

import com.rallyhealth.weejson.v1.{Obj, Str, Value}
import org.scalactic.TypeCheckedTripleEquals
import org.scalatest.Inside
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

class XmlTests extends AnyFreeSpec with Matchers with Inside with TypeCheckedTripleEquals {

  "tests" - {
    val ugly =
      """<root>
        |    <element0>JSON Test Pattern pass1</element0>
        |    <element1>
        |        <objectWithOneMember>array with 1 element</objectWithOneMember>
        |    </element1>
        |    <element2/>
        |    <element3>-42</element3>
        |    <element4>true</element4>
        |    <element5>false</element5>
        |    <element6/>
        |    <element7>
        |        <integer>1234567890</integer>
        |        <real>-9876.54321</real>
        |        <e>1.23456789E-13</e>
        |        <E>1.23456789E34</E>
        |        <mustNotBeBlank>2.3456789012E76</mustNotBeBlank>
        |        <zero>0</zero>
        |        <one>1</one>
        |        <space></space>
        |        <quote>"</quote>
        |        <backslash>\</backslash>
        |        <controls>
        |            &#xd;
        |        </controls>
        |        <slash>/ &amp; /</slash>
        |        <alpha>abcdefghijklmnopqrstuvwyz</alpha>
        |        <ALPHA>ABCDEFGHIJKLMNOPQRSTUVWYZ</ALPHA>
        |        <digit>0123456789</digit>
        |        <mustStartWithAlpha0123456789>digit</mustStartWithAlpha0123456789>
        |        <special>`1~!@#$%^&amp;*()_+-={':[,]}|;.&lt;/>?</special>
        |        <hex>ģ䕧覫췯ꯍ</hex>
        |        <true>true</true>
        |        <false>false</false>
        |        <null/>
        |        <object/>
        |        <address>50 St. James Street</address>
        |        <url>http://www.JSON.org/</url>
        |        <comment>// /* &lt;!-- -- # -- --> */</comment>
        |        <compact>
        |            <compact1>1</compact1>
        |            <compact2>2</compact2>
        |            <compact3>3</compact3>
        |            <compact4>4</compact4>
        |            <compact5>5</compact5>
        |            <compact6>6</compact6>
        |            <compact7>7</compact7>
        |        </compact>
        |        <jsontext>{"object with 1 member":["array with 1 element"]}</jsontext>
        |        <xmltext><![CDATA[<obj><objectWithOneMember>"array with 1 element"</objectWithOneMember></obj>]]></xmltext>
        |        <quotes>&amp;#34; " %22 0x22 034 &amp;#x22;</quotes>
        |        <쫾_몾-볚1>A key can be _almost_ any string</쫾_몾-볚1>
        |    </element7>
        |    <element8>0.5</element8>
        |    <element9>98.6</element9>
        |    <element10>99.44</element10>
        |    <element11>1066</element11>
        |    <element12>10.0</element12>
        |    <element13>1.0</element13>
        |    <element14>0.1</element14>
        |    <element15>1.0</element15>
        |    <element16>2.0</element16>
        |    <element17>2.0</element17>
        |    <element18>rosebud</element18>
        |</root>
      """.stripMargin

    def readXml(s: String): Value = FromXml(s"<root>$s</root>").transform(Value)

    val parsed = FromXml(ugly).transform(Value)

    "correctness" in {
      val unparsed = parsed.transform(ToXml.string)
      val reparsed = FromXml(unparsed).transform(Value)
      for (json <- Seq(parsed, reparsed)) {
        assertResult("JSON Test Pattern pass1")(json("element0").value)
        assertResult(-9876.54321)(json("element7")("real").str.toDouble) // everything's a string
        assertResult("// /* <!-- -- # -- --> */")(json("element7")("comment").value)
        assertResult("{\"object with 1 member\":[\"array with 1 element\"]}")(json("element7")("jsontext").value)
        assertResult("<obj><objectWithOneMember>\"array with 1 element\"</objectWithOneMember></obj>")(json("element7")("xmltext").value)
        assertResult("rosebud")(json("element18").value)
      }
      assert(parsed("element18") == reparsed("element18"))
    }
    "inputs" in {
      val unparsed = parsed.transform(ToXml.string)
      val fromString = FromXml(unparsed).transform(Value)
      val fromBytes = FromXml(unparsed.getBytes).transform(Value)
      val fromInputStream = FromXml(new java.io.ByteArrayInputStream(unparsed.getBytes)).transform(Value)

      assert(fromString == fromBytes)
      assert(fromBytes == fromInputStream)
    }
    "shortcuts" - {
      "positive" in {
        assertResult(Map("a" -> Str("1")))(readXml("<a>1</a>").obj) // everything's a string
        assertResult(1)(readXml("<a>1</a>")("a").str.toInt) // everything's a string
      }
      "int" in {
        val x = readXml("1")
        assert(x === Obj("" -> Str("1"))) // different than weexml 1.8 (jackson-dataformat-xml 2.13 -> 2.15)
        val y = Str("1").transform(ToXml.string)
        assert(y == "<root>1</root>")
        val v = FromXml(y).transform(Value)
        assert(v === Obj("" -> Str("1"))) // different than weexml 1.8 (jackson-dataformat-xml 2.13 -> 2.15)
      }
      "string" in {
        val x = readXml("\"1\"") // parses as object with nameless attribute in {"":"\"1\""}
        assert(x === Obj("" -> Str("\"1\""))) // different than weexml 1.8 (jackson-dataformat-xml 2.13 -> 2.15)
        val y = Str("\"1\"").transform(ToXml.string)
        assert(y === "<root>\"1\"</root>")
        val v2 = FromXml(y).transform(Value)
        assert(v2 === Obj("" -> Str("\"1\""))) // different than weexml 1.8 (jackson-dataformat-xml 2.13 -> 2.15)
      }
    }
  }
}
