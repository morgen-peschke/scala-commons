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
    def choose: Gen[Int] = RangeGens.choose(range)
  }

  implicit final class ScalaCommonsNumericRangeToGenOps[A]
    (private val range: NumericRange[A])
      extends AnyVal {
    def choose(implicit N: Numeric[A]): Gen[A] =
      NumericRangeGens.chooseNumeric(range)
  }

  implicit final class ScalaCommonsGenOpsEntry[A](private val gen: Gen[A])
      extends AnyVal {
    def as: ScalaCommonsGenOps[A] = new ScalaCommonsGenOps[A](gen)

    def optional: Gen[Option[A]] = Gen.option(gen)
  }

  final class ScalaCommonsGenOps[A](private val gen: Gen[A]) extends AnyVal {
    def list(range: Range)(implicit RG: RangeableGen[A, List[A]])
      : Gen[List[A]] = RG.lift(range, gen)

    def vector(range: Range)(implicit RG: RangeableGen[A, Vector[A]])
      : Gen[Vector[A]] = RG.lift(range, gen)

    def chain(range: Range)(implicit RG: RangeableGen[A, Chain[A]])
      : Gen[Chain[A]] = RG.lift(range, gen)

    def nel(range: Range)(implicit RG: RangeableGen[A, NonEmptyList[A]])
      : Gen[NonEmptyList[A]] = RG.lift(range, gen)

    def nec(range: Range)(implicit RG: RangeableGen[A, NonEmptyChain[A]])
      : Gen[NonEmptyChain[A]] = RG.lift(range, gen)

    def nev(range: Range)(implicit RG: RangeableGen[A, NonEmptyVector[A]])
      : Gen[NonEmptyVector[A]] = RG.lift(range, gen)

    def string(range: Range)(implicit RG: RangeableGen[A, String])
      : Gen[String] = RG.lift(range, gen)
  }
}
