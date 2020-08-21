package com.rallyhealth.weejson.v1.xml

import com.fasterxml.jackson.dataformat.xml.XmlFactory

object DefaultXmlFactory {

  val Instance = new XmlFactory() // javadoc suggests reusing
}
