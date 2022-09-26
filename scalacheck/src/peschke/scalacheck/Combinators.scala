package peschke.scalacheck

import cats.data.Chain
import org.scalacheck.Gen

/** Various common operations and [[Gen]] factories to make working with [[org.scalacheck.Gen]] easier.
  *
  * Most of the time, bulk importing the members of [[peschke.scalacheck.syntax]] gives better results.
  */
object Combinators {

  /** Similar to [[Gen.listOfN]], however the length of the list is generated based on `range`, rather than a fixed
    * value
    */
  def listOfR[A](range: Range, ga: Gen[A]): Gen[List[A]] =
    RangeGens.choose(range).flatMap(Gen.listOfN(_, ga))

  /** Similar to [[listOfR]], however it returns a [[Vector]]
    */
  def vectorOfR[A](range: Range, ga: Gen[A]): Gen[Vector[A]] =
    RangeGens.choose(range).flatMap(Gen.buildableOfN[Vector[A], A](_, ga))

  /** Similar to [[listOfR]], however it returns a [[Chain]]
    */
  def chainOfR[A](range: Range, ga: Gen[A]): Gen[Chain[A]] =
    vectorOfR(range, ga).map(Chain.fromSeq)
}
