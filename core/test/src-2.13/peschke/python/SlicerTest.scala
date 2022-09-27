package peschke.python

import cats.Foldable
import cats.data.Chain
import cats.syntax.all._
import org.python.util.PythonInterpreter
import org.scalacheck.Gen
import org.scalatest.Inside
import org.scalatestplus.scalacheck.{ScalaCheckDrivenPropertyChecks => GenChecks}
import peschke.TableSpec
import peschke.python.Slicer.syntax._
import peschke.scalacheck.syntax._

import java.lang.{Integer => JInt}
import scala.jdk.CollectionConverters._
import scala.reflect.runtime.universe.WeakTypeTag

class SlicerTest extends TableSpec with SliceTestSyntax {

  "Slicer.indices" should {
    // Cases adapted from https://github.com/python/cpython/blob/main/Lib/test/seq_tests.py
    "produce the expected indices" in {
      val slicesToRanges = Table(
        ("slice", "range"),
        ("[0:0]", 0L until 0L),
        ("[1:1]", 1L until 1L),
        ("[1:2]", 1L until 2L),
        ("[-2:-1]", 23L until 24L),
        ("[-10:10]", 15L until 10L),
        ("[10:-10]", 10L until 15L),
        ("[:]", 0L until 25L),
        ("[1:]", 1L until 25L),
        ("[:3]", 0L until 3L),
        // Extended slices
        ("[::]", 0L until 25L),
        ("[::2]", 0L until 25L by 2L),
        ("[1::2]", 1L until 25L by 2L),
        ("[::-1]", 24L until -1L by -1L),
        ("[::-2]", 24L until -1L by -2L),
        ("[3::-2]", 3L until -1L by -2L),
        ("[3:3:-2]", 3L until 3L by -2L),
        ("[3:2:-2]", 3L until 2L by -2L),
        ("[3:1:-2]", 3L until 1L by -2L),
        ("[3:0:-2]", 3L until 0L by -2L),
        ("[::-10]", 24L until -1L by -10L),
        ("[10:-10:]", 10L until 15L),
        ("[-10:10:]", 20L until 10L),
        ("[10:-10:-1]", 10L until 15L by -1L),
        ("[-10:10:-1]", 15L until 10L by -1L),
        ("[-10:10:2]", 15L until 10L by 2L)
      )

      forAll(slicesToRanges) { (slice, range) =>
        Slicer.indices(slice.parseSlice, 25L) mustBe range
      }
    }

    "match the reference implementation" in
      GenChecks.forAll {
        SlicerTest
          .scalaAndPythonSlices(
            (0 to 20)
              .choose
              .gen
              .vector(0 to 20)
          )
          .map(_.map(_.length))
      } { case ((startOpt, endOpt, stepOpt), length) =>
        val scalaResult = Slicer.indices(
          Slice(
            startOpt.map(_.toLong),
            endOpt.map(_.toLong),
            stepOpt.map(_.toLong)
          ),
          length
        )

        val interpreter = new PythonInterpreter();
        interpreter.set("start", startOpt.map(JInt.valueOf).orNull)
        interpreter.set("end", endOpt.map(JInt.valueOf).orNull)
        interpreter.set("step", stepOpt.map(JInt.valueOf).orNull)
        interpreter.set("length", JInt.valueOf(length))
        interpreter.exec(
          "(rStart, rEnd, rStep) = slice(start,end,step).indices(length)"
        )
        val rStart = interpreter.get("rStart").asInt.toLong
        val rEnd = interpreter.get("rEnd").asInt.toLong
        val rStep = interpreter.get("rStep").asInt.toLong
        val pythonResult = rStart until rEnd by rStep

        Inside.inside((scalaResult, pythonResult)) { case (actual, expected) =>
          actual mustBe expected
        }
      }
  }

