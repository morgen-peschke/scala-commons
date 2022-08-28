package peschke.collections.range

import scala.collection.immutable.NumericRange

object syntax {
  implicit final class ScalaCommonsRangeOps(private val range: Range)
      extends AnyVal {

    /** The inverse of [[Range.drop]]
      *
      * @see
      *   [[RangeUtils.grow]]
      */
    def grow(steps: Int): Range = RangeUtils.grow(range, steps)

    /** The inverse of [[Range.dropRight]]
      *
      * @see
      *   [[RangeUtils.growRight]]
      */
    def growRight(steps: Int): Range = RangeUtils.growRight(range, steps)

    /** Shift the entire range by a number of steps
      *
      * @see
      *   [[RangeUtils.shift]]
      */
    def shift(steps: Int): Range = RangeUtils.shift(range, steps)

    /** Shift the entire range by a number of steps
      *
      * @see
      *   [[RangeUtils.unshift]]
      */
    def unshift(steps: Int): Range = RangeUtils.unshift(range, steps)
  }

  implicit final class ScalaCommonsNumericRangeOps[N]
    (private val range: NumericRange[N])
      extends AnyVal {

    /** Specialization of [[NumericRange.slice]], returning a [[NumericRange]]
      * instead of an [[IndexedSeq]]
      *
      * @see
      *   [[RangeUtils.shiftNumeric]]
      */
    def sliceRange(from: Int, until: Int)(implicit I: Integral[N])
      : NumericRange[N] =
      RangeUtils.sliceNumeric(range, from, until)

    /** The inverse of [[NumericRange.drop]]
      *
      * @see
      *   [[RangeUtils.growNumeric]]
      */
    def grow(steps: Int)(implicit I: Integral[N]): NumericRange[N] =
      RangeUtils.growNumeric(range, steps)

    /** The inverse of [[NumericRange.dropRight]]
      *
      * @see
      *   [[RangeUtils.growNumericRight]]
      */
    def growRight(steps: Int)(implicit I: Integral[N]): NumericRange[N] =
      RangeUtils.growNumericRight(range, steps)

    /** Shift the entire range by a number of steps
      *
      * @see
      *   [[RangeUtils.shiftNumeric]]
      */
    def shift(steps: Int)(implicit I: Integral[N]): NumericRange[N] =
      RangeUtils.shiftNumeric(range, steps)

    /** Shift the entire range by a number of steps
      *
      * @see
      *   [[RangeUtils.unshiftNumeric]]
      */
    def unshift(steps: Int)(implicit I: Integral[N]): NumericRange[N] =
      RangeUtils.unshiftNumeric(range, steps)
  }
}
