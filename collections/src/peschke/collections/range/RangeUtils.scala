package peschke.collections.range

import scala.collection.immutable.NumericRange

object RangeUtils {

  private def copy
    (range: Range)
    (start: Int = range.start, end: Int = range.end, step: Int = range.step) // scalafix:ok DisableSyntax.defaultArgs
    : Range =
    if (range.isInclusive) Range.inclusive(start, end, step)
    else Range(start, end, step)

  private def copyNumeric[N]
    (range:      NumericRange[N])
    (start:      N = range.start, end: N = range.end, step: N = range.step) // scalafix:ok DisableSyntax.defaultArgs
    (implicit I: Integral[N])
    : NumericRange[N] =
    if (range.isInclusive) NumericRange.inclusive(start, end, step)
    else NumericRange(start, end, step)

  /** The inverse of [[Range.drop]]
    *
    * Extends the range start such that `range.grow(n).drop(n) == range`
    */
  def grow(range: Range, steps: Int): Range =
    if (steps <= 0) range
    else copy(range)(start = range.start - (range.step * steps))

  /** The inverse of [[Range.dropRight]]
    *
    * Extends the range end such that `range.growRight(n).dropRight(n) == range`
    */
  def growRight(range: Range, steps: Int): Range =
    if (steps <= 0) range
    else copy(range)(end = range.end + (range.step * steps))

  /** Shift the entire range by a number of steps
    *
    * Equivalent to `range.growRight(n).drop(n)`
    *
    * Note: with the exception of situations where intermediate values would overflow or underflow, [[shift]] and
    * [[unshift]] are inverse operations
    */
  def shift(range: Range, steps: Int): Range =
    if (steps <= 0) range
    else {
      val offset = range.step * steps
      copy(range)(
        start = range.start + offset,
        end = range.end + offset
      )
    }

  /** Shift the entire range by a number of steps
    *
    * Equivalent to `range.grow(n).dropRight(n)`, and the inverse of [[shift]]
    */
  def unshift(range: Range, steps: Int): Range =
    if (steps <= 0) range
    else {
      val offset = range.step * steps
      copy(range)(
        start = range.start - offset,
        end = range.end - offset
      )
    }

  /** Specialization of [[NumericRange.slice]], returning a [[NumericRange]] instead of an [[IndexedSeq]]
    *
    * Equivalent to `r.drop(from).take(until - from)`
    */
  def sliceNumeric[N](range: NumericRange[N], from: Int, until: Int)(implicit I: Integral[N]): NumericRange[N] =
    if (from <= 0) range.take(until)
    else if (until >= range.length) range.drop(from)
    else {
      val fromValue = I.plus(range.start, I.times(range.step, I.fromInt(from)))

      def untilValue =
        I.plus(range.start, I.times(range.step, I.fromInt(until - 1)))

      if (from >= until) NumericRange(fromValue, fromValue, range.step)
      else NumericRange.inclusive(fromValue, untilValue, range.step)
    }

  /** The inverse of [[NumericRange.drop]]
    *
    * Extends the range start such that `range.grow(n).drop(n) == range`
    */
  def growNumeric[N](range: NumericRange[N], steps: Int)(implicit I: Integral[N]): NumericRange[N] =
    if (steps <= 0) range
    else
      copyNumeric(range)(start = I.minus(range.start, I.times(range.step, I.fromInt(steps))))

  /** The inverse of [[NumericRange.dropRight]]
    *
    * Extends the range end such that `range.growRight(n).dropRight(n) == range`
    */
  def growNumericRight[N](range: NumericRange[N], steps: Int)(implicit I: Integral[N]): NumericRange[N] =
    if (steps <= 0) range
    else
      copyNumeric(range)(end = I.plus(range.end, I.times(range.step, I.fromInt(steps))))

  /** Shift the entire range by a number of steps
    *
    * Equivalent to `range.drop(n).growRight(n)`
    *
    * Note: with the exception of situations where intermediate values would overflow or underflow, [[shiftNumeric]] and
    * [[unshiftNumeric]] are inverse operations
    */
  def shiftNumeric[N](range: NumericRange[N], steps: Int)(implicit I: Integral[N]): NumericRange[N] =
    if (steps <= 0) range
    else {
      val offset = I.times(range.step, I.fromInt(steps))
      copyNumeric(range)(
        start = I.plus(range.start, offset),
        end = I.plus(range.end, offset)
      )
    }

  /** Shift the entire range by a number of steps
    *
    * Equivalent to `range.dropRight(n).grow(n)`, and the inverse of [[shiftNumeric]]
    */
  def unshiftNumeric[N](range: NumericRange[N], steps: Int)(implicit I: Integral[N]): NumericRange[N] =
    if (steps <= 0) range
    else {
      val offset = I.times(range.step, I.fromInt(steps))
      copyNumeric(range)(
        start = I.minus(range.start, offset),
        end = I.minus(range.end, offset)
      )
    }
}
