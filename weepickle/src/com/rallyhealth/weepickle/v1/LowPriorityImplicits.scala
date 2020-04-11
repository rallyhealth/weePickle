package com.rallyhealth.weepickle.v1

import java.time.ZonedDateTime

abstract class LowPriorityImplicits
  extends AttributeTagged
    with EnumFromTos {

  implicit val FromZonedDateTime: From[ZonedDateTime] = FromString.comap[ZonedDateTime](_.toString)
  implicit val ToZonedDateTime: To[ZonedDateTime] = new MapStringTo(s => ZonedDateTime.parse(s.toString))
}
