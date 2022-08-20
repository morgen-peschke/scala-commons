package peschke.scalacheck

import org.scalacheck.Gen
import org.scalatest.Inspectors
import org.scalatest.prop.TableDrivenPropertyChecks
import peschke.PropSpec

class RangeGensTest extends PropSpec with TableDrivenPropertyChecks {
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

  property("RangeGens.choose should produce elements entirely contained within the provided range") {
    forAll(bounds.flatMap((ranges _).tupled).flatMap(r => choose(r).map(r -> _))) {
      case (range, i) => range.contains(i) mustBe true
    }
  }
}
