package peschke.shims.resource

import cats.Id
import cats.Semigroup
import cats.syntax.all._
import peschke.Complete

import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.util.Failure
import scala.util.Success
import scala.util.Try

/** This is a poor replacement for cats-effect's MonadCancel.
  *
  * If you have access to cats-effect, use `MonadCancel` instead. If you don't,
  * this can serve as a shim.
  */
trait Bracket[F[_]] {

  /** Initialize, use, and release some resource.
    *
    * The resource should not be assumed to be available outside of the scope of
    * `use`
    */
  def bracket[R, A]
    (acquire: () => F[R], use: R => F[A], release: R => F[Complete])
    : F[A]
}
object Bracket {
  def apply[F[_]](implicit B: Bracket[F]): B.type = B

  implicit def bracketFuture(implicit ec: ExecutionContext): Bracket[Future] =
    new Bracket[Future] {
      override def bracket[R, A](
                                  acquire: () => Future[R],
                                  use:     R => Future[A],
                                  release: R => Future[Complete]
      ): Future[A] =
        acquire().flatMap { resource =>
          use(resource).transformWith {
            case Success(value) => release(resource).map(_ => value)
            case Failure(useException) =>
              release(resource)
                .transform {
                  case Failure(releaseException) =>
                    useException.addSuppressed(releaseException)
                    Failure(useException)
                  case Success(_) => Failure(useException)
                }
          }
        }
    }

  implicit val bracketTry: Bracket[Try] =
    new Bracket[Try] {
      override def bracket[R, A](
                                  acquire: () => Try[R],
                                  use:     R => Try[A],
                                  release: R => Try[Complete]
      ): Try[A] =
        acquire().flatMap { resource =>
          use(resource).transform(
            value => release(resource).map(_ => value),
            useException =>
              release(resource)
                .transform(
                  _ => Failure(useException),
                  releaseException => {
                    useException.addSuppressed(releaseException)
                    Failure(useException)
                  }
                )
          )
        }
    }

  implicit val bracketId: Bracket[Id] = new Bracket[Id] {
    override def bracket[R, A](
                                acquire: () => Id[R],
                                use:     R => Id[A],
                                release: R => Id[Complete]
    ): Id[A] =
      bracketTry
        .bracket[R, A](
          () => Try(acquire()),
          r => Try(use(r)),
          r => Try(release(r))
        ).get
  }

  implicit def bracketEither[E]
    (implicit EM: ExceptionMapper[Either[E, *], E], SE: Semigroup[E])
    : Bracket[Either[E, *]] =
    new Bracket[Either[E, *]] {
      override def bracket[R, A](
                                  acquire: () => Either[E, R],
                                  use:     R => Either[E, A],
                                  release: R => Either[E, Complete])
        : Either[E, A] =
        EM.catchNonFatalF(acquire()).flatMap { resource =>
          EM.catchNonFatalF(use(resource)) match {
            case Right(value) =>
              EM.catchNonFatalF(release(resource)).map(_ => value)
            case Left(useException) =>
              EM.catchNonFatalF(release(resource)) match {
                case Right(_) => useException.asLeft
                case Left(releaseException) =>
                  SE.combine(useException, releaseException).asLeft
              }
          }
        }
    }
}
