package peschke.collections

import scala.annotation.tailrec
import scala.collection.generic.CanBuildFrom
import scala.collection.{TraversableLike, mutable}
import scala.language.higherKinds

/**
 * Provides a reversed alternative to [[scala.collection.GenTraversableLike.takeWhile]]
 *
 * Of particular interest is that [[TakeUntil.takeUntil]] includes the final value which
 * caused the predicate to evaluate to `true`.
 */
object TakeUntil {
  def takeUntil[E, C[X] <: TraversableLike[X, C[X]], That](source: C[E])
                                                          (p: E => Boolean)
                                                          (implicit cbf: CanBuildFrom[C[E], E, That]): That = {
    val builder: mutable.Builder[E, That] = cbf()
    @tailrec
    def loop(remaining: C[E]): That =
      remaining.headOption match {
        case None => builder.result()
        case Some(x) if p(x) =>
          builder += x
          builder.result()
        case Some(x) =>
          builder += x
          loop(remaining.drop(1))
      }

    loop(source)
  }

  object syntax {
    implicit class CanTakeUntil[E, C[X] <: TraversableLike[X, C[X]]](val source: C[E]) extends AnyVal {
      def takeUntil[That](p: E => Boolean)(implicit cbf: CanBuildFrom[C[E], E, That]): That =
        TakeUntil.takeUntil(source)(p)(cbf)
    }
  }
}
