package peschke.python

import cats.syntax.all._

/** Represent a Python slice in Scala
  */
sealed abstract class Slice extends Product with Serializable {
  def startOpt: Option[Long] = this match {
    case Slice.All(_)                => none
    case Slice.FromStart(_, _)       => none
    case Slice.ToEnd(start, _)       => start.some
    case Slice.SubSlice(start, _, _) => start.some
    case Slice.At(index)             => index.some
  }

  def endOpt: Option[Long] = this match {
    case Slice.All(_)              => none
    case Slice.FromStart(end, _)   => end.some
    case Slice.ToEnd(_, _)         => none
    case Slice.SubSlice(_, end, _) => end.some
    case Slice.At(index)           => (index + 1L).some
  }
  def step: Long
}
object Slice {
  def apply(startOpt: Option[Long], endOpt: Option[Long], stepOpt: Option[Long]): Slice = {
    val step = stepOpt.getOrElse(1L)
    (startOpt, endOpt) match {
      case (Some(start), Some(end)) => SubSlice(start, end, step)
      case (Some(start), None)      => ToEnd(start, step)
      case (None, None)             => All(step)
      case (None, Some(end))        => FromStart(end, step)
    }
  }

  /** Equivalent to:
    * {{{
    * [:]
    * [::]
    * [::step]
    * }}}
    */
  final case class All(step: Long) extends Slice

  /** Equivalent to:
    * {{{
    * [:end]
    * [:end:]
    * [:end:step]
    * }}}
    */
  final case class FromStart(end: Long, step: Long) extends Slice

  /** Equivalent to:
    * {{{
    * [start:]
    * [start::]
    * [start::step]
    * }}}
    */
  final case class ToEnd(start: Long, step: Long) extends Slice

  /** Equivalent to:
    * {{{
    * [start:end:slice]
    * }}}
    */
  final case class SubSlice(start: Long, end: Long, step: Long) extends Slice

  /** Equivalent to:
    * {{{
    * [index]
    * }}}
    */
  final case class At(index: Long) extends Slice {
    override def step: Long = 1L
  }
}
