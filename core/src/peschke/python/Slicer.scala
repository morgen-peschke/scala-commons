package peschke.python

import cats.data.Chain

import scala.annotation.tailrec
import scala.collection.immutable.NumericRange

/**
 * A typeclass describing the ability to be accessed using a [[Slice]]
 */
trait Slicer[C[_]] {
  /**
   * Return the elements described by `slice`
   *
   * Should return identical results as `foo[start:end:step]`
   */
  def in[A](slice: Slice, ca: C[A]): C[A]
}

object Slicer {
  def apply[C[_]](implicit I: Slicer[C]): I.type = I

  /**
   * Return the indices described by `slice`
   *
   * Should return identical results as `slice(start,end,step).indices(maxIndex)`
   */
  def indices(slice: Slice, maxIndex: Long): NumericRange[Long] = {
    val step = slice.step
    val reversed = step < 0

    val start =
      slice
        .startOpt
        .map { s =>
          (if (s >= 0L) s else maxIndex + s).max(0L)
        }
        .getOrElse(if (reversed) maxIndex - 1L else 0L)

    val end =
      slice
        .endOpt
        .map { e =>
          (if (e >= 0L) e else maxIndex + e).max(-1L)
        }
        .getOrElse(if (reversed) -1L else maxIndex)
        .min(maxIndex)

    start until end by step
  }

  object syntax {
    implicit final class SlicerOps[C[_], E](private val ce: C[E]) extends AnyVal {
      def in(slice: Slice)(implicit S: Slicer[C]): C[E] = S.in(slice, ce)
    }
  }

  implicit val chainSlicer: Slicer[Chain] = new Slicer[Chain] {
    override def in[A](slice: Slice, ca: Chain[A]): Chain[A] = {
      val indexes = indices(slice, ca.length)
      if (indexes.isEmpty) Chain.empty[A]
      else {
        val combine: (Chain[A], A) => Chain[A] =
          if (indexes.step < 0) (_: Chain[A]).prepend(_: A)
          else (_: Chain[A]).append(_: A)

        @tailrec
        def loop(pending: Chain[A], accum: Chain[A], currentIndex: Long): Chain[A] =
          pending.uncons match {
            case None => accum
            case Some((element, rest)) if indexes.contains(currentIndex) =>
              loop(rest, combine(accum, element), currentIndex + 1L)

            case Some((_, rest)) => loop(rest, accum, currentIndex + 1L)
          }

        loop(ca, Chain.empty, 0L)
      }
    }
  }

  implicit val listSlicer: Slicer[List] = new Slicer[List] {
    override def in[A](slice: Slice, ca: List[A]): List[A] =
      chainSlicer.in(slice, Chain.fromSeq(ca)).toList
  }

  implicit val vectorSlicer: Slicer[Vector] = new Slicer[Vector] {
    override def in[A](slice: Slice, ca: Vector[A]): Vector[A] =
      chainSlicer.in(slice, Chain.fromSeq(ca)).toVector
  }
}