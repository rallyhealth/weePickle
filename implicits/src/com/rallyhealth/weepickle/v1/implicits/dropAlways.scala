package com.rallyhealth.weepickle.v1.implicits

import scala.annotation.StaticAnnotation

/**
 * Used to ignore a certain case class field at serialization.
 * Supersedes [[dropDefault]] if used together (though it wouldn't make sense to have both, anyway).
 *
 * Inspired by Jackson:
 * https://fasterxml.github.io/jackson-annotations/javadoc/2.5/com/fasterxml/jackson/annotation/JsonIgnore.html
 */
class dropAlways extends StaticAnnotation
