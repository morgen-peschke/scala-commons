package peschke.collections.range

import cats.syntax.eq._
import peschke.PropSpec
import peschke.collections.range.syntax._
import peschke.scalacheck.NumericRangeGens
import peschke.scalacheck.RangeGens
import peschke.scalacheck.syntax._

import scala.collection.immutable.NumericRange

class RangeUtilsTest extends PropSpec {
  private val stepsInRange =
    for {
      range <- RangeGens.ranges(Int.MinValue / 2, Int.MaxValue / 2)
      steps <- range.chooseSteps
    } yield range -> steps

  private val stepsInNumericRange =
    for {
      range <- NumericRangeGens.numericRanges(
        (Int.MinValue / 2).toLong,
        (Int.MaxValue / 2).toLong
      )
      steps <- range.chooseSteps
    } yield range -> steps

  def calculationWasSafe[I](oldValue: I, newValue: I, stepSize: I)(implicit I: Integral[I]): Boolean = {
    def negative(i: I): Boolean = I.lt(i, I.zero)
    def positive(i: I): Boolean = I.gt(i, I.zero)

    if (positive(stepSize))
      !(positive(oldValue) && negative(newValue))
    else if (negative(stepSize))
      !(negative(oldValue) && positive(newValue))
    else true
  }

  def growthWasSafe(oldValue: Range, newValue: Range): Boolean = {
    val stepWasNotChanged = oldValue.step eqv newValue.step

    val startChangeWasSafe =
      (oldValue.start eqv newValue.start) || calculationWasSafe(
        oldValue.start,
        newValue.start,
        -oldValue.step
      )

    val endChangeWasSafe =
      (oldValue.end eqv newValue.end) || calculationWasSafe(
        oldValue.end,
        newValue.end,
        oldValue.step
      )

    stepWasNotChanged && startChangeWasSafe && endChangeWasSafe
  }

  def growthWasSafe(oldValue: NumericRange[Long], newValue: NumericRange[Long]): Boolean = {
    val stepWasNotChanged = oldValue.step eqv newValue.step

    val startChangeWasSafe =
      (oldValue.start eqv newValue.start) || calculationWasSafe(
        oldValue.start,
        newValue.start,
        -oldValue.step
      )

    val endChangeWasSafe =
      (oldValue.end eqv newValue.end) || calculationWasSafe(
        oldValue.end,
        newValue.end,
        oldValue.step
      )

    val lengthIsSafe =
      ((newValue.start - newValue.end) / newValue.step).abs < Int
        .MaxValue.toLong

    stepWasNotChanged && startChangeWasSafe && endChangeWasSafe && lengthIsSafe
  }

  property("RangeUtils.grow should be the inverse of Range#drop") {
    forAll(stepsInRange) { case (range, steps) =>
      val grown = range.grow(steps)
      whenever(growthWasSafe(range, grown)) {
        if (steps eqv 0) {
          grown mustBe range
        }
        else {
          grown.start must not be range.start
          grown.end mustBe range.end
          grown.step mustBe range.step
        }

        withClue(s"($grown).drop($steps): ") {
          grown.drop(steps) mustBe range
        }
      }
    }
  }

  property("RangeUtils.growRight should be the inverse of Range#dropRight") {
    forAll(stepsInRange) { case (range, steps) =>
      val grown = range.growRight(steps)
      whenever(growthWasSafe(range, grown)) {
        if (steps eqv 0) {
          grown mustBe range
        }
        else {
          grown.start mustBe range.start
          grown.end must not be range.end
          grown.step mustBe range.step
        }

        withClue(s"($grown).dropRight($steps): ") {
          grown.dropRight(steps) mustBe range
        }
      }
    }
  }

  property("RangeUtils.shift should be equivalent to growRight(n).drop(n)") {
    forAll(stepsInRange) { case (range, steps) =>
      val shifted = range.shift(steps)
      whenever(growthWasSafe(range, shifted)) {
        val grown = range.growRight(steps)
        whenever(growthWasSafe(range, grown)) {
          shifted mustBe grown.drop(steps)
        }
      }
    }
  }

