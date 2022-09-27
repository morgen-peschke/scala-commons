package peschke.cats

import cats.Applicative
import cats.FlatMap
import cats.Functor
import cats.Order
import cats.data.Validated
import cats.kernel.Eq
import cats.syntax.all._
import cats.~>

object syntax {
  implicit final class ScalaCommonsCatsNestedFunctorOps[F[_], G[_], A](private val fga: F[G[A]]) extends AnyVal {

    /** Equivalent to `.map(_.map(_))`
      *
      * Handy for when you just need to do this once or twice, and a Transformer or [[cats.data.Nested]] would be
      * overkill.
      */
    def innerMap[B](ab: A => B)(implicit F: Functor[F], G: Functor[G]): F[G[B]] = fga.map(_.map(ab))

    /** Equivalent to `.map(_.flatMap(_))`
      *
      * Handy for when you just need to do this once or twice, and a Transformer or [[cats.data.Nested]] would be
      * overkill.
      */
    def innerFlatMap[B](aGb: A => G[B])(implicit F: Functor[F], G: FlatMap[G]): F[G[B]] = fga.map(_.flatMap(aGb))

    /** Basically [[cats.data.Nested.mapK]], but without the wrapper
      */
    def mapK[H[_]](fh: F ~> H): H[G[A]] = fh(fga)

    /** Basically [[mapK]], but operating on the inner `G` instead of the outer `F`
      */
    def innerMapK[H[_]](gh: G ~> H)(implicit F: Functor[F]): F[H[A]] =
      fga.map(ga => gh(ga))
  }

  implicit final class ScalaCommonsCatsValidatedOps[E, A](private val va: Validated[E, A]) extends AnyVal {

    /** [[cats.Traverse.flatTraverse]], but for [[Validated]]
      *
      * [[Validated]] doesn't have a [[FlatMap]] instance, so we can't use [[cats.Traverse.flatTraverse]] directly, but
      * sometimes that is something really, really handy.
      */
    def andThenF[F[_]: Applicative, B](f: A => F[Validated[E, B]]): F[Validated[E, B]] =
      va match {
        case Validated.Invalid(e) => e.invalid[B].pure[F]
        case Validated.Valid(a)   => f(a)
      }

    /** Exactly equivalent to [[Validated.traverse]]
      *
      * When paired with [[andThenF]], it can read better to use [[mapF]].
      */
    def mapF[F[_]: Applicative, B](f: A => F[B]): F[Validated[E, B]] =
      va.traverse(f)
  }

  implicit final class ScalaCommonsCatsOrderOps[A](private val order: Order[A]) extends AnyVal {

    /** Alias of [[cats.Order.reverse]], which can be a bit easier to chain.
      */
    def reversed: Order[A] = Order.reverse(order)
  }

  implicit final class ScalaCommonsCatsOrderObjOps(private val O: Order.type) extends AnyVal {

    /** Build an [[Order]] from a bunch of existing [[Order]] instances.
      *
      * Equivalent to using [[cats.Order.whenEqual]] to combine them all, but can be a bit easier to read and write
      * because of it's flat nature.
      *
      * {{{
      * final case class Foo(i: Int, s: String, f: Float)
      * implicit val order: Order[Foo] =
      *   Order
      *     .builder[Foo]
      *     .by(_.i)
      *     .andThen(Order.by(_.s).reversed)
      *     .by(_.f)
      *     .build
      * }}}
      */
    def builder[A]: OrderUsingEmptyBuilder[A] = new OrderUsingEmptyBuilder[A](O)
  }

  implicit final class ScalaCommonsCatsEqObjOps(private val E: Eq.type) extends AnyVal {

    /** Build an [[Eq]] from a bunch of existing [[Eq]] instances.
      *
      * Equivalent to using [[Eq.and]] to combine them all, but can be a bit easier to read and write because of it's
      * flat nature.
      *
      * {{{
      * final case class Foo(i: Int, s: String, f: Float)
      * implicit val eq: Eq[Foo] =
      *   Eq
      *     .builder[Foo]
      *     .by(_.i)
      *     .and(Eq.instance(_.s equalsIgnoreCase _.s))
      *     .by(_.f)
      *     .build
      * }}}
      */
    def builder[A]: EqUsingEmptyBuilder[A] = new EqUsingEmptyBuilder[A](E)
  }
}

final class OrderUsingEmptyBuilder[A](private val O: Order.type) extends AnyVal {

  /** Set the primary [[Order]] instance
    */
  def on(oa: Order[A]): OrderUsingBuilder[A] =
    new OrderUsingBuilder[A](oa)

  /** Sugar for `on(Order.by(fab))`
    */
  def by[B: Order](fab: A => B): OrderUsingBuilder[A] =
    on(O.by(fab))
}
final class OrderUsingBuilder[A](private val accum: Order[A]) extends AnyVal {

  /** Add an additional [[Order]] tie-breaker
    */
  def andThen(oa: Order[A]): OrderUsingBuilder[A] =
    new OrderUsingBuilder[A](Order.whenEqual(accum, oa))

  /** Sugar for `andThen(Order.by(fab))`
    */
  def by[B: Order](fab: A => B): OrderUsingBuilder[A] =
    andThen(Order.whenEqual(accum, Order.by(fab)))

  /** Unwrap the resulting [[Order]]
    */
  def build: Order[A] = accum
}

final class EqUsingEmptyBuilder[A](private val E: Eq.type) extends AnyVal {

  /** Set the primary [[Eq]] instance
    */
  def on(oa: Eq[A]): EqUsingBuilder[A] = new EqUsingBuilder[A](oa)

  /** Sugar for `on(Eq.by(fab))`
    */
  def by[B: Eq](fab: A => B): EqUsingBuilder[A] =
    new EqUsingBuilder[A](E.by(fab))
}
final class EqUsingBuilder[A](private val accum: Eq[A]) extends AnyVal {

  /** Add an additional [[Eq]] instance that must be satisfied
    */
  def and(oa: Eq[A]): EqUsingBuilder[A] =
    new EqUsingBuilder[A](Eq.and(accum, oa))

  /** Sugar for `and(Eq.by(fab))`
    */
  def by[B: Eq](fab: A => B): EqUsingBuilder[A] =
    new EqUsingBuilder[A](Eq.and(accum, Eq.by(fab)))

  /** Unwrap the resulting [[Eq]]
    */
  def build: Eq[A] = accum
}
