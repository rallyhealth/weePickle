package com.rallyhealth.weejson.v1.xml

import com.rallyhealth.weejson.v1.jackson.{FromJson, ToPrettyJson}

object TestApp extends App {
  //
  //  val xml = """---
  //               |# A list of tasty fruits
  //               |- Apple
  //               |- Orange
  //               |- Strawberry
  //               |- Mango
  //               |...""".stripMargin

  val xml = """<root>
              |  <employee><key>martin</key>
              |    <name>Martin D'vloper</name>
              |    <job>Developer</job>
              |    <skills type="array">
              |      <skill name="python"/>
              |      <skill name="perl"/>
              |      <skill name="pascal"/>
              |    </skills>
              |  </employee>
              |  <employee><key>tabitha</key>
              |    <name>Tabitha Bitumen</name>
              |    <job>Developer</job>
              |    <skills type="array">
              |      <skill name="lisp"/>
              |      <skill name="fortran"/>
              |      <skill name="erlang"/>
              |    </skills>
              |  </employee>
              |</root>
              |""".stripMargin //.replaceAll("\\n", " ")

  println(xml)
  val json = FromXml(xml).transform(ToPrettyJson.string)
  println(json)

  val xmlAgain: String = FromJson(json).transform(ToXml.string)
  println(xmlAgain)

}
