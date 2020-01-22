package com.rallyhealth.weejson.v1.jackson

import com.fasterxml.jackson.core.JsonGenerator
import com.rallyhealth.weepickle.v1.core.Visitor

import scala.util.Try

object JsonRenderer {

  /**
    * Generates a single json value (e.g. `{"a": 1, "b": 2}`, `[1,2,3]`, `"pony"`, or `42`),
    * then closes, releasing underlying buffers back to the pool and preventing further writes.
    *
    * This is the one you want 99% of the time.
    *
    * ==Configuration==
    * - [[com.fasterxml.jackson.core.json.JsonWriteFeature]]
    * - [[JsonGenerator.Feature]]
    */
  def apply(generator: JsonGenerator): Visitor[Any, JsonGenerator] = {
    underlying(generator)
      .map { gen =>
        Try(gen.close())
        gen
      }
  }

  /**
    * Lowest level Jackson interface.
    *
    * ==close()==
    * The caller is responsible for calling .close(). Otherwise, data may not be
    * flushed, buffers will not be released back to the pool, performance may
    * suffer due to buffer reallocation.
    *
    * ==Multiple values==
    * Can generates multiple json values (e.g. `{} "pony" 42`) separated by
    * [[com.fasterxml.jackson.core.PrettyPrinter.DEFAULT_ROOT_VALUE_SEPARATOR]].
    *
    * Underlying resources is flushed() at the end of each value.
    */
  def underlying(jsonGenerator: JsonGenerator): Visitor[Any, JsonGenerator] = {
    new JsonGeneratorVisitor(jsonGenerator).map { gen =>
      gen.flush()
      gen
    }
  }
}
