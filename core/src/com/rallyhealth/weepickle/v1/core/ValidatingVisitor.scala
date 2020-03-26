package com.rallyhealth.weepickle.v1.core

import java.time.Instant

import scala.annotation.tailrec
import scala.collection.mutable.ListBuffer
import scala.util.control.NonFatal
import scala.util.{Failure, Success, Try}

/**
  * A slight variation of JsonPointerVisitor, where we
  * add a *sequence* of JSON Pointers to the result of the delegate Visitor
  * rather than throwing a single exception for the first error encountered by the delegate.
  *
  * JSON Pointer is standardized by RFC 6901 and commonly used by JSON Schema.
  *
  * Useful for debugging failures. May add ~10-20% overhead depending on the parser, document size, number of errors, etc..
  *
  * In this microbenchmark parsing a 640 KB, 24 K line Json with two errors it added ~20% (i.e., about 1 ms):
  *
  * [info] BenchTest.play_All     thrpt   20   57.468 ± 1.367  ops/s
  * [info] BenchTest.wee_1_First  thrpt   20  229.404 ± 5.225  ops/s
  * [info] BenchTest.wee_2_All    thrpt   20  184.232 ± 3.377  ops/s
  *
  * (But still much faster than Play)
  *
  * @see https://tools.ietf.org/html/rfc6901
  */
object ValidatingVisitor {

  type Errors = Seq[JsonPointerVisitor.JsonPointerException]

  implicit class Ops[T, J](delegate: Visitor[T, J]) {

    def returningAllErrors: Visitor[T, Either[Errors, J]] = new ValidatingVisitor(delegate, RootHasPath)
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
    def path: String = components.iterator.flatMap(_.pathComponent).mkString("/")

    override def toString: String = path

    def wrap[T](f: => T): Either[Errors, T] = Try(Right(f)) match {
      case Failure(NonFatal(cause)) =>
        Left(Seq(new JsonPointerVisitor.JsonPointerException(path, cause)))

      case Failure(unexpected) =>
        throw unexpected

      case Success(t) => t
    }
  }

  private object RootHasPath extends HasPath {

    override def pathComponent: Option[String] = Some("")

    override def parent: Option[HasPath] = None
  }

}

/*
 * Conceptually similar to Visitor.MapTo, but more careful about catching exceptions
 * (because argument to wrap is by name rather than value)
 */
