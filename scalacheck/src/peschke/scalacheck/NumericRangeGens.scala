package peschke.scalacheck

import org.scalacheck.Gen
import peschke.Convertible
import peschke.numeric.Bounded
import peschke.Convertible.syntax._

import scala.collection.immutable.NumericRange

/** Various [[Gen]] factories to make working with [[NumericRange]] and
  * [[org.scalacheck.Gen]] easier.
  */
trait NumericRangeGens {
  private val MaxLength = BigInt(Int.MaxValue - 1)
  private def buildRangeSafely[A](start: A, end: A, step: A, inclusive: Boolean)
                                 (
                                     implicit I: Integral[A],
                                     A2BI:       Convertible[A, BigInt],
                                     BI2A:       Convertible[BigInt, Option[A]])
    : NumericRange[A] = {
    import I.{mkNumericOps, mkOrderingOps}
    val stepForward    = step.abs
    val normalizedStep = if (start < end) stepForward else -stepForward

    val length =
      (end.convert[BigInt] - start.convert[BigInt]).abs / step.convert[BigInt]
    val safeLength = length.min(MaxLength)
    val goodLength = safeLength.convertOpt[A].getOrElse(I.fromInt(Int.MaxValue))
    val safeEnd =
      if (safeLength == length) end else start + (goodLength * normalizedStep)

    try {
      if (inclusive) NumericRange.inclusive(start, safeEnd, normalizedStep)
      else NumericRange(start, safeEnd, normalizedStep)
    }
    catch {
      case e: IllegalArgumentException =>
        throw new IllegalArgumentException(
          s"Unable to safely create a NumericRange using $start -> $safeEnd by $step: ${e.getMessage}",
          e
        )
    }
  }

  /** Generate [[NumericRange]] values within a subset of the domain of `A`
    *
    * Other than producing `A` instead of [[Int]], this method should behave
    * identically to [[RangeGens.inclusiveRanges()]]
    */
  def inclusiveNumericRanges[A](min: A, max: A)
                               (
                                   implicit I: Integral[A],
                                   C:          Gen.Choose[A],
                                   ToBigInt:   Convertible[A, BigInt],
                                   ToA:        Convertible[BigInt, Option[A]])
    : Gen[NumericRange[A]] = {
    import I.{mkNumericOps, mkOrderingOps}
    for {
      start <- Gen.chooseNum(min, max)
      end   <- Gen.chooseNum(min, max)
      step  <- Gen.chooseNum(I.one, (start - end).abs.max(I.one))
    } yield buildRangeSafely(start, end, step, inclusive = true)
  }

  /** Generate [[NumericRange]] values within a subset of the domain of `A`
    *
    * Other than producing `A` instead of [[Int]], this method should behave
    * identically to [[RangeGens.exclusiveRanges()]]
    */
  def exclusiveNumericRanges[A](min: A, max: A)
                               (
                                   implicit I: Integral[A],
                                   C:          Gen.Choose[A],
                                   ToBigInt:   Convertible[A, BigInt],
                                   ToA:        Convertible[BigInt, Option[A]],
                                   B:          Bounded[A])
    : Gen[NumericRange[A]] = {
    import I.{mkNumericOps, mkOrderingOps}
    val safeMax = if (max == B.maximum) max - I.one else max
    val safeMin = if (safeMax < min) safeMax - I.one else min
    for {
      start <- Gen.chooseNum(safeMin, safeMax)
      end   <- Gen.chooseNum(safeMin, safeMax + I.one)
      step  <- Gen.chooseNum(I.one, (start - end).abs.max(I.one))
    } yield buildRangeSafely(start, end, step, inclusive = false)
  }

  /** Generate [[NumericRange]] values within a subset of the domain of `A`
    *
    * Other than producing [[NumericRange]] instead of [[Range]], this method
    * should behave identically to [[RangeGens.ranges()]]
    */
  def numericRanges[A](min: A, max: A)
                      (
                          implicit I: Integral[A],
                          C:          Gen.Choose[A],
                          ToBigInt:   Convertible[A, BigInt],
                          ToA:        Convertible[BigInt, Option[A]],
                          B:          Bounded[A])
    : Gen[NumericRange[A]] =
    Gen.oneOf(
      inclusiveNumericRanges(min, max),
      exclusiveNumericRanges(min, max)
    )

  /** Generate `A` values bounded by a [[NumericRange]]
    *
    * Other than not producing `A` values instead of [[Int]], this should behave
    * identically to [[RangeGens.choose()]]
    */
  def chooseNumeric[A](range: NumericRange[A])(implicit A: Numeric[A]): Gen[A] =
    if (range.isEmpty) Gen.fail
    else
      Gen.choose(0, range.length - 1).map { stepCount =>
        A.plus(range.start, A.times(A.fromInt(stepCount), range.step))
      }

}
object NumericRangeGens extends NumericRangeGens
