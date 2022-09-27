package peschke.testing

import cats.effect.IO
import cats.effect.unsafe.IORuntime
import cats.syntax.all._
import munit.Location
import peschke.testing.ValueExtractor.Timeouts

import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.Failure
import scala.util.Try
import scala.util.control.NoStackTrace

class SmallError(msg: String) extends IllegalArgumentException(msg) with NoStackTrace

class AssertionValueExtractorsTest extends munit.FunSuite with AssertionValueExtractors {
  implicit private val runtime: IORuntime = IORuntime.global
  implicit private val executionContext: ExecutionContext =
    ExecutionContext.global

  private val rightAway = 1.seconds
  private val quick = 2.seconds
  implicit val timeouts: Timeouts = Timeouts(rightAway, rightAway)

  private def path(implicit loc: Location): String = loc.path
  private def l(implicit loc: Location): Int = loc.line

  test("valueOf[Option[_]] should succeed on a Some(_)") {
    assertEquals(valueOf(5.some), 5)
  }

  test("valueOf[Option] should fail on a None") {
    val cachedLoc = implicitly[Location]
    try {
      valueOf(Option.empty[Int])
      fail("Should have failed")
    }
    catch {
      case ae: AssertionError =>
        implicit val loc: Location = cachedLoc
        assertEquals(
          ae.getMessage,
          s"""$path:${l + 2} Option.empty[Int] was None"""
        )
    }
  }

  test("valueOf[Either[_,_]] should succeed on a Right(_)") {
    assertEquals(valueOf(Either.right[String, Int](5)), 5)
  }

  test("valueOf[Either[_,_]] should fail on a Left(_)") {
    val cachedLoc = implicitly[Location]
    try {
      valueOf(Either.left[String, Int]("oops!"))
      fail("Should have failed")
    }
    catch {
      case ae: AssertionError =>
        implicit val loc: Location = cachedLoc
        assertEquals(
          ae.getMessage,
          s"""$path:${l + 2} Either.left[String, Int]("oops!") was a Left(oops!) rather than a Right(_)"""
        )
    }
  }

  test("valueOf[Validated[_,_]] should succeed on a Valid(_)") {
    assertEquals(valueOf(5.valid[String]), 5)
  }

  test("valueOf[Validated[_,_]] should fail on an Invalid(_)") {
    val cachedLoc = implicitly[Location]
    try {
      valueOf("oops!".invalid[Int])
      fail("Should have failed")
    }
    catch {
      case ae: AssertionError =>
        implicit val loc: Location = cachedLoc
        assertEquals(
          ae.getMessage,
          s"""$path:${l + 2} "oops!".invalid[Int] was an Invalid(oops!) rather than a Valid(_)"""
        )
    }
  }

  test("valueOf[Try[_]] should succeed on a Success(_)") {
    assertEquals(valueOf(Try(5)), 5)
  }

  test("valueOf[Try[_]] should fail on a Failure(_)") {
    val cachedLoc = implicitly[Location]
    try {
      valueOf(Failure(new SmallError("oops!")): Try[Int])
      fail("Should have failed")
    }
    catch {
      case ae: AssertionError =>
        implicit val loc: Location = cachedLoc
        assertEquals(
          ae.getMessage,
          s"""$path:${l + 2} Failure(new SmallError("oops!")): Try[Int] was a Failure(SmallError: oops!) rather than a Success(_)"""
        )
    }
  }

  test("valueOf[IO] should succeed if the IO succeeds and none of the finalizers time out") {
    assertEquals(valueOf(IO.pure(5)), 5)
  }

  test("valueOf[IO] should fail if the IO fails") {
    val cachedLoc = implicitly[Location]
    try {
      valueOf(IO.raiseError[Int](new SmallError("Oops!")))
      fail("Should have failed")
    }
    catch {
      case ae: AssertionError =>
        implicit val loc: Location = cachedLoc
        assertEquals(
          ae.getMessage,
          s"""$path:${l + 2} IO.raiseError[Int](new SmallError("Oops!")) failed (SmallError: Oops!)"""
        )
    }
  }

  test("valueOf[IO] should fail if the IO times out") {
    val cachedLoc = implicitly[Location]
    try {
      valueOf(IO.sleep(quick).as(5))
      fail("Should have failed")
    }
    catch {
      case ae: AssertionError =>
        implicit val loc: Location = cachedLoc
        assertEquals(
          ae.getMessage,
          s"""$path:${l + 2} IO.sleep(quick).as(5) timed out after $rightAway""".stripMargin
        )
    }
  }

  test("valueOf[IO] should fail if the IO finalizers time out") {
    val long = IO.sleep(3.seconds)
    val cachedLoc = implicitly[Location]
    try {
      valueOf(IO.pure(5).guaranteeCase(_ => long))
      fail("Should have failed")
    }
    catch {
      case ae: AssertionError =>
        implicit val loc: Location = cachedLoc
        assertEquals(
          ae.getMessage,
          s"""$path:${l + 2} IO.pure(5).guaranteeCase(_ => long) timed out while running finalizers, after $quick"""
        )
    }
  }

  test("valueOf[IO] should fail if the IO finalizers fail") {
    val cachedLoc = implicitly[Location]
    try {
      valueOf(
        IO.pure(5).guaranteeCase(_ => IO.raiseError(new SmallError("Oops!")))
      )
      fail("Should have failed")
    }
    catch {
      case ae: AssertionError =>
        implicit val loc: Location = cachedLoc
        assertEquals(
          ae.getMessage,
          s"""$path:${l + 2} IO.pure(5).guaranteeCase(_ => IO.raiseError(new SmallError("Oops!"))) failed (SmallError: Oops!)"""
        )
    }
  }

  test("valueOf[Future] should succeed if the Future succeeds") {
    assertEquals(valueOf(Future.successful(5)), 5)
  }

  test("valueOf[Future] should fail if the Future fails") {
    val cachedLoc = implicitly[Location]
    try {
      valueOf(Future.failed[Int](new SmallError("Oops!")))
      fail("Should have failed")
    }
    catch {
      case ae: AssertionError =>
        implicit val loc: Location = cachedLoc
        assertEquals(
          ae.getMessage,
          s"""$path:${l + 2} Future.failed[Int](new SmallError("Oops!")) failed (SmallError: Oops!)"""
        )
    }
  }

  test("valueOf[Future] should fail if the Future times out") {
    def sleepFor(fd: FiniteDuration) = Future(Thread.sleep(fd.toMillis)).as(5)
    val cachedLoc = implicitly[Location]
    try {
      valueOf(sleepFor(10.seconds))
      fail("Should have failed")
    }
    catch {
      case ae: AssertionError =>
        implicit val loc: Location = cachedLoc
        assertEquals(
          ae.getMessage,
          s"""$path:${l + 2} sleepFor(10.seconds) timed out after ${timeouts.total}""".stripMargin
        )
    }
  }
}
