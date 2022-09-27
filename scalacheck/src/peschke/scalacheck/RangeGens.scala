package peschke.scalacheck

import cats.syntax.eq._
import org.scalacheck.Gen

/** Various [[Gen]] factories to make working with [[Range]] and [[org.scalacheck.Gen]] easier.
  */
trait RangeGens {

  /** Generate [[Range]] values within a subset of the domain of [[Int]]
    *
    * Bounds are inclusive to match expectations of [[Gen.chooseNum()]].
    *
    * This produces only [[Range.Inclusive]] values.
    *
    * Note: to avoid running afoul of the invariants on [[Range]], elements with index past [[Int.MaxValue]] will be
    * dropped
    *
    * @param min
    *   Inclusive minimum for [[Range.start]]
    * @param max
    *   Inclusive maximum for [[Range.end]]
    */
  def inclusiveRanges(min: Int, max: Int): Gen[Range] =
    for {
      start <- Gen.chooseNum(min, max)
      end   <- Gen.chooseNum(min, max)
      step  <- Gen.chooseNum(1, (start - end).abs.max(1))
    } yield (start to end by (if (start < end) step else -step))
      .take(Int.MaxValue)

  /** Generate [[Range]] values within a subset of the domain of [[Int]]
    *
    * Bounds are inclusive to match expectations of [[Gen.chooseNum()]].
    *
    * This produces only [[Range.Exclusive]] values.
    *
    * Note: to avoid running afoul of the invariants on [[Range]], the following normalization is done:
    *   - If `max` is [[Int.MaxValue]], `max - 1` will be used instead. If `min` is [[Int.MaxValue]], it will also be
    *     lowered to avoid issues with [[Gen.chooseNum()]]
    *   - Elements with index past [[Int.MaxValue]] will be dropped
    *
    * @param min
    *   Inclusive minimum for [[Range.start]]
    * @param max
    *   Inclusive maximum for [[Range.end]]
    */
  def exclusiveRanges(min: Int, max: Int): Gen[Range] = {
    val safeMax = if (max === Int.MaxValue) max - 1 else max
    val safeMin = if (safeMax < min) safeMax - 1 else min
    for {
      start <- Gen.chooseNum(safeMin, safeMax)
      end   <- Gen.chooseNum(safeMin, safeMax + 1)
      step  <- Gen.chooseNum(1, (start - end).abs.max(1))
    } yield (start until end by (if (start < end) step else -step))
      .take(Int.MaxValue)
  }

  /** Generate [[Range]] values within a subset of the domain of [[Int]]
    *
    * Bounds are inclusive to match expectations of [[Gen.chooseNum()]].
    *
    * This produces both [[Range.Inclusive]] and [[Range.Exclusive]] values.
    *
    * Note: to avoid running a foul of the invariants of [[Range]], normalization is applied. See [[inclusiveRanges()]]
    * and [[exclusiveRanges()]] for details.
    *
    * @param min
    *   Inclusive minimum for [[Range.start]]
    * @param max
    *   Inclusive maximum for [[Range.end]]
    */
  def ranges(min: Int, max: Int): Gen[Range] =
    Gen.oneOf(
      inclusiveRanges(min, max),
      exclusiveRanges(min, max)
    )

  /** Generate a value in the range [0, range.length)
    *
    * This can be a safer way to generate a number of steps within a range.
    */
  def chooseSteps(range: Range): Gen[Int] =
    if (range.isEmpty) Gen.const(0)
    else Gen.chooseNum(0, 1.max(range.length - 1))

  /** Generate [[Int]] values bounded by a [[Range]]
    *
    * Values will honor [[Range.step]], so [[Range.contains]] should return `true` for all values produced by this
    * [[Gen]]
    */
  def choose(range: Range): Gen[Int] =
    if (range.isEmpty) Gen.fail
    else
      Gen.chooseNum(0, range.length - 1).map { stepCount =>
        range.start + (stepCount * range.step)
      }

  /** Generate [[Range]]s that are a subset of a reference [[Range]]
    */
  def slices(range: Range): Gen[Range] =
    if (range.isEmpty) Gen.fail
    else
      for {
        a <- Gen.chooseNum(0, range.length - 1)
        b <- Gen.chooseNum(0, range.length - 1)
      } yield range.slice(a.min(b), a.max(b))
}
object RangeGens extends RangeGens
