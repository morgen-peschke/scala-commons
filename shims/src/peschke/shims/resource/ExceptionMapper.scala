package peschke.shims.resource

import cats.ApplicativeError
import cats.Id
import cats.syntax.all._

import scala.concurrent.Future
import scala.util.Try
import scala.util.control.NonFatal

/** Something like [[cats.ApplicativeError]], specialized for our particular use case.
  *
  * Specifically: the need to easily adapt a [[Throwable]] into an `E`
  */
trait ExceptionMapper[F[_], E] {
  def catchNonFatal[A](a: => A): F[A]
  def catchNonFatalF[A](fa: => F[A]): F[A]
}
object ExceptionMapper         {
  def apply[F[_], E](implicit EM: ExceptionMapper[F, E]): EM.type = EM

  def usingApplicativeError[F[_], E]
    (mapErrorIfPossible: Throwable => Option[E])
    (implicit AE:        ApplicativeError[F, E])
    : ExceptionMapper[F, E] =
    new ExceptionMapper[F, E] {
      override def catchNonFatal[A](a: => A): F[A] = catchNonFatalF(a.pure[F])

      override def catchNonFatalF[A](fa: => F[A]): F[A] =
        try fa
        catch {
          case NonFatal(ex) =>
            mapErrorIfPossible(ex).map(AE.raiseError[A]).getOrElse(throw ex) // scalafix:ok DisableSyntax.throw
        }
    }

  implicit val doNothingForId: ExceptionMapper[Id, Throwable] =
    new ExceptionMapper[Id, Throwable] {
      override def catchNonFatal[A](a: => A): Id[A] = a

      override def catchNonFatalF[A](fa: => Id[A]): Id[A] = fa
    }

  implicit val doNothingForTry: ExceptionMapper[Try, Throwable] =
    new ExceptionMapper[Try, Throwable] {
      override def catchNonFatal[A](a: => A): Try[A] = Try(a)

      override def catchNonFatalF[A](fa: => Try[A]): Try[A] = Try(fa).flatten
    }

  implicit val doNothingForFuture: ExceptionMapper[Future, Throwable] =
    new ExceptionMapper[Future, Throwable] {
      override def catchNonFatal[A](a: => A): Future[A] = Future.fromTry(Try(a))

      override def catchNonFatalF[A](fa: => Future[A]): Future[A] =
        Future.fromTry(Try(fa)).flatten
    }
}
