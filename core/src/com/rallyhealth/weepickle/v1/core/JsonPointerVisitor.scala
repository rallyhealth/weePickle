package com.rallyhealth.weepickle.v1.core

import java.util.Base64

import com.rallyhealth.weepickle.v1.core.JsonPointerVisitor._

import scala.annotation.tailrec
import scala.util.control.{NoStackTrace, NonFatal}

/**
  * Adds a JSON Pointer to exceptions thrown by the delegate Visitor.
  *
  * JSON Pointer is standardized by RFC 6901 and commonly used by JSON Schema.
  *
  * Useful for debugging failures.
  * Adds ~10% overhead depending on the parser.
  *
  * @see https://tools.ietf.org/html/rfc6901
  */
object JsonPointerVisitor {

  def apply[T, J](delegate: Visitor[T, J]): Visitor[T, J] = new JsonPointerVisitor(delegate, RootHasPath)

  /**
    * JSON Pointer indicating where the problem occurred.
    * Added as a suppressed exception.
    *
    * @param jsonPointer e.g. "/hits/hits/3/_source/foo/bar"
    * @see https://tools.ietf.org/html/rfc6901
    */
  class JsonPointerException(val jsonPointer: String, cause: Throwable)
      extends Exception(jsonPointer, cause)
      with NoStackTrace {

    override def toString: String = jsonPointer
  }

  /**
    * Internally, the paths form a linked list back to the root by the visitors themselves.
    * Compared to something like a List[String] or List[Object], this does not require
    * extra String allocation or boxing unless we actually ask for the path.
    */
  private trait HasPath {

    /**
      * Forms a chain toward the root.
      */
    def parent: Option[HasPath]

    /**
      * @return name of a single level, if any, e.g. "foo"
      */
    def pathComponent: Option[String]

    /**
      * @return root-to-leaf ordered chain as a List[HasPath]
      */
    private def components: List[HasPath] = {
      // Used rarely. Stack safety > memory efficiency here.
      @tailrec def listPath(o: Option[HasPath], list: List[HasPath]): List[HasPath] = {
        o match {
          case Some(p) => listPath(p.parent, p :: list)
          case None    => list
        }
      }
      listPath(parent, List(this))
    }

    /**
      * @return the full path, slash-delimited.
      */
    def path: String = components.iterator.map(_.pathComponent).flatten.mkString("/")

    override def toString: String = path

    def wrap[T](f: => T): T = {
      try {
        f
      } catch {
        case NonFatal(cause) =>
          /**
            * Can't chain them normally here without upsetting Parser.reject
            * and failing a bunch of the unit tests.
            */
          cause.addSuppressed(new JsonPointerException(path, null))
          throw cause
      }
    }
  }

  private object RootHasPath extends HasPath {

    override def pathComponent: Option[String] = Some("")

    override def parent: Option[HasPath] = None
  }

}

