package peschke.scalacheck

import cats.data.Chain
import org.scalacheck.Gen

/**
 * Various common operations to make working with [[org.scalacheck.Gen]] easier.
 *
 * Most of the time, bulk importing the members of [[peschke.scalacheck.syntax]] gives better results.
 */
object Combinators {
  def choose(range: Range): Gen[Int] =
    Gen.chooseNum(0, range.length).map { stepCount =>
      range.start + (stepCount * range.step)
    }

  def listOfR[A](range: Range, ga: Gen[A]): Gen[List[A]] =
    choose(range).flatMap(Gen.listOfN(_, ga))

  def vectorOfR[A](range: Range, ga: Gen[A]): Gen[Vector[A]] =
    choose(range).flatMap(Gen.buildableOfN[Vector[A], A](_, ga))

  def chainOfR[A](range: Range, ga: Gen[A]): Gen[Chain[A]] =
    vectorOfR(range, ga).map(Chain.fromSeq)
}