  property("RangeUtils.shift should be the inverse of RangeUtils.unshift") {
    forAll(stepsInRange) { case (range, steps) =>
      val shifted = range.shift(steps)
      whenever(growthWasSafe(range, shifted)) {
        if (steps eqv 0) {
          shifted mustBe range
        }
        else {
          shifted.start must not be range.start
          shifted.end must not be range.end
          shifted.step mustBe range.step
        }
        shifted.unshift(steps) mustBe range
      }
    }
  }

  property("RangeUtils.unshift should be equivalent to grow(n).dropRight(n)") {
    forAll(stepsInRange) { case (range, steps) =>
      val shifted = range.unshift(steps)
      whenever(growthWasSafe(range, shifted)) {
        val grown = range.grow(steps)
        whenever(growthWasSafe(range, grown)) {
          shifted mustBe grown.dropRight(steps)
        }
      }
    }
  }

  property("RangeUtils.unshift should be the inverse of RangeUtils.shift") {
    forAll(stepsInRange) { case (range, steps) =>
      val unShifted = range.unshift(steps)
      whenever(growthWasSafe(range, unShifted)) {
        if (steps eqv 0) {
          unShifted mustBe range
        }
        else {
          unShifted.start must not be range.start
          unShifted.end must not be range.end
          unShifted.step mustBe range.step
        }
        unShifted.shift(steps) mustBe range
      }
    }
  }

  // Note: test coverage for the numeric versions is a bit sparse because it's
  // hard to reliably test without running into lengths greater than Int.MaxValue.
  // Some of the reference methods on Range also don't exist on NumericRange, so
  // they'll blow the heap trying to allocate an IndexedSeq[Long] with Int.MaxValue
  // elements.

  property("RangeUtils.sliceNumeric should be equivalent to drop(f).take(u - f)") {
    forAll(stepsInNumericRange.flatMap(g => g._1.chooseSteps.map(g -> _))) { case ((range, steps1), steps2) =>
      val from = steps1.min(steps2)
      val until = steps1.max(steps2)
      // Cannot replace with built-in slice because it will attempt to materialize the entire range
      // noinspection DropTakeToSlice
      range.sliceRange(from, until) mustBe range.drop(from).take(until - from)
    }
  }

  property(
    "RangeUtils.growNumeric should be the inverse of NumericRange#drop"
  ) {
    forAll(stepsInNumericRange) { case (range, steps) =>
      val grown = range.grow(steps)
      whenever(growthWasSafe(range, grown)) {
        if (steps eqv 0) {
          grown mustBe range
        }
        else {
          grown.start must not be range.start
          grown.end mustBe range.end
          grown.step mustBe range.step
        }

        withClue(s"($grown).drop($steps): ") {
          grown.drop(steps) mustBe range
        }
      }
    }
  }

  property(
    "RangeUtils.shiftNumeric should be equivalent to growRight(n).drop(n)"
  ) {
    forAll(stepsInNumericRange) { case (range, steps) =>
      val shifted = range.shift(steps)
      whenever(growthWasSafe(range, shifted)) {
        val grown = range.growRight(steps)
        whenever(growthWasSafe(range, grown)) {
          shifted mustBe grown.drop(steps)
        }
      }
    }
  }

  property(
    "RangeUtils.shiftNumeric should be the inverse of RangeUtils.unshiftNumeric"
  ) {
    forAll(stepsInNumericRange) { case (range, steps) =>
      val shifted = range.shift(steps)
      whenever(growthWasSafe(range, shifted)) {
        if (steps eqv 0) {
          shifted mustBe range
        }
        else {
          shifted.start must not be range.start
          shifted.end must not be range.end
          shifted.step mustBe range.step
        }
        shifted.unshift(steps) mustBe range
      }
    }
  }

  property(
    "RangeUtils.unshiftNumeric should be the inverse of RangeUtils.shiftNumeric"
  ) {
    forAll(stepsInNumericRange) { case (range, steps) =>
      val unShifted = range.unshift(steps)
      whenever(growthWasSafe(range, unShifted)) {
        if (steps eqv 0) {
          unShifted mustBe range
        }
        else {
          unShifted.start must not be range.start
          unShifted.end must not be range.end
          unShifted.step mustBe range.step
        }
        unShifted.shift(steps) mustBe range
      }
    }
  }
}
