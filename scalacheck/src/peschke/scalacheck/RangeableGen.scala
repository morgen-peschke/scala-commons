package peschke.scalacheck

import cats.data.{Chain, NonEmptyChain, NonEmptyList, NonEmptyVector}
import org.scalacheck.Gen

/**
 * A typeclass to define how to lift repeated values from a `Gen[A]` into some other type.
 *
 * @tparam A the source type, which will be the elements of the resulting value
 * @tparam B the destination type, usually a collection of some sort, though not parameterized because
 *           [[scala.Predef.String]] is also handy.
 */
trait RangeableGen[A, B] {
  def lift(count: Range, ga: Gen[A]): Gen[B]

  /**
   * Use this [[RangeableGen]] to produce another [[RangeableGen]], via a mapping function.
   */
  def map[C](gb2gc: B => C): RangeableGen[A, C] = lift(_, _).map(gb2gc)
}

object RangeableGen {
  def apply[A,B](implicit GL: RangeableGen[A,B]): GL.type = GL

  implicit def liftToList[A]: RangeableGen[A, List[A]] = Combinators.listOfR(_, _)

  implicit def liftToVector[A]: RangeableGen[A, Vector[A]] = Combinators.vectorOfR(_, _)

  implicit def liftToChain[A]: RangeableGen[A, Chain[A]] = Combinators.chainOfR(_, _)

  implicit def liftToNonEmptyList[A]: RangeableGen[A, NonEmptyList[A]] =
    (count, ga) =>
      for {
        head <- ga
        tail <- Combinators.listOfR(count.start + count.step to count.last by count.step, ga)
      } yield NonEmptyList(head, tail)

  implicit def liftToNonEmptyChain[A]: RangeableGen[A, NonEmptyChain[A]] =
    (count, ga) =>
      for {
        head <- ga
        tail <- Combinators.chainOfR(count.start + count.step to count.last by count.step, ga)
      } yield NonEmptyChain.fromChainPrepend(head, tail)

  implicit def liftToNonEmptyVector[A]: RangeableGen[A, NonEmptyVector[A]] =
    liftToNonEmptyChain[A].map(_.toNonEmptyVector)

  implicit val charToString: RangeableGen[Char, String] =
    (count, ga) => Combinators.choose(count).flatMap(Gen.stringOfN(_, ga))
}
