package com.rallyhealth.weejson.v1.yaml

import com.rallyhealth.weejson.v1.jackson.{FromJson, ToPrettyJson}

object TestApp extends App {
//
//  val yaml = """---
//               |# A list of tasty fruits
//               |- Apple
//               |- Orange
//               |- Strawberry
//               |- Mango
//               |...""".stripMargin

  val yaml = """# Employee records
                |-  martin:
                |    name: Martin D'vloper
                |    job: Developer
                |    skills:
                |      - python
                |      - perl
                |      - pascal
                |-  tabitha:
                |    name: Tabitha Bitumen
                |    job: Developer
                |    skills:
                |      - lisp
                |      - fortran
                |      - erlang""".stripMargin

  val json = FromYaml(yaml).transform(ToPrettyJson.string)
  println(json)

  val yamlAgain: String = FromJson(json).transform(ToYaml.string)
  println(yamlAgain)

}
