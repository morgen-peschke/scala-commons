package peschke

import cats.{Applicative, Functor}
import cats.syntax.all._

/** Provides a more principled and visible way of managing implicit conversions.
  */
trait Convertible[In, Out] {
  def convert(in: In): Out

  /** Derive a [[Convertible]] by modifying the value returned from
    * [[Convertible.convert()]]
    */
  def map[Result](f: Out => Result): Convertible[In, Result] = in =>
    f(convert(in))

  /** Derive a [[Convertible]] by modifying the input to
    * [[Convertible.convert()]]
    */
  def contraMap[Input](f: Input => In): Convertible[Input, Out] = in =>
    convert(f(in))
}

trait ConvertibleLifts {
  implicit final def ApplicativeLift[F[_]: Applicative, A, B]
    (implicit AB: Convertible[A, B])
    : Convertible[A, F[B]] =
    AB.map(_.pure[F])

  implicit final def FunctorLift[F[_]: Functor, A, B]
    (implicit AB: Convertible[A, B])
    : Convertible[F[A], F[B]] =
    _.map(AB.convert)
}
object Convertible extends ConvertibleLifts {
  def apply[I, O](implicit C: Convertible[I, O]): C.type = C

  object syntax {
    implicit final class ScalaCommonsConvertibleOps[In](private val in: In)
        extends AnyVal {
      def convert[Out](implicit C: Convertible[In, Out]): Out = C.convert(in)
      def convertOpt[Out](implicit C: Convertible[In, Option[Out]])
        : Option[Out] = C.convert(in)
    }
  }

  // These are handy because Numeric does not provide a standard conversion to BigInt
  implicit final val IntToBigInt:  Convertible[Int, BigInt]  = BigInt(_)
  implicit final val LongToBigInt: Convertible[Long, BigInt] = BigInt(_)
  implicit final val CharToBigInt: Convertible[Char, BigInt] =
    IntToBigInt.contraMap(_.toInt)
  implicit final val ShortToBigInt: Convertible[Short, BigInt] =
    IntToBigInt.contraMap(_.toInt)

  implicit final val BigIntToBigInt: Convertible[BigInt, BigInt] = identity(_)
  implicit final val BigIntToInt: Convertible[BigInt, Option[Int]] = b =>
    if (b.isValidInt) Some(b.toInt) else None
  implicit final val BigIntToLong: Convertible[BigInt, Option[Long]] = b =>
    if (b.isValidLong) Some(b.toLong) else None
  implicit final val BigIntToChar: Convertible[BigInt, Option[Char]] = b =>
    if (b.isValidChar) Some(b.toChar) else None
  implicit final val BigIntToShort: Convertible[BigInt, Option[Short]] = b =>
    if (b.isValidShort) Some(b.toShort) else None
}
