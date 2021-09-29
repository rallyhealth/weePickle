package com.rallyhealth.weepickle.v1.laws

import com.rallyhealth.weejson.v1.{GenValue, Value}
import com.rallyhealth.weepickle.v1.core.NullVisitor
import org.scalacheck.Prop.forAll
import utest._

import java.time.Instant

object LawsVisitorTests
  extends utest.TestSuite
  with UTestScalaCheck
  with GenValue {

  type E = IllegalStateException

  override val tests: Tests = Tests {

    "positive" - {
      "Value" - check {
        forAll { (v: Value) =>
          v.transform(new LawsVisitor(Value)) ==> v
        }
      }
    }

    "negative" - {
      val parent = new LawsVisitor(NullVisitor)

      "close no value" - intercept[E](parent.close())

      "close" - {
        parent.visitTrue()
        parent.close()

        "close" - intercept[E](parent.close())
        "visitObj" - intercept[E](parent.visitObject(-1))
        "visitArr" - intercept[E](parent.visitArray(-1))
        "visitNull" - intercept[E](parent.visitNull())
        "visitTrue" - intercept[E](parent.visitTrue())
        "visitFalse" - intercept[E](parent.visitFalse())
        "visitString" - intercept[E](parent.visitString(""))
        "visitFloat64StringParts" - intercept[E](
          parent.visitFloat64StringParts("", 1, 1)
        )
        "visitFloat64" - intercept[E](parent.visitFloat64(1.1))
        "visitFloat32" - intercept[E](parent.visitFloat32(1.1f))
        "visitInt32" - intercept[E](parent.visitInt32(1))
        "visitInt64" - intercept[E](parent.visitInt64(1))
        "visitUInt64" - intercept[E](parent.visitUInt64(1))
        "visitFloat64String" - intercept[E](parent.visitFloat64String(""))
        "visitChar" - intercept[E](parent.visitChar('c'))
        "visitBinary" - intercept[E](
          parent.visitBinary("pony".getBytes(), 0, 4)
        )
        "visitExt" - intercept[E](
          parent.visitExt(0, Array.emptyByteArray, 0, 0)
        )
        "visitTimestamp" - intercept[E](parent.visitTimestamp(Instant.now()))
      }

      "visitObj" - {
        val obj = parent.visitObject(-1).narrow

        "parent" - {
          "visitTrue" - intercept[E](parent.visitTrue())
          "visitObj" - intercept[E](parent.visitObject(-1))
          "visitArr" - intercept[E](parent.visitArray(-1))
          "close" - intercept[E](parent.close())
        }

        "visitKeyValue" - intercept[E](obj.visitKeyValue(""))
        "subVisitor" - intercept[E](obj.subVisitor)
        "visitValue" - intercept[E](obj.visitValue(""))

        "visitKey" - {
          val kv = obj.visitKey()

          "twice" - intercept[E](obj.visitKey())
          "visitKeyValue" - intercept[E](obj.visitKeyValue(""))
          "kv.visitFalse" - {
            kv.visitFalse()

            "twice" - intercept[E](kv.visitFalse())
            "subVisitor" - intercept[E](obj.subVisitor)
            "visitValue" - intercept[E](obj.visitValue(""))
            "visitKeyValue" - {
              obj.visitKeyValue(false)

              "visitKey" - intercept[E](obj.visitKey())
              "visitKeyValue" - intercept[E](obj.visitKeyValue(""))
              "visitValue" - intercept[E](obj.visitValue(""))
              "visitEnd" - intercept[E](obj.visitEnd())
              "subVisitor" - {
                val sub = obj.subVisitor

                "visitKey" - intercept[E](obj.visitKey())
                "visitKeyValue" - intercept[E](obj.visitKeyValue(""))
                "subVisitor" - intercept[E](obj.subVisitor)
                "visitValue" - intercept[E](obj.visitValue(""))
                "visitEnd" - intercept[E](obj.visitEnd())
                "visitTrue" - {
                  sub.visitTrue()

                  "visitKey" - intercept[E](obj.visitKey())
                  "visitKeyValue" - intercept[E](obj.visitKeyValue(""))
                  "subVisitor" - intercept[E](obj.subVisitor)
                  "visitEnd" - intercept[E](obj.visitEnd())
                  "visitValue" - {
                    obj.visitValue(true)

                    "visitKeyValue" - intercept[E](obj.visitKeyValue(""))
                    "subVisitor" - intercept[E](obj.subVisitor)
                    "parent" - intercept[E](parent.visitTrue())
                    "visitEnd" - {
                      obj.visitEnd()

                      "visitEnd" - intercept[E](obj.visitEnd())
                      "visitKey" - intercept[E](obj.visitKey())
                      "visitKeyValue" - intercept[E](obj.visitKeyValue(""))
                      "subVisitor" - intercept[E](obj.subVisitor)
                      "visitValue" - intercept[E](obj.visitValue(""))
                    }
                  }
                }
              }
            }
          }
        }
      }

      "visitArr" - {
        val arr = parent.visitArray(-1).narrow

        "parent" - {
          "visitTrue" - intercept[E](parent.visitTrue())
          "visitObj" - intercept[E](parent.visitObject(-1))
          "visitArr" - intercept[E](parent.visitArray(-1))
          "close" - intercept[E](parent.close())
        }

        "subVisitor" - {
          val sub1 = arr.subVisitor

          "subVisitor" - intercept[E](arr.subVisitor)
          "visitValue" - intercept[E](arr.visitValue(""))
          "visitInt" - {
            sub1.visitInt32(42)

            "subVisitor" - intercept[E](arr.subVisitor)
            "visitEnd" - intercept[E](arr.visitEnd())
            "visitValue" - {
              arr.visitValue(42)

              "parent" - intercept[E](parent.visitTrue())
              "visitEnd" - {
                arr.visitEnd()
              }

              "subVisitor" - {
                val sub2 = arr.subVisitor

                "sub1" - intercept[E](sub1.visitTrue())
                "subVisitor" - intercept[E](arr.subVisitor)
                "visitValue" - intercept[E](arr.visitValue(""))
                "visitInt" - {
                  sub2.visitInt32(42)

                  "subVisitor" - intercept[E](arr.subVisitor)
                  "visitEnd" - intercept[E](arr.visitEnd())
                  "visitValue" - {
                    arr.visitValue(42)

                    "parent" - intercept[E](parent.visitTrue())
                    "visitEnd" - {
                      arr.visitEnd()
                    }
                  }
                }
              }
            }
          }
        }
      }
    }
  }
}
