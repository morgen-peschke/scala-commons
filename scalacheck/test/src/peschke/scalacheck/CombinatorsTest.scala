package peschke.scalacheck

import org.scalacheck.Gen
import org.scalatest.Inspectors
import peschke.PropSpec

class CombinatorsTest extends PropSpec {
  private val bounds =
    for {
      a <- Gen.chooseNum(Int.MinValue, Int.MaxValue - 100)
      b <- Gen.chooseNum(a, Int.MaxValue - 100)
    } yield (a, b)

  property("Combinators.inclusiveRanges should produce ranges entirely contained within min and max") {
    forAll(bounds.flatMap(b => (Combinators.inclusiveRanges _).tupled(b).map(b -> _))) {
      case ((start, end), range) =>
        range.start must be >= start
        if (range.nonEmpty) {
          range.last must be <= end
        }
    }
  }

  property("Combinators.exclusiveRanges should produce ranges entirely contained within min and max") {
    forAll(bounds.flatMap(b => (Combinators.exclusiveRanges _).tupled(b).map(b -> _))) {
      case ((start, end), range) =>
        range.start must be >= start
        if (range.nonEmpty) {
          range.last must be <= end
        }
    }
  }

  property("Combinators.ranges should produce ranges entirely contained within min and max") {
    forAll(bounds.flatMap(b => (Combinators.ranges _).tupled(b).map(b -> _))) {
      case ((start, end), range) =>
        range.start must be >= start
        if (range.nonEmpty) {
          range.last must be <= end
        }
    }
  }

  property("Combinators.ranges should not choke when asked to produce ranges with extreme bounds") {
    val closeToMax =
        for {
          a <- Gen.chooseNum (Int.MaxValue - 10, Int.MaxValue)
          b <- Gen.chooseNum (a, Int.MaxValue)
        } yield (a, b, "close to Int.MaxValue")

    val reallyLong =
      for {
        a <- Gen.chooseNum(Int.MinValue, Int.MinValue - 10)
        b <- Gen.chooseNum(Int.MaxValue - 10, Int.MaxValue)
      } yield (a, b, "really long ranges")

    forAll(Gen.oneOf(closeToMax, reallyLong)) {
      case (start, end, _) =>
        val gen = Combinators.ranges(start, end)
        val values = List.fill(10)(gen.sample.value)
        Inspectors.forAll(values)(_.length)
    }
  }

  property("Combinator.choose should produce elements entirely contained within the provided range") {
    forAll(bounds.flatMap((Combinators.ranges _).tupled).flatMap(r => Combinators.choose(r).map(r -> _))) {
      case (range, i) => range.contains(i) mustBe true
    }
  }
}
