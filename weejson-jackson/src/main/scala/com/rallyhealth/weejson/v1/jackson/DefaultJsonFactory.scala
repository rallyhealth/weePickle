package com.rallyhealth.weejson.v1.jackson

import com.fasterxml.jackson.core.{JsonFactory, JsonFactoryBuilder}

object DefaultJsonFactory {

  final val Instance: JsonFactory = // javadoc suggests reusing this
    new JsonFactoryBuilder()
      .configure(JsonFactory.Feature.INTERN_FIELD_NAMES, false) // vuln to String.hashcode collisions
      .build()
}
