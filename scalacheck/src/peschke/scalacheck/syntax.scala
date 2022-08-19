package peschke.scalacheck

import cats.data.{Chain, NonEmptyChain, NonEmptyList, NonEmptyVector}
import org.scalacheck.Gen

/** Various helpers to make working with [[org.scalacheck.Gen]] a bit easier.
  */
object syntax {
  implicit final class ScalaCommonsRangeToGenOps(private val range: Range)
      extends AnyVal {
    def choose: Gen[Int] = Combinators.choose(range)
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
