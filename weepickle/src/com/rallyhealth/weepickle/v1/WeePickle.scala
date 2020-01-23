package com.rallyhealth.weepickle.v1

import com.rallyhealth.weepickle.v1.core.{Transmittable, Visitor}

/**
  * Converters for default scala types.
  * Macros to generate converters for case classes.
  */
object WeePickle extends AttributeTagged {

  object ToScala {

    /**
      * Returns a visitor that can produce an [[Out]].
      *
      * ==Examples==
      * - `FromJson("[1,2,3]").transform(ToScala[Seq[Int]])`
      * - `FromJson("""{"name":"Twilight"}""").transform(ToScala[Pony])`
      */
    def apply[Out](implicit pickleOut: Receiver[Out]): Visitor[_, Out] = pickleOut
  }

  object FromScala {

    /**
      * Pairs the scala data type with a [[Transmitter]]
      * capable of pushing the data through a [[Visitor]].
      *
      * ==Examples==
      * - `FromScala(Seq(1,2,3)).transform(ToJson.string) // [1,2,3]`
      * - `FromScala(pony).transform(ToJson.string) // {"name":"Twilight"}`
      */
    def apply[In](scala: In)(implicit pickleIn: Transmitter[In]): Transmittable = fromScala(scala)
  }

}
