package peschke.collections

import scala.collection.{AbstractView, BuildFrom}

/** Provides a reversed alternative to [[scala.collection.Iterator.takeWhile]]
  *
  * Of particular interest is that [[TakeUntil.takeUntil]] includes the final
  * value which caused the predicate to evaluate to `true`.
  */
object TakeUntil {
  def takeUntil[E, C[_] <: IterableOnce[_], Out]
    (source:      C[E])(stop:                  E => Boolean)
    (implicit bf: BuildFrom[C[E], E, Out], ev: C[E] <:< IterableOnce[E])
    : Out =
    bf.fromSpecific(source)(new AbstractView[E] {
      override def iterator: Iterator[E] = {
        Iterator.unfold[E, Iterator[E]](ev(source).iterator) { it =>
          it.nextOption() match {
            case None               => None
            case Some(x) if stop(x) => Some(x -> Iterator.empty)
            case Some(x)            => Some(x -> it)
          }
        }
      }
    })

  object syntax {
    // Can't be an AnyVal style extension method because there's no way to infer the element type without
    // breaking the invariant that Value Classes can only wrap a single value.
    class TakeUntilOps[E, C[_] <: IterableOnce[_]](source: C[E]) {
      def takeUntil[Out]
        (stop:        E => Boolean)
        (implicit bf: BuildFrom[C[E], E, Out], ev: C[E] <:< IterableOnce[E])
        : Out =
        TakeUntil.takeUntil[E, C, Out](source)(stop)(bf, ev)
    }

    import scala.language.implicitConversions

    implicit def takeUntilOp[E, C[_] <: IterableOnce[_]](source: C[E])
      : TakeUntilOps[E, C] =
      new TakeUntilOps[E, C](source)
  }
}
