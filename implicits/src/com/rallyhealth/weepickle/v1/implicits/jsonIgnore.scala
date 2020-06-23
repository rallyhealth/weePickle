package com.rallyhealth.weepickle.v1.implicits

import scala.annotation.StaticAnnotation

/**
 * Used to ignore a certain case class field at serialization.
 *
 * Inspired by Jackson:
 * https://fasterxml.github.io/jackson-annotations/javadoc/2.5/com/fasterxml/jackson/annotation/JsonIgnore.html
 */
class jsonIgnore extends StaticAnnotation