private class JsonPointerVisitor[T, J](
  protected val delegate: Visitor[T, J],
  parentPath: HasPath
) extends Visitor.Delegate[T, J](delegate) {

  override def visitObject(length: Int): ObjVisitor[T, J] = {
    val objVisitor = parentPath.wrap(super.visitObject(length))
    new ObjVisitor[T, J] with HasPath {
      private var key: String = _

      override def visitKey(): Visitor[_, _] = new JsonPointerVisitor[Nothing, Any](objVisitor.visitKey(), this) {
        override def visitString(cs: CharSequence): Any = {
          key = cs.toString
          wrap(this.delegate.visitString(key))
        }

        override def visitInt32(i: Int): Any = {
          key = i.toString
          wrap(this.delegate.visitInt32(i))
        }

        override def visitInt64(l: Long): Any = {
          key = l.toString
          wrap(this.delegate.visitInt64(l))
        }

        override def visitNull(): Any = {
          key = "null"
          wrap(this.delegate.visitNull())
        }

        override def visitTrue(): Any = {
          key = "true"
          wrap(this.delegate.visitTrue())
        }

        override def visitFalse(): Any = {
          key = "false"
          wrap(this.delegate.visitFalse())
        }

        override def visitFloat64StringParts(cs: CharSequence, decIndex: Int, expIndex: Int): Any = {
          key = cs.toString
          wrap(this.delegate.visitFloat64StringParts(cs, decIndex, expIndex))
        }

        override def visitFloat64(d: Double): Any = {
          key = d.toString
          wrap(this.delegate.visitFloat64(d))
        }

        override def visitFloat32(d: Float): Any = {
          key = d.toString
          wrap(this.delegate.visitFloat32(d))
        }

        override def visitUInt64(ul: Long): Any = {
          key = java.lang.Long.toUnsignedString(ul)
          wrap(this.delegate.visitUInt64(ul))
        }

        override def visitFloat64String(s: String): Any = {
          key = s.toString
          wrap(this.delegate.visitFloat64String(s))
        }

        override def visitChar(c: Char): Any = {
          key = c.toString
          wrap(this.delegate.visitChar(c))
        }

        override def visitBinary(bytes: Array[Byte], offset: Int, len: Int): Any = {
          key = {
            val arr = if (offset > 0 || len < bytes.length) {
              bytes.slice(offset, offset + len)
            } else {
              bytes
            }
            Base64.getEncoder.encodeToString(arr)
          }
          wrap(this.delegate.visitBinary(bytes, offset, len))
        }
      }

      override def visitKeyValue(v: Any): Unit = {
        if (key == null) key = "?"
        wrap(objVisitor.visitKeyValue(v))
      }

      override def subVisitor: Visitor[Nothing, Any] = {
        new JsonPointerVisitor(objVisitor.subVisitor.asInstanceOf[Visitor[T, J]], this)
      }

      override def visitValue(v: T): Unit = {
        key = null // reset before visitEnd.
        wrap(objVisitor.visitValue(v))
      }

      override def visitEnd(): J = {
        wrap(objVisitor.visitEnd())
      }

      override def pathComponent: Option[String] =
        Option(key).map(_.replace("~", "~0").replace("/", "~1"))

      override def parent: Option[HasPath] = Some(JsonPointerVisitor.this.parentPath)
    }
  }

  override def visitArray(length: Int): ArrVisitor[T, J] = {
    val arrVisitor = parentPath.wrap(super.visitArray(length))
    new ArrVisitor[T, J] with HasPath {
      private var i = -1

      override def subVisitor: Visitor[Nothing, Any] = {
        i += 1
        new JsonPointerVisitor(arrVisitor.subVisitor.asInstanceOf[Visitor[T, J]], this)
      }

      override def visitValue(v: T): Unit = {
        wrap(arrVisitor.visitValue(v))
      }

      override def visitEnd(): J = {
        i = -1
        wrap(arrVisitor.visitEnd())
      }

      override def pathComponent: Option[String] = if (i >= 0) Some(i.toString) else None

      override def parent: Option[HasPath] = Some(JsonPointerVisitor.this.parentPath)
    }
  }

  override def visitNull(): J = parentPath.wrap(super.visitNull())

  override def visitTrue(): J = parentPath.wrap(super.visitTrue())

  override def visitFalse(): J = parentPath.wrap(super.visitFalse())

  override def visitString(cs: CharSequence): J = parentPath.wrap(super.visitString(cs))

  override def visitFloat64StringParts(cs: CharSequence, decIndex: Int, expIndex: Int): J =
    parentPath.wrap(super.visitFloat64StringParts(cs, decIndex, expIndex))

  override def visitFloat64(d: Double): J = parentPath.wrap(super.visitFloat64(d))

  override def visitFloat32(d: Float): J = parentPath.wrap(super.visitFloat32(d))

  override def visitInt32(i: Int): J = parentPath.wrap(super.visitInt32(i))

  override def visitInt64(l: Long): J = parentPath.wrap(super.visitInt64(l))

  override def visitUInt64(ul: Long): J = parentPath.wrap(super.visitUInt64(ul))

  override def visitFloat64String(s: String): J = parentPath.wrap(super.visitFloat64String(s))

  override def visitChar(c: Char): J = parentPath.wrap(super.visitChar(c))

  override def visitBinary(bytes: Array[Byte], offset: Int, len: Int): J =
    parentPath.wrap(super.visitBinary(bytes, offset, len))

  override def visitExt(tag: Byte, bytes: Array[Byte], offset: Int, len: Int): J =
    parentPath.wrap(super.visitExt(tag, bytes, offset, len))

  override def toString: String = parentPath.toString
}
