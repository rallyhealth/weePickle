package com.rallyhealth.weepickle.v1.implicits

import com.rallyhealth.weepickle.v1.core.Annotator

trait FromsVersionSpecific extends CaseClassFromPiece:
  this: com.rallyhealth.weepickle.v1.core.Types with Froms with Annotator =>
end FromsVersionSpecific