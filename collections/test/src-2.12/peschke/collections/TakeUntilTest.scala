package peschke.collections

import cats.syntax.eq._
import peschke.UnitSpec

class TakeUntilTest extends UnitSpec {
  import peschke.collections.TakeUntil.takeUntil
  "TakeUntil.takeUntil" should {
    "produce all elements until the predicate is true, and final value" in {
      val input: List[Int] = (0 to 10).toList
      val predicate: Int => Boolean = _ eqv 5
      takeUntil(input)(predicate) mustBe List(0, 1, 2, 3, 4, 5)
    }

    "produce the entire input, if the predicate is never true" in {
      val input: List[Int] = (0 to 10).toList
      val predicate: Int => Boolean = _ eqv 50
      takeUntil(input)(predicate) mustBe input
    }

    "not attempt to access past the point where the predicate becomes true" in {
      def wentTooFar: Stream[Int] = fail("Attempted to read too far")
      val input: Stream[Int] = 0 #:: 1 #:: 2 #:: 3 #:: wentTooFar
      val predicate: Int => Boolean = _ eqv 3
      takeUntil(input)(predicate).toList mustBe List(0, 1, 2, 3)
    }

    "infer types correctly" in {
      assertResult(true) {
        val input: List[Int] = (0 to 10).toList
        takeUntil(input)(5.max(_) =!= 5) eqv (List(0, 1, 2, 3, 4, 5, 6))
      }
    }
  }

  "TakeUntil.CanTakeUntil" should {
    import peschke.collections.TakeUntil.syntax._

    "infer types correctly" in {
      assertResult(true) {
        val input: List[Int] = (0 to 10).toList
        input.takeUntil(5.max(_) =!= 5) eqv List(0, 1, 2, 3, 4, 5, 6)
      }
    }
  }
}
