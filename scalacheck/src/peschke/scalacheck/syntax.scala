package peschke.scalacheck

import cats.data.Chain
import cats.data.NonEmptyChain
import cats.data.NonEmptyList
import cats.data.NonEmptyVector
import org.scalacheck.Gen

import scala.collection.immutable.NumericRange

/** Various helpers to make working with [[org.scalacheck.Gen]] a bit easier.
  */
object syntax {
  implicit final class ScalaCommonsRangeToGenOps(private val range: Range)
      extends AnyVal {

    /** Generate [[Int]] values bounded by a [[Range]]
      *
      * @see
      *   [[RangeGens.choose]]
      */
    def choose: Gen[Int] = RangeGens.choose(range)

    /** Generate a value in the range [0, range.length)
      *
      * @see
      *   [[RangeGens.chooseSteps]]
      */
    def chooseSteps: Gen[Int] = RangeGens.chooseSteps(range)

    /** Generate [[Range]]s that are a subset of a reference [[Range]]
      *
      * @see
      *   [[RangeGens.slices]]
      */
    def slices: Gen[Range] = RangeGens.slices(range)
  }

  implicit final class ScalaCommonsNumericRangeToGenOps[A]
    (private val range: NumericRange[A])
      extends AnyVal {

    /** Generate `A` values bounded by a [[NumericRange]]
      *
      * @see
      *   [[NumericRangeGens.chooseNumeric]]
      */
    def choose(implicit I: Integral[A]): Gen[A] =
      NumericRangeGens.chooseNumeric(range)

    /** Generate a value in the range [0, range.length)
      *
      * @see
      *   [[NumericRangeGens.chooseSteps]]
      */
    def chooseSteps: Gen[Int] = NumericRangeGens.chooseSteps(range)

    /** Generate [[NumericRange]]s that are a subset of a reference
      * [[NumericRange]]
      *
      * @see
      *   [[NumericRangeGens.slices]]
      */
    def slices(implicit I: Integral[A]): Gen[NumericRange[A]] =
      NumericRangeGens.slices(range)
  }

  implicit final class ScalaCommonsGenOpsEntry[A](private val baseGen: Gen[A])
      extends AnyVal {

    /** Entry point for scala-commons enriched methods that don't directly pass
      * through to existing behavior
      */
    def gen: ScalaCommonsGenOps[A] = new ScalaCommonsGenOps[A](baseGen)

    def optional: Gen[Option[A]] = Gen.option(baseGen)

    def zip[B](genB: Gen[B]): Gen[(A, B)] = Gen.zip(baseGen, genB)
  }

  final class ScalaCommonsGenOps[A](private val gen: Gen[A]) extends AnyVal {

    /** Lift a [[org.scalacheck.Gen]] of `A` to an [[org.scalacheck.Gen]] of
      * [[List]] of `A` by repeated application.
      *
      * @see
      *   [[RangeableGen.lift]]
      */
    def list(range: Range)(implicit RG: RangeableGen[A, List[A]])
      : Gen[List[A]] = RG.lift(range, gen)

    /** Lift a [[org.scalacheck.Gen]] of `A` to an [[org.scalacheck.Gen]] of
      * [[Vector]] of `A` by repeated application.
      *
      * @see
      *   [[RangeableGen.lift]]
      */
    def vector(range: Range)(implicit RG: RangeableGen[A, Vector[A]])
      : Gen[Vector[A]] = RG.lift(range, gen)

    /** Lift a [[org.scalacheck.Gen]] of `A` to an [[org.scalacheck.Gen]] of
      * [[Chain]] of `A` by repeated application.
      *
      * @see
      *   [[RangeableGen.lift]]
      */
    def chain(range: Range)(implicit RG: RangeableGen[A, Chain[A]])
      : Gen[Chain[A]] = RG.lift(range, gen)

    /** Lift a [[org.scalacheck.Gen]] of `A` to an [[org.scalacheck.Gen]] of
      * [[NonEmptyList]] of `A` by repeated application.
      *
      * @see
      *   [[RangeableGen.lift]]
      */
    def nel(range: Range)(implicit RG: RangeableGen[A, NonEmptyList[A]])
      : Gen[NonEmptyList[A]] = RG.lift(range, gen)

    /** Lift a [[org.scalacheck.Gen]] of `A` to an [[org.scalacheck.Gen]] of
      * [[NonEmptyChain]] of `A` by repeated application.
      *
      * @see
      *   [[RangeableGen.lift]]
      */
    def nec(range: Range)(implicit RG: RangeableGen[A, NonEmptyChain[A]])
      : Gen[NonEmptyChain[A]] = RG.lift(range, gen)

    /** Lift a [[org.scalacheck.Gen]] of `A` to an [[org.scalacheck.Gen]] of
      * [[NonEmptyVector]] of `A` by repeated application.
      *
      * @see
      *   [[RangeableGen.lift]]
      */
    def nev(range: Range)(implicit RG: RangeableGen[A, NonEmptyVector[A]])
      : Gen[NonEmptyVector[A]] = RG.lift(range, gen)

    /** Lift a [[org.scalacheck.Gen]] of [[Char]] to an [[org.scalacheck.Gen]]
      * of [[String]] by repeated application.
      *
      * @see
      *   [[RangeableGen.lift]]
      */
    def string(range: Range)(implicit RG: RangeableGen[A, String])
      : Gen[String] = RG.lift(range, gen)
  }
}
