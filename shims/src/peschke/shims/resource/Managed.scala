package peschke.shims.resource

import cats.Applicative
import cats.Monad
import cats.syntax.all._

/** This is a poor replacement for cats-effect's `Resource`
  *
  * If you have access to cats-effect, use `Resource`. If you don't, this can serve as a shim.
  */
trait Managed[F[_], R] {

  /** Use the resource
    */
  def use[Out](f: R => F[Out]): F[Out]

  /** Get the resource
    *
    * This is handy when the bulk of the computations have been handled in `map` and this returns a computed value,
    * rather than the resource itself.
    *
    * This can be used to retrieve a resource, however no guarantees can be made WRT to the validity of the returned
    * value.
    */
  def value(implicit A: Applicative[F]): F[R] = use(_.pure[F])
}
object Managed {

  def factory[F[_], E]: Factory[F, E] = new Factory[F, E]()

  final class Factory[F[_], E](private val ignored: Boolean = true) extends AnyVal {

    /** Create a `Managed` instance that always returns the same resource.
      *
      * Note: because it always returns the same instance, it will never release this resource.
      */
    def pure[R](resource: R)(implicit EM: ExceptionMapper[F, E]): Managed[F, R] = new Pure[F, E, R](resource)

    /** Create a `Managed` instance that always returns a new resource.
      *
      * This is a convenience variant of [[Factory.alwaysF()]], which lifts the returned value into `F`
      */
    def always[R](open: => R)(implicit C: Closer[F, R], B: Bracket[F], EM: ExceptionMapper[F, E]): Managed[F, R] =
      new Resource[F, E, R](() => EM.catchNonFatal(open))

    /** Create a `Managed` instance that always returns a new resource.
      */
    def alwaysF[R](open: => F[R])(implicit C: Closer[F, R], B: Bracket[F], EM: ExceptionMapper[F, E]): Managed[F, R] =
      new Resource[F, E, R](() => open)
  }

  implicit def catsInstances[F[_], E](implicit EM: ExceptionMapper[F, E]): Monad[Managed[F, *]] =
    new Monad[Managed[F, *]] {
      override def pure[R](x: R): Managed[F, R] = new Pure[F, E, R](x)

      override def flatMap[R0, R1](fa: Managed[F, R0])(f: R0 => Managed[F, R1]): Managed[F, R1] =
        new FlatMapped[F, E, R0, R1](fa, f)

      override def map[R0, R1](fa: Managed[F, R0])(f: R0 => R1): Managed[F, R1] =
        new Mapped[F, E, R0, R1](fa, f)

      // Note: the call to tailRecM should be tail recursive, however the returned `Managed` may still blow the stack
      // when `use` is called.
      override def tailRecM[R0, R1](a: R0)(f: R0 => Managed[F, Either[R0, R1]]): Managed[F, R1] =
        flatMap(f(a)) {
          case Left(a)  => tailRecM(a)(f)
          case Right(b) => pure(b)
        }
    }

  final class Pure[F[_], E, R](resource: R)(implicit EM: ExceptionMapper[F, E]) extends Managed[F, R] {
    override def use[Out](f: R => F[Out]): F[Out] =
      EM.catchNonFatalF(f(resource))
  }

  final class Resource[F[_], E, R](open: () => F[R])(implicit C: Closer[F, R], B: Bracket[F], EM: ExceptionMapper[F, E])
      extends Managed[F, R] {
    override def use[Out](f: R => F[Out]): F[Out] =
      B.bracket[R, Out](
        () => EM.catchNonFatalF(open()),
        resource => EM.catchNonFatalF(f(resource)),
        resource => EM.catchNonFatalF(C.close(resource))
      )
  }

  final class Mapped[F[_], E, R0, R1](resource: Managed[F, R0], mapper: R0 => R1)(implicit EM: ExceptionMapper[F, E])
      extends Managed[F, R1] {
    override def use[Out](f: R1 => F[Out]): F[Out] =
      resource.use(r => EM.catchNonFatalF(f(mapper(r))))
  }

  final class FlatMapped[F[_], E, R0, R1]
    (resource: Managed[F, R0], flatMapper: R0 => Managed[F, R1])
    (implicit EM: ExceptionMapper[F, E])
      extends Managed[F, R1] {
    override def use[Out](f: R1 => F[Out]): F[Out] =
      resource.use(r0 => flatMapper(r0).use(r1 => EM.catchNonFatalF(f(r1))))
  }
}
