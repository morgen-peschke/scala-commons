package peschke.scalacheck

import org.scalacheck.Gen
import org.scalatest.Inspectors
import peschke.PropSpec

class RangeGensTest extends PropSpec {
  import RangeGens._

  private val bounds =
    for {
      a <- Gen.chooseNum(Int.MinValue, Int.MaxValue - 100)
      b <- Gen.chooseNum(a, Int.MaxValue - 100)
    } yield (a, b)

  property("RangeGens.inclusiveRanges should produce ranges entirely contained within min and max") {
    forAll(bounds.flatMap(b => (inclusiveRanges _).tupled(b).map(b -> _))) {
      case ((start, end), range) =>
        range.start must be >= start
        if (range.nonEmpty) {
          range.last must be <= end
        }
    }
  }

  property("RangeGens.exclusiveRanges should produce ranges entirely contained within min and max") {
    forAll(bounds.flatMap(b => (exclusiveRanges _).tupled(b).map(b -> _))) {
      case ((start, end), range) =>
        range.start must be >= start
        if (range.nonEmpty) {
          range.last must be <= end
        }
    }
  }

  property("RangeGens.ranges should produce ranges entirely contained within min and max") {
    forAll(bounds.flatMap(b => (ranges _).tupled(b).map(b -> _))) {
      case ((start, end), range) =>
        range.start must be >= start
        if (range.nonEmpty) {
          range.last must be <= end
        }
    }
  }

  property("RangeGens.ranges should not choke when asked to produce ranges with extreme bounds") {
    val closeToMax =
      for {
        a <- Gen.chooseNum(Int.MaxValue - 10, Int.MaxValue)
        b <- Gen.chooseNum(a, Int.MaxValue)
      } yield (a, b, "close to Int.MaxValue")

    val reallyLong =
      for {
        a <- Gen.chooseNum(Int.MinValue, Int.MinValue + 10)
        b <- Gen.chooseNum(Int.MaxValue - 10, Int.MaxValue)
      } yield (a, b, "really long ranges")

    forAll(Gen.oneOf(closeToMax, reallyLong)) {
      case (start, end, _) =>
        val gen    = ranges(start, end)
        val values = List.fill(10)(gen.sample.value)
        Inspectors.forAll(values)(_.length)
    }
  }

  private val problematicBounds = Table(
    ("start", "end"),
    (Int.MaxValue, Int.MaxValue),
    (Int.MinValue, Int.MinValue),
    (Int.MinValue, Int.MaxValue)
  )

  property(
    "RangeGens.inclusiveRanges should act appropriately for problematic input"
  ) {
    forAll(problematicBounds) { (start, end) =>
      val gen    = inclusiveRanges(start, end)
      val values = List.fill(10)(gen.sample.value)
      Inspectors.forAll(values)(_.length)
    }
  }

  property(
    "RangeGens.exclusiveRanges should act appropriately for problematic input"
  ) {
    forAll(problematicBounds) { (start, end) =>
      val gen    = exclusiveRanges(start, end)
      val values = List.fill(10)(gen.sample.value)
      Inspectors.forAll(values)(_.length)
    }
  }

  // Can't use the full range of Int here due to what appears to be an issue with Range
  // see: https://github.com/scala/bug/issues/12635
  private val arbitraryRanges =
    ranges(Int.MinValue / 2, Int.MaxValue / 2).map(r =>
      if (r.step < 0) r.reverse else r
    )

  property("RangeGens.choose should produce elements entirely contained within the provided range") {
    forAll(arbitraryRanges.flatMap(r => choose(r).map(r -> _))) {
      case (range, i) =>
        withClue(s"($range) contains ($i):") {
          range.contains(i) mustBe true
        }
    }
  }

  property("RangeGens.within should produce ranges that are subsets of the provided range") {
    forAll(arbitraryRanges.flatMap(r => slices(r).map(r -> _))) {
      case (reference, produced) =>
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