private class ValidatingVisitor[T, J](
  protected val delegate: Visitor[T, J],
  parentPath: ValidatingVisitor.HasPath
) extends Visitor[T, Either[ValidatingVisitor.Errors, J]] {
  import ValidatingVisitor._

  /*
   * Collects all errors in objects and arrays
   */
  private trait HasMultiplePaths extends HasPath {

    protected def allErrors: ListBuffer[Errors]

    /*
     * v ought to have runtime type Either[Errors, T]
     */
    def wrappedVisitValue(v: Any, visitValue: T => Unit): Unit = v match {
      case Left(e: Errors @unchecked) =>
        allErrors.append(e)
      case Right(b: T @unchecked) =>
        wrap(visitValue(b)) match {
          case Left(e: Errors) =>
            allErrors.append(e)
          case Right(_) => ()
        }
    }

    def wrappedVisitEnd(visitEnd: => J): Either[Errors, J] =
      wrap(visitEnd) match {
        case Right(t) if allErrors.isEmpty => Right(t)
        case Right(_)                      => Left(allErrors.toSeq.flatten)
        // case Left(ex) if allErrors.nonEmpty => Left(allErrors.flatten)
        // may restate other errors, e.g., invalid type leads to field being considered missing too
        case Left(ex) =>
          allErrors += ex
          Left(allErrors.toSeq.flatten)
      }
  }

  /*
   * Using ObjVisitor[Any, Either[Errors, J]] here rather than ObjVisitor[T, Either[Errors, J]]
   * because subVisitor is wrapped, so visitValue winds up receiving Either[Errors, T], and
   * ObjVisitor[Either[Errors, T], Either[Errors, J]] does not conform to required type for
   * visitObject (it is all a bit of a mess with how types are coerced).
   */
  override def visitObject(length: Int): ObjVisitor[T, Either[Errors, J]] =
    new ObjVisitor[Any, Either[Errors, J]] with HasMultiplePaths {
      val objVisitor: ObjVisitor[T, J] = delegate.visitObject(length)

      private var key: String = _
      override val allErrors: ListBuffer[Errors] = new ListBuffer()
      private val keyVisitor: Visitor[_, _] = objVisitor.visitKey()

      override def visitKey(): Visitor[_, _] = new Visitor.Delegate[Nothing, Any](keyVisitor) {
        override def visitString(cs: CharSequence): Any = {
          key = cs.toString
          keyVisitor.visitString(key) // no point wrapping these
        }
      }

      override def visitKeyValue(v: Any): Unit = {
        if (key == null) key = "?"
        objVisitor.visitKeyValue(v) // no point wrapping these either
      }

      override def subVisitor: Visitor[Any, Either[Errors, T]] =
        new ValidatingVisitor(objVisitor.subVisitor.asInstanceOf[Visitor[Any, T]], this)

      override def visitValue(v: Any): Unit = {
        key = null // reset before visitEnd.
        wrappedVisitValue(v, objVisitor.visitValue)
      }

      override def visitEnd(): Either[Errors, J] =
        wrappedVisitEnd(objVisitor.visitEnd())

      override def pathComponent: Option[String] =
        Option(key).map(_.replaceAllLiterally("~", "~0").replaceAllLiterally("/", "~1"))

      override def parent: Option[HasPath] = Some(ValidatingVisitor.this.parentPath)
    }

  override def visitArray(length: Int): ArrVisitor[T, Either[Errors, J]] =
    new ArrVisitor[Any, Either[Errors, J]] with HasMultiplePaths {
      val arrVisitor: ArrVisitor[T, J] = delegate.visitArray(length)
      private var i = -1

      override val allErrors: ListBuffer[Errors] = new ListBuffer()

      override def subVisitor: Visitor[Any, Either[Errors, T]] = {
        i += 1
        new ValidatingVisitor(arrVisitor.subVisitor.asInstanceOf[Visitor[Any, T]], this)
      }

      override def visitValue(v: Any): Unit =
        wrappedVisitValue(v, arrVisitor.visitValue)

      override def visitEnd(): Either[Errors, J] = {
        i = -1
        wrappedVisitEnd(arrVisitor.visitEnd())
      }

      override def pathComponent: Option[String] = if (i >= 0) Some(i.toString) else None

      override def parent: Option[HasPath] = Some(ValidatingVisitor.this.parentPath)
    }

  override def visitNull(): Either[Errors, J] = parentPath.wrap(delegate.visitNull())

  override def visitTrue(): Either[Errors, J] = parentPath.wrap(delegate.visitTrue())

  override def visitFalse(): Either[Errors, J] = parentPath.wrap(delegate.visitFalse())

  override def visitString(cs: CharSequence): Either[Errors, J] = parentPath.wrap(delegate.visitString(cs))

  override def visitFloat64StringParts(cs: CharSequence, decIndex: Int, expIndex: Int): Either[Errors, J] =
    parentPath.wrap(delegate.visitFloat64StringParts(cs, decIndex, expIndex))

  override def visitFloat64(d: Double): Either[Errors, J] = parentPath.wrap(delegate.visitFloat64(d))

  override def visitFloat32(d: Float): Either[Errors, J] = parentPath.wrap(delegate.visitFloat32(d))

  override def visitInt32(i: Int): Either[Errors, J] = parentPath.wrap(delegate.visitInt32(i))

  override def visitInt64(l: Long): Either[Errors, J] = parentPath.wrap(delegate.visitInt64(l))

  override def visitUInt64(ul: Long): Either[Errors, J] = parentPath.wrap(delegate.visitUInt64(ul))

  override def visitFloat64String(s: String): Either[Errors, J] = parentPath.wrap(delegate.visitFloat64String(s))

  override def visitChar(c: Char): Either[Errors, J] = parentPath.wrap(delegate.visitChar(c))

  override def visitBinary(bytes: Array[Byte], offset: Int, len: Int): Either[Errors, J] =
    parentPath.wrap(delegate.visitBinary(bytes, offset, len))

  override def visitExt(tag: Byte, bytes: Array[Byte], offset: Int, len: Int): Either[Errors, J] =
    parentPath.wrap(delegate.visitExt(tag, bytes, offset, len))

  override def visitTimestamp(instant: Instant): Either[Errors, J] =
    parentPath.wrap(delegate.visitTimestamp(instant))

  override def toString: String = parentPath.toString
}
