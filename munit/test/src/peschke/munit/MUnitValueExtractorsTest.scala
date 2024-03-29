package peschke.munit

import cats.effect.IO
import cats.effect.unsafe.IORuntime
import cats.syntax.all._
import munit.FailException
import munit.Location
import peschke.testing.ValueExtractor.Timeouts

import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.control.NoStackTrace

class SmallError(msg: String) extends IllegalArgumentException(msg) with NoStackTrace

class MUnitValueExtractorsTest extends munit.FunSuite with MUnitValueExtractors {
  implicit private val runtime: IORuntime = IORuntime.global
  implicit private val executionContext: ExecutionContext =
    ExecutionContext.global

  private val rightAway = 1.seconds
  private val quick = 2.seconds
  implicit val timeouts: Timeouts = Timeouts(rightAway, rightAway)

  private def path(implicit loc: Location): String = loc.path
  private def l(implicit loc: Location): Int = loc.line
  private val <<< = "\u001b[7m" // Terminal control to start highlighting text
  private val >>> = "\u001b[0m" // Terminal control to stop highlighting text
  private val ___ = ""          // For padding, to make writing the expected results much, much, easier.

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
      case tf: FailException =>
        implicit val loc: Location = cachedLoc
        assertEquals(
          tf.message,
          s"""|$path:${l + 2} Option.empty[Int] was None
              |${___}${l + 1}:    try {
              |${<<<}${l + 2}:      extract(Option.empty[Int])${>>>}
              |${___}${l + 3}:      fail("Should have failed")""".stripMargin
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
      case tf: FailException =>
        implicit val loc: Location = cachedLoc
        assertEquals(
          tf.message,
          s"""|$path:${l + 2} Either.left[String, Int]("oops!") was a Left(oops!) rather than a Right(_)
              |${___}${l + 1}:    try {
              |${<<<}${l + 2}:      extract(Either.left[String, Int]("oops!"))${>>>}
              |${___}${l + 3}:      fail("Should have failed")""".stripMargin
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
      case tf: FailException =>
        implicit val loc: Location = cachedLoc
        assertEquals(
          tf.message,
          s"""|$path:${l + 2} IO.raiseError[Int](new SmallError("Oops!")) failed (SmallError: Oops!)
              |${___}${l + 1}:    try {
              |${<<<}${l + 2}:      extract(IO.raiseError[Int](new SmallError("Oops!")))${>>>}
              |${___}${l + 3}:      fail("Should have failed")""".stripMargin
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
      case tf: FailException =>
        implicit val loc: Location = cachedLoc
        assertEquals(
          tf.message,
          s"""|$path:${l + 2} IO.sleep(quick).as(5) timed out after $rightAway
              |${___}${l + 1}:    try {
              |${<<<}${l + 2}:      extract(IO.sleep(quick).as(5))${>>>}
              |${___}${l + 3}:      fail("Should have failed")""".stripMargin
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
      case tf: FailException =>
        implicit val loc: Location = cachedLoc
        assertEquals(
          tf.message,
          s"""|$path:${l + 2} IO.pure(5).guaranteeCase(_ => long) timed out while running finalizers, after $quick
              |${___}${l + 1}:    try {
              |${<<<}${l + 2}:      extract(IO.pure(5).guaranteeCase(_ => long))${>>>}
              |${___}${l + 3}:      fail("Should have failed")""".stripMargin
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
      case tf: FailException =>
        implicit val loc: Location = cachedLoc
        assertEquals(
          tf.message,
          s"""|$path:${l + 2} IO.pure(5).guaranteeCase(_ => IO.raiseError(new SmallError("Oops!"))) failed (SmallError: Oops!)
              |${___}${l + 1}:    try {
              |${<<<}${l + 2}:      extract(${>>>}
              |${___}${l + 3}:        IO.pure(5).guaranteeCase(_ => IO.raiseError(new SmallError("Oops!")))""".stripMargin
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
          s"""|$path:${l + 2} Future.failed[Int](new SmallError("Oops!")) failed (SmallError: Oops!)
              |${___}${l + 1}:    try {
              |${<<<}${l + 2}:      extract(Future.failed[Int](new SmallError("Oops!")))${>>>}
              |${___}${l + 3}:      fail("Should have failed")""".stripMargin
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
          s"""|$path:${l + 2} sleepFor(10.seconds) timed out after ${timeouts.total}
              |${___}${l + 1}:    try {
              |${<<<}${l + 2}:      extract(sleepFor(10.seconds))${>>>}
              |${___}${l + 3}:      fail("Should have failed")""".stripMargin
        )
    }
  }
}