  "Slicer[Chain].in" should {
    val input = Chain(0, 1, 2, 3, 4)
    val slicesToElements = Table(
      ("slice", "expected"),
      ("[0:0]", Chain.empty),
      ("[1:1]", Chain.empty),
      ("[1:2]", Chain(1)),
      ("[-2:-1]", Chain(3)),
      ("[-1000:1000]", Chain(0, 1, 2, 3, 4)),
      ("[1000:-1000]", Chain.empty),
      ("[:]", Chain(0, 1, 2, 3, 4)),
      ("[1:]", Chain(1, 2, 3, 4)),
      ("[:3]", Chain(0, 1, 2)),
      // Extended slices
      ("[::]", Chain(0, 1, 2, 3, 4)),
      ("[::2]", Chain(0, 2, 4)),
      ("[1::2]", Chain(1, 3)),
      ("[::-1]", Chain(4, 3, 2, 1, 0)),
      ("[::-2]", Chain(4, 2, 0)),
      ("[3::-2]", Chain(3, 1)),
      ("[3:3:-2]", Chain.empty),
      ("[3:2:-2]", Chain(3)),
      ("[3:1:-2]", Chain(3)),
      ("[3:0:-2]", Chain(3, 1)),
      ("[::-100]", Chain(4)),
      ("[100:-100:]", Chain.empty),
      ("[-100:100:]", Chain(0, 1, 2, 3, 4)),
      ("[100:-100:-1]", Chain(4, 3, 2, 1, 0)),
      ("[-100:100:-1]", Chain.empty),
      ("[-100:100:2]", Chain(0, 2, 4))
    )

    "produce the expected values for the input" in forAll(slicesToElements) { (slice, expected) =>
      input.in(slice.parseSlice) mustBe expected
    }
  }

  def checkAgainstReference[C[_]]
    (targets:     Gen[C[Int]])
    (implicit tt: WeakTypeTag[C[_]], S: Slicer[C], F: Foldable[C])
    : Unit = {
    s"Slicer[${tt.tpe}].in" should {
      "conform to the reference implementation" in
        GenChecks.forAll(SlicerTest.scalaAndPythonSlices(targets)) { case ((startOpt, endOpt, stepOpt), scalaTarget) =>
          val scalaResult = scalaTarget.in(
            Slice(
              startOpt.map(_.toLong),
              endOpt.map(_.toLong),
              stepOpt.map(_.toLong)
            )
          )

          val interpreter = new PythonInterpreter();
          interpreter.set(
            "target",
            scalaTarget.toList.map(JInt.valueOf).toArray
          )
          interpreter.set("start", startOpt.map(JInt.valueOf).orNull)
          interpreter.set("end", endOpt.map(JInt.valueOf).orNull)
          interpreter.set("step", stepOpt.map(JInt.valueOf).orNull)
          interpreter.exec("result = target[start:end:step]")
          val pythonResult =
            interpreter
              .get("result")
              .asIterable()
              .asScala
              .map(_.asInt)
              .toList

          scalaResult.toList mustBe pythonResult
        }
    }
  }

  checkAgainstReference[Chain]((0 to 20).choose.gen.chain(0 to 20))
  checkAgainstReference[List]((0 to 20).choose.gen.list(0 to 20))
  checkAgainstReference[Vector]((0 to 20).choose.gen.vector(0 to 20))
}
object SlicerTest {
  def scalaAndPythonSlices[C[_]: Foldable](targets: Gen[C[Int]]): Gen[((Option[Int], Option[Int], Option[Int]), C[Int])] =
    targets.flatMap { target =>
      val targetRange = target.toList.indices
      val wideRange =
        -targetRange.start to targetRange.end * 2 by targetRange.step
      val inRange =
        for {
          startOpt <- targetRange.choose.optional
          endOpt   <- targetRange.choose.optional
          stepOpt  <- targetRange.choose.map(_.max(1)).optional
        } yield (startOpt, endOpt, stepOpt)

      val maybeOutOfRange =
        for {
          startOpt <- wideRange.choose.optional
          endOpt   <- wideRange.choose.optional
          stepOpt  <- wideRange.choose.map(s => if (s === 0) 1 else s).optional
        } yield (startOpt, endOpt, stepOpt)

      Gen.oneOf(inRange, maybeOutOfRange).map(_ -> target)
    }
}
