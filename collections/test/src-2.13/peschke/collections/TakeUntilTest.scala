package peschke.collections

import cats.syntax.eq._
import peschke.UnitSpec

class TakeUntilTest extends UnitSpec {
  import peschke.collections.TakeUntil.syntax._

  val input: List[Int] = (0 to 10).toList

  "TakeUntil.takeUntilOp" should {
    "produce all elements until the predicate is true, and final value" in {
      input.takeUntil(_ eqv 5) mustBe List(0, 1, 2, 3, 4, 5)
    }

    "produce the entire input, if the predicate is never true" in {
      input.takeUntil(_ eqv 50) mustBe input
    }

    "not attempt to access past the point where the predicate becomes true" in {
      def wentTooFar: LazyList[Int] = fail("Attempted to read too far")

      val input: LazyList[Int] = 0 #:: 1 #:: 2 #:: 3 #:: 4 #:: 5 #:: wentTooFar
      input.takeUntil(_ eqv 3).toList mustBe List(0, 1, 2, 3)
    }

    "be lazy for lazy collections" in {
      def wentTooFar: LazyList[Int] = fail("Was not lazy")

      val input: LazyList[Int] = 0 #:: 1 #:: wentTooFar

      input.takeUntil(_ eqv 3).headOption.value mustBe 0
    }

    "infer types correctly" in {
      assertResult(true) {
        input.takeUntil(5.max(_) =!= 5) eqv List(0, 1, 2, 3, 4, 5, 6)
      }
    }
  }
}
