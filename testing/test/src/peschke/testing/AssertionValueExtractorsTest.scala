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

  test("extract[Option[_]] should succeed on a Some(_)") {
    assertEquals(extract(5.some), 5)
  }

  test("extract[Option] should fail on a None") {
    val cachedLoc = implicitly[Location]
    try {
      extract(Option.empty[Int])
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

  test("extract[Either[_,_]] should succeed on a Right(_)") {
    assertEquals(extract(Either.right[String, Int](5)), 5)
  }

  test("extract[Either[_,_]] should fail on a Left(_)") {
    val cachedLoc = implicitly[Location]
    try {
      extract(Either.left[String, Int]("oops!"))
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

  test("extract[Validated[_,_]] should succeed on a Valid(_)") {
    assertEquals(extract(5.valid[String]), 5)
  }

  test("extract[Validated[_,_]] should fail on an Invalid(_)") {
    val cachedLoc = implicitly[Location]
    try {
      extract("oops!".invalid[Int])
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

  test("extract[Try[_]] should succeed on a Success(_)") {
    assertEquals(extract(Try(5)), 5)
  }

  test("extract[Try[_]] should fail on a Failure(_)") {
    val cachedLoc = implicitly[Location]
    try {
      extract(Failure(new SmallError("oops!")): Try[Int])
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

  test("extract[IO] should succeed if the IO succeeds and none of the finalizers time out") {
    assertEquals(extract(IO.pure(5)), 5)
  }

  test("extract[IO] should fail if the IO fails") {
    val cachedLoc = implicitly[Location]
    try {
      extract(IO.raiseError[Int](new SmallError("Oops!")))
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

  test("extract[IO] should fail if the IO times out") {
    val cachedLoc = implicitly[Location]
    try {
      extract(IO.sleep(quick).as(5))
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

  test("extract[IO] should fail if the IO finalizers time out") {
    val long = IO.sleep(3.seconds)
    val cachedLoc = implicitly[Location]
    try {
      extract(IO.pure(5).guaranteeCase(_ => long))
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

  test("extract[IO] should fail if the IO finalizers fail") {
    val cachedLoc = implicitly[Location]
    try {
      extract(
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

  test("extract[Future] should succeed if the Future succeeds") {
    assertEquals(extract(Future.successful(5)), 5)
  }

  test("extract[Future] should fail if the Future fails") {
    val cachedLoc = implicitly[Location]
    try {
      extract(Future.failed[Int](new SmallError("Oops!")))
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

  test("extract[Future] should fail if the Future times out") {
    def sleepFor(fd: FiniteDuration) = Future(Thread.sleep(fd.toMillis)).as(5)
    val cachedLoc = implicitly[Location]
    try {
      extract(sleepFor(10.seconds))
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
