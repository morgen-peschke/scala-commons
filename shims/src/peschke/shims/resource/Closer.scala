package peschke.shims.resource

import cats.ApplicativeThrow
import cats.syntax.all._
import peschke.Complete

import java.io.Closeable

/** Close a resource, which can potentially fail.
  *
  * Most of the default instances of this will need a [[Convertible]] in the
  * implicit scope that can lift a [[Throwable]] into `F`
  */
trait Closer[F[_], R] {
  def close(resource: R): F[Complete]
}

trait CloserJavaInstances {
  implicit def closerForJavaAutoClosable[F[_], J <: AutoCloseable]
    (implicit AT: ApplicativeThrow[F])
    : Closer[F, J] = Closer.wrappingSideEffect(_.close())
}

trait CloserJavaIOInstances extends CloserJavaInstances {
  implicit def closerForJavaClosable[F[_], J <: Closeable]
    (implicit AT: ApplicativeThrow[F])
    : Closer[F, J] = Closer.wrappingSideEffect(_.close())
}

object Closer extends CloserJavaIOInstances {
  def apply[F[_], R](implicit C: Closer[F, R]): C.type = C

  def wrappingSideEffect[F[_], R](close:       R => Unit)
                                 (implicit AT: ApplicativeThrow[F])
    : Closer[F, R] = resource => AT.catchNonFatal(close(resource)).as(Complete)
}
