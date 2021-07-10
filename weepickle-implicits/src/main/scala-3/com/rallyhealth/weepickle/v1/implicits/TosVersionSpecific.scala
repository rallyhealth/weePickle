package com.rallyhealth.weepickle.v1.implicits

import com.rallyhealth.weepickle.v1.core.{ Visitor, ObjVisitor, Annotator }

import deriving._, compiletime._

trait TosVersionSpecific extends CaseClassToPiece:
  this: com.rallyhealth.weepickle.v1.core.Types with Tos with Annotator =>
end TosVersionSpecific
