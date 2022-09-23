package peschke.testing

import cats.Show
import cats.data.Validated
import cats.effect.IO
import cats.effect.unsafe.IORuntime
import cats.syntax.all._
import sourcecode.Text

import java.util.concurrent.TimeoutException
import scala.concurrent.Await
import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration
import scala.util.Failure
import scala.util.Success
import scala.util.Try

/** A typeclass that encodes the ability to extract a concrete value from an
  * effect
  *
  * This isn't something that's normally a good idea in application code,
  * however in tests it can be very handy to extract a value or fail the test
  * gracefully if one is not available.
  *
  * @tparam F
  *   the effect type
  */
trait ValueExtractor[F[_]] {

  /** Extract a value of type `A` from inside an effect `F[_]`
    * @param fa
    *   the wrapped value, with source information. The conversion from `F[A]`
    *   to `Text[F[A]]` is handled by macros, and should be transparent
    * @param at
    *   the location `valueOf` is being called, which will be reported to the
    *   test framework in the case of failure
    * @param fail
    *   the test framework failure capability
    * @param Loc
    *   compatibility layer between [[At]] and `Loc`
    * @tparam A
    *   the type of the wrapped value
    * @tparam Loc
    *   the type the test framework uses to track failure locations
    */
  def valueOf[A, Loc](fa:          sourcecode.Text[F[A]])
                     (implicit at: At, fail: Fail[Loc], Loc: At.To[Loc])
    : A
}

object ValueExtractor {
  def apply[F[_]](implicit VE: ValueExtractor[F]): VE.type = VE

  /** Configuration for how long to wait before giving up on types like
    * [[scala.concurrent.Future]] or [[cats.effect.IO]]
    * @param primary
    *   This is the timeout for the value itself
    * @param finalization
    *   In the case where the operation needs to be canceled, this is how long
    *   we'll wait for this to complete. If `F` cannot be canceled, this is
    *   taken as extra time for completion
    */
  final case class Timeouts
    (primary: FiniteDuration, finalization: FiniteDuration) {
    def total: FiniteDuration = primary + finalization
  }

  /** A mix-in base trait for creating mixins specific to each framework.
    *
    * The recommended pattern is to create a mixin trait that extends [[Syntax]]
    * and provides the needed implicits for a particular `Loc`.
    *
    * @tparam Loc
    *   the type the test framework uses to track failure locations
    */
  trait Syntax[Loc] {
    def valueOf[F[_], A](fa: sourcecode.Text[F[A]])
                        (
                            implicit VE: ValueExtractor[F],
                            path:        sourcecode.File,
                            file:        sourcecode.FileName,
                            line:        sourcecode.Line,
                            fail:        Fail[Loc],
                            loc:         At.To[Loc]
      )
      : A =
      VE.valueOf(fa)(At.here, fail, loc)
  }

  implicit val optionCanExtractValue: ValueExtractor[Option] =
    new ValueExtractor[Option] {
      override def valueOf[A, Loc]
        (fa:          sourcecode.Text[Option[A]])
        (implicit at: At, fail: Fail[Loc], Loc: At.To[Loc])
        : A =
        fa.value.getOrElse(fail("was None", fa, Loc.from(at)))
    }

  implicit def eitherCanExtractValue[L: Show]: ValueExtractor[Either[L, *]] =
    new ValueExtractor[Either[L, *]] {
      override def valueOf[A, Loc]
        (fa:          sourcecode.Text[Either[L, A]])
        (implicit at: At, fail: Fail[Loc], Loc: At.To[Loc])
        : A =
        fa.value.valueOr { l =>
          fail(show"was a Left($l) rather than a Right(_)", fa, Loc.from(at))
        }
    }

  implicit def validatedCanExtractValue[L: Show]
    : ValueExtractor[Validated[L, *]] =
    new ValueExtractor[Validated[L, *]] {
      override def valueOf[A, Loc]
        (fa:          Text[Validated[L, A]])
        (implicit at: At, fail: Fail[Loc], Loc: At.To[Loc])
        : A =
        fa.value.valueOr { l =>
          fail(show"was an Invalid($l) rather than a Valid(_)", fa, Loc.from(at))
        }
    }

  implicit val tryCanExtractValue: ValueExtractor[Try] =
    new ValueExtractor[Try] {
      override def valueOf[A, Loc]
        (fa: Text[Try[A]])(implicit at: At, fail: Fail[Loc], Loc: At.To[Loc])
        : A =
        fa.value match {
          case Failure(ex) =>
            fail(
              s"was a Failure(${ex.getClass.getSimpleName}: ${ex.getMessage}) rather than a Success(_)",
              fa,
              ex,
              Loc.from(at)
            )
          case Success(value) => value
        }
    }

  implicit def IOCanExtractValue
    (implicit timeouts: ValueExtractor.Timeouts, runtime: IORuntime)
    : ValueExtractor[IO] =
    new ValueExtractor[IO] {
      override def valueOf[A, Loc]
        (fa:          sourcecode.Text[IO[A]])
        (implicit at: At, fail: Fail[Loc], Loc: At.To[Loc])
        : A = {
        val limit = timeouts.primary
        val total = timeouts.total
        Either
          .catchNonFatal {
            fa.value
              .timeout(limit)
              .unsafeRunTimed(total)
              .toRight(
                s"timed out while running finalizers, after $total" -> none[
                  Throwable
                ]
              )
          }
          .leftMap {
            case _: TimeoutException =>
              s"timed out after $limit" -> none[Throwable]
            case ex =>
              s"failed (${ex.getClass.getSimpleName}: ${ex.getMessage})" -> ex.some
          }
          .flatMap(identity)
          .valueOr {
            case (msg, Some(cause)) => fail(msg, fa, cause, Loc.from(at))
            case (msg, None)        => fail(msg, fa, Loc.from(at))
          }
      }
    }

  implicit def FutureCanExtractValue(implicit timeouts: ValueExtractor.Timeouts)
    : ValueExtractor[Future] =
    new ValueExtractor[Future] {
      override def valueOf[A, Loc]
        (fa:          sourcecode.Text[Future[A]])
        (implicit at: At, fail: Fail[Loc], Loc: At.To[Loc])
        : A = {
        val limit = timeouts.total

        def futureTimedOut: (String, Option[Throwable]) =
          s"timed out after $limit" -> none[Throwable]

        def futureFailed(ex: Throwable): (String, Option[Throwable]) =
          s"failed (${ex.getClass.getSimpleName}: ${ex.getMessage})" -> ex.some

        Either
          .catchNonFatal {
            Await
              .ready(fa.value, limit)
              .value
              .toRight(futureTimedOut)
              .flatMap(_.toEither.leftMap(futureFailed))
          }
          .leftMap {
            case _: TimeoutException | _: InterruptedException => futureTimedOut
            case ex => futureFailed(ex)
          }
          .flatMap(identity)
          .valueOr {
            case (msg, Some(cause)) => fail(msg, fa, cause, Loc.from(at))
            case (msg, None)        => fail(msg, fa, Loc.from(at))
          }
      }
    }
}
