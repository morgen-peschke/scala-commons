package peschke.scalacheck

import org.scalacheck.Gen
import org.scalatest.Inspectors
import peschke.PropSpec
import peschke.scalacheck.NumericRangeGens.PrettyRange

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
  val chooseNumeric: PrettyRange[Long] => Gen[Long] = pr =>
    NumericRangeGens.chooseNumeric(pr.range)
  val withinNumeric: PrettyRange[Long] => Gen[PrettyRange[Long]] =
    pr => NumericRangeGens.slices(pr.range).map(PrettyRange(_))

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

  // Can't use the full range of Long here due to what appears to be an issue with NumericRange
  // see: https://github.com/scala/bug/issues/12635
  private val arbitraryRanges =
    numericRanges(Long.MinValue / 2L, Long.MaxValue / 2L)
  // .map(r => if (r.range.step < 0L) r.copy(range = r.range.reverse) else r)

  property("NumericRangeGens[Long].chooseNumeric should produce elements entirely contained within the provided range") {
    forAll(arbitraryRanges.flatMap(r => chooseNumeric(r).map(r -> _))) {
      case (PrettyRange(range), i) => range.contains(i) mustBe true
    }
  }

  property("NumericRangeGens[Long].within should produce ranges that are subsets of the provided range") {
    forAll(arbitraryRanges.flatMap(r => withinNumeric(r).map(r -> _))) {
      case (PrettyRange(reference), PrettyRange(produced)) =>
        produced.step mustBe reference.step
        withClue(s"($reference) contains (${produced.start}):") {
          reference.contains(produced.start) mustBe true
        }
        if (produced.nonEmpty) {
          withClue(s"($reference) contains (${produced.last}):") {
            reference.contains(produced.last) mustBe true
          }
        }
    }
  }
}
