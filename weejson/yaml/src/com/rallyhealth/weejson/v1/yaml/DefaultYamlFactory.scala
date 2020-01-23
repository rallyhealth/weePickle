package com.rallyhealth.weejson.v1.yaml

import com.fasterxml.jackson.dataformat.yaml.YAMLFactory

object DefaultYamlFactory {

  val Instance = new YAMLFactory() // javadoc suggests reusing
}
