package peschke.scalatest

import cats.effect.IO
import cats.effect.unsafe.IORuntime
import cats.syntax.all._
import org.scalatest.exceptions.TestFailedException
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import peschke.testing.At
import peschke.testing.ValueExtractor.Timeouts

import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.concurrent.duration.DurationInt
import scala.concurrent.duration.FiniteDuration
import scala.util.control.NoStackTrace

class SmallError(msg: String) extends IllegalArgumentException(msg) with NoStackTrace

class ScalaTestValueExtractorsTest extends AnyWordSpec with Matchers with ScalaTestValueExtractors {
  implicit private val runtime: IORuntime = IORuntime.global
  implicit private val executionContext: ExecutionContext =
    ExecutionContext.global

  private val rightAway = 1.seconds
  private val quick = 2.seconds
  implicit val timeouts: Timeouts = Timeouts(rightAway, rightAway)

  private def path(implicit loc: At): String = loc.path.value

  private def l(implicit loc: At): Int = loc.line.value

  "valueOf[Option[_]]" should {
    "succeed on a Some(_)" in {
      valueOf(5.some) mustBe 5
    }

    "fail on a None" in {
      val cachedLoc = At.here
      try {
        valueOf(Option.empty[Int])
        fail("Should have failed")
      }
      catch {
        case tfe: TestFailedException =>
          implicit val loc: At = cachedLoc
          tfe.getMessage mustBe s"""$path:${l + 2} Option.empty[Int] was None"""
      }
    }
  }

  "valueOf[Either[_,_]]" should {
    "succeed on a Right(_)" in {
      valueOf(Either.right[String, Int](5)) mustBe 5
    }

    "fail on a Left(_)" in {
      val cachedLoc = At.here
      try {
        valueOf(Either.left[String, Int]("oops!"))
        fail("Should have failed")
      }
      catch {
        case tfe: TestFailedException =>
          implicit val loc: At = cachedLoc
          tfe.getMessage mustBe {
            s"""$path:${l + 2} Either.left[String, Int]("oops!") was a Left(oops!) rather than a Right(_)"""
          }
      }
    }
  }

  "valueOf[IO]" should {
    "succeed if the IO succeeds and none of the finalizers time out" in {
      valueOf(IO.pure(5)) mustBe 5
    }

    "fail if the IO fails" in {
      val cachedLoc = At.here
      try {
        valueOf(IO.raiseError[Int](new SmallError("Oops!")))
        fail("Should have failed")
      }
      catch {
        case tfe: TestFailedException =>
          implicit val loc: At = cachedLoc
          tfe.getMessage mustBe {
            s"""$path:${l + 2} IO.raiseError[Int](new SmallError("Oops!")) failed (SmallError: Oops!)"""
          }
      }
    }

    "fail if the IO times out" in {
      val cachedLoc = At.here
      try {
        valueOf(IO.sleep(quick).as(5))
        fail("Should have failed")
      }
      catch {
        case tfe: TestFailedException =>
          implicit val loc: At = cachedLoc
          tfe.getMessage mustBe s"""$path:${l + 2} IO.sleep(quick).as(5) timed out after $rightAway"""
      }
    }

    "fail if the IO finalizers time out" in {
      val long = IO.sleep(3.seconds)
      val cachedLoc = At.here
      try {
        valueOf(IO.pure(5).guaranteeCase(_ => long))
        fail("Should have failed")
      }
      catch {
        case tfe: TestFailedException =>
          implicit val loc: At = cachedLoc
          tfe.getMessage mustBe {
            s"""$path:${l + 2} IO.pure(5).guaranteeCase(_ => long) timed out while running finalizers, after $quick"""
          }
      }
    }

    "fail if the IO finalizers fail" in {
      val cachedLoc = At.here
      try {
        valueOf(
          IO.pure(5).guaranteeCase(_ => IO.raiseError(new SmallError("Oops!")))
        )
        fail("Should have failed")
      }
      catch {
        case tfe: TestFailedException =>
          implicit val loc: At = cachedLoc
          tfe.getMessage mustBe {
            s"""$path:${l + 2} IO.pure(5).guaranteeCase(_ => IO.raiseError(new SmallError("Oops!"))) failed (SmallError: Oops!)"""
          }
      }
    }
  }

  "valueOf[Future]" should {
    "succeed if the Future succeeds" in {
      valueOf(Future.successful(5)) mustBe 5
    }

    "fail if the Future fails" in {
      val cachedLoc = At.here
      try {
        valueOf(Future.failed[Int](new SmallError("Oops!")))
        fail("Should have failed")
      }
      catch {
        case tfe: TestFailedException =>
          implicit val loc: At = cachedLoc
          tfe.getMessage mustBe {
            s"""$path:${l + 2} Future.failed[Int](new SmallError("Oops!")) failed (SmallError: Oops!)"""
          }
      }
    }

    "fail if the Future times out" in {
      def sleepFor(fd: FiniteDuration) = Future(Thread.sleep(fd.toMillis)).as(5)

      val cachedLoc = At.here
      try {
        valueOf(sleepFor(10.seconds))
        fail("Should have failed")
      }
      catch {
        case tfe: TestFailedException =>
          implicit val loc: At = cachedLoc
          tfe.getMessage mustBe {
            s"""$path:${l + 2} sleepFor(10.seconds) timed out after ${timeouts.total}""".stripMargin
          }
      }
    }
  }
}
