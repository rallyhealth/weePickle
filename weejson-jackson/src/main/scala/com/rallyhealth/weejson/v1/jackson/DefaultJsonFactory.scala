package com.rallyhealth.weejson.v1.jackson

import com.fasterxml.jackson.core.JsonFactory

object DefaultJsonFactory {

  final val Instance = new JsonFactory() // javadoc suggests reusing this
}
