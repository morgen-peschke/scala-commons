package peschke.numeric

/** Provides the maximum and minimum values for a bounded type.
  *
  * This comes in handy for things like generators, and is not provided by
  * [[Numeric]] or [[Integral]]
  */
trait Bounded[A] {
  def maximum: A
  def minimum: A
}
object Bounded {
  def apply[A](implicit A: Bounded[A]):   A.type = A
  def minimum[A](implicit A: Bounded[A]): A      = A.minimum
  def maximum[A](implicit A: Bounded[A]): A      = A.maximum

  def from[A](min: A, max: A): Bounded[A] = new Bounded[A] with Serializable {
    override val maximum: A = max

    override val minimum: A = min
  }

  implicit val IntIsBounded: Bounded[Int] =
    from(Int.MinValue, Int.MaxValue)
  implicit val CharIsBounded: Bounded[Char] =
    from(Char.MinValue, Char.MaxValue)
  implicit val LongIsBounded: Bounded[Long] =
    from(Long.MinValue, Long.MaxValue)
  implicit val ShortIsBounded: Bounded[Short] =
    from(Short.MinValue, Short.MaxValue)
  implicit val FloatIsBounded: Bounded[Float] =
    from(Float.MinValue, Float.MaxValue)
  implicit val DoubleIsBounded: Bounded[Double] =
    from(Double.MinValue, Double.MaxValue)
}
