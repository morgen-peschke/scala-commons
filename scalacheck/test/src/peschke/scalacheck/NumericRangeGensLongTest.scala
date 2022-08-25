package peschke.scalacheck

import org.scalacheck.Gen
import org.scalatest.Inspectors
import peschke.PropSpec
import peschke.scalacheck.NumericRangeGensLongTest.PrettyRange

import scala.collection.immutable.NumericRange

class NumericRangeGensLongTest extends PropSpec {
  private val bounds =
    for {
      a <- Gen.chooseNum[Long](Long.MinValue, Long.MaxValue - 100L)
      b <- Gen.chooseNum[Long](a, Long.MaxValue - 100L)
    } yield (a, b)

  val inclusiveNumericRanges: (Long, Long) => Gen[PrettyRange[Long]] =
    NumericRangeGens.inclusiveNumericRanges(_, _).map(PrettyRange(_))
  val exclusiveNumericRanges: (Long, Long) => Gen[PrettyRange[Long]] =
    NumericRangeGens.exclusiveNumericRanges(_, _).map(PrettyRange(_))
  val numericRanges: (Long, Long) => Gen[PrettyRange[Long]] =
    NumericRangeGens.numericRanges(_, _).map(PrettyRange(_))

  property("NumericRangeGens[Long].inclusiveNumericRanges should produce ranges entirely contained within min and max") {
    forAll(bounds.flatMap(b => inclusiveNumericRanges.tupled(b).map(b -> _))) {
      case ((start, end), PrettyRange(range)) =>
        range.start must be >= start
        if (range.nonEmpty) {
          range.last must be <= end
        }
    }
  }

  property("NumericRangeGens[Long].exclusiveNumericRanges should produce ranges entirely contained within min and max") {
    forAll(bounds.flatMap(b => exclusiveNumericRanges.tupled(b).map(b -> _))) {
      case ((start, end), PrettyRange(range)) =>
        range.start must be >= start
        if (range.nonEmpty) {
          range.last must be <= end
        }
    }
  }

  property("NumericRangeGens[Long].numericRanges should produce ranges entirely contained within min and max") {
    forAll(bounds.flatMap(b => numericRanges.tupled(b).map(b -> _))) {
      case ((start, end), PrettyRange(range)) =>
        range.start must be >= start
        if (range.nonEmpty) {
          range.last must be <= end
        }
    }
  }

  property("NumericRangeGens[Long].numericRanges should not choke when asked to produce ranges with extreme bounds") {
    val closeToMax =
      for {
        a <- Gen.chooseNum(Long.MaxValue - 10L, Long.MaxValue)
        b <- Gen.chooseNum(a, Long.MaxValue)
      } yield (a, b, "close to Bounded[Long].maximum")

    val reallyLong =
      for {
        a <- Gen.chooseNum(Long.MinValue, Long.MinValue + 10L)
        b <- Gen.chooseNum(Long.MaxValue - 10L, Long.MaxValue)
      } yield (a, b, "really long ranges")

    forAll(Gen.oneOf(closeToMax, reallyLong)) {
      case (start, end, _) =>
        val gen    = numericRanges(start, end)
        val values = List.fill(10)(gen.sample.value)
        Inspectors.forAll(values)(_.range.length)
    }
  }

  private val problematicBounds = Table(
    ("start", "end"),
    (Long.MaxValue, Long.MaxValue),
    (Long.MinValue, Long.MinValue),
    (Long.MinValue, Long.MaxValue)
  )

  property("NumericRangeGens[Long].inclusiveNumericRanges should act appropriately for problematic input") {
    forAll(problematicBounds) { (start, end) =>
      val gen    = inclusiveNumericRanges(start, end)
      val values = List.fill(10)(gen.sample.value)
      Inspectors.forAll(values)(_.range.length)
    }
  }

  property("NumericRangeGens[Long].exclusiveNumericRanges should act appropriately for problematic input") {
    forAll(problematicBounds) { (start, end) =>
      val gen    = exclusiveNumericRanges(start, end)
      val values = List.fill(10)(gen.sample.value)
      Inspectors.forAll(values)(_.range.length)
    }
  }

  property("NumericRangeGens[Long].chooseNumeric should produce elements entirely contained within the provided range") {
    forAll(
      bounds
        .flatMap(numericRanges.tupled).flatMap(r =>
          NumericRangeGens.chooseNumeric(r.range).map(r -> _)
        )
    ) { case (PrettyRange(range), i) => range.contains(i) mustBe true }
  }
}
object NumericRangeGensLongTest {
  // Needed because invalid ranges break ScalaTest
  final case class PrettyRange[A](range: NumericRange[A]) {
    override def toString: String =
      if (range.isInclusive) s"${range.start} to ${range.end} by ${range.step}"
      else s"${range.start} until ${range.end} by ${range.step}"
  }
}
