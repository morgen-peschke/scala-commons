package peschke.python

import cats.data.NonEmptyChain
import cats.syntax.all._
import org.scalacheck.Arbitrary
import org.scalacheck.Gen
import org.scalatest.matchers.Matcher
import peschke.PropSpec
import peschke.python.Slice._
import peschke.python.SliceParser.ErrorContext
import peschke.python.SliceParser.Index
import peschke.python.SliceParser.ParseError
import peschke.python.SliceParserTest._
import peschke.scalacheck.syntax._

class SliceParserTest extends PropSpec {
  private val rangeParser = SliceParser.default[ParseError.OrRight](
    openBrace = "[",
    closeBrace = "]",
    contextLength = 5
  )

  val parse: Matcher[Test] = Matcher { test =>
    be(test.range.asRight).apply(rangeParser.parse(test.raw))
  }

  val failToParse: Matcher[(String, ParseError)] = Matcher {
    case (input, expected) =>
      be(NonEmptyChain(expected).asLeft).apply(rangeParser.parse(input))
  }

  val parsePrefix: Matcher[(Test, String)] = Matcher {
    case (test, suffix) =>
      be((test.range, suffix).asRight).apply(rangeParser.parsePrefix(test.raw))
  }

  val parseUnbraced: Matcher[Test] = Matcher { test =>
    be(test.range.asRight).apply(rangeParser.parseUnbraced(test.rawNoBraces))
  }

  val parseUnbracedPrefix: Matcher[(Test, String)] = Matcher {
    case (test, suffix) =>
      be((test.range, suffix).asRight)
        .apply(rangeParser.parseUnbracedPrefix(test.rawNoBraces))
  }

  // region RangeParser.parse

  property("RangeParser.parse should be able to parse an All") {
    forAll(allGen)(_ must parse)
  }

  property("RangeParser.parse should be able to parse a FromStart") {
    forAll(fromStartGen)(_ must parse)
  }

  property("RangeParser.parse should be able to parse a ToEnd") {
    forAll(toEndGen)(_ must parse)
  }

  property("RangeParser.parse should be able to parse a SubSlice") {
    forAll(subSliceGen)(_ must parse)
  }

  property("RangeParser.parse should be able to parse an At") {
    forAll(atGen)(_ must parse)
  }

  property("RangeParser.parse should not choke on valid input") {
    forAll(gensWithoutExpectations) { input =>
      rangeParser.parse(s"[$input]").value
    }
  }

  // endregion

  // region RangeParser.parsePrefix

  property("RangeParser.parsePrefix should be able to parse an All") {
    forAll(addSuffix(allGen))(_ must parsePrefix)
  }

  property("RangeParser.parsePrefix should be able to parse a FromStart") {
    forAll(addSuffix(fromStartGen))(_ must parsePrefix)
  }

  property("RangeParser.parsePrefix should be able to parse a ToEnd") {
    forAll(addSuffix(toEndGen))(_ must parsePrefix)
  }

  property("RangeParser.parsePrefix should be able to parse a SubSlice") {
    forAll(addSuffix(subSliceGen))(_ must parsePrefix)
  }

  property("RangeParser.parsePrefix should be able to parse an At") {
    forAll(addSuffix(atGen))(_ must parsePrefix)
  }

  property("RangeParser.parsePrefix should not choke on valid input") {
    forAll(gensWithoutExpectations, Gen.alphaChar.gen.string(0 to 10)) {
      (input, suffix) => rangeParser.parsePrefix(s"[$input]$suffix").value
    }
  }

  // endregion

  // region RangeParser.parseUnbraced

  property("RangeParser.parseUnbraced should be able to parse an All") {
    forAll(allGen)(_ must parseUnbraced)
  }

  property("RangeParser.parseUnbraced should be able to parse a FromStart") {
    forAll(fromStartGen)(_ must parseUnbraced)
  }

  property("RangeParser.parseUnbraced should be able to parse a ToEnd") {
    forAll(toEndGen)(_ must parseUnbraced)
  }

  property("RangeParser.parseUnbraced should be able to parse a SubSlice") {
    forAll(subSliceGen)(_ must parseUnbraced)
  }

  property("RangeParser.parseUnbraced should be able to parse an At") {
    forAll(atGen)(_ must parseUnbraced)
  }

  property("RangeParser.parseUnbraced should not choke on valid input") {
    forAll(gensWithoutExpectations) { input =>
      rangeParser.parseUnbraced(s"$input").value
    }
  }

  // endregion

  // region RangeParser.parseUnbracedPrefix

  property("RangeParser.parseUnbracedPrefix should be able to parse an All") {
    forAll(addSuffix(allGen))(_ must parseUnbracedPrefix)
  }

  property("RangeParser.parseUnbracedPrefix should be able to parse a FromStart") {
    forAll(addSuffix(fromStartGen))(_ must parseUnbracedPrefix)
  }

  property("RangeParser.parseUnbracedPrefix should be able to parse a ToEnd") {
    forAll(addSuffix(toEndGen))(_ must parseUnbracedPrefix)
  }

  property("RangeParser.parseUnbracedPrefix should be able to parse a SubSlice") {
    forAll(addSuffix(subSliceGen))(_ must parseUnbracedPrefix)
  }

  property("RangeParser.parseUnbracedPrefix should be able to parse an At") {
    forAll(addSuffix(atGen))(_ must parseUnbracedPrefix)
  }

  property("RangeParser.parseUnbracedPrefix should not choke on valid input") {
    forAll(gensWithoutExpectations, Gen.alphaChar.gen.string(0 to 10)) {
      (input, suffix) => rangeParser.parseUnbracedPrefix(s"$input$suffix").value
    }
  }

  // endregion

  property("Step cannot be zero") {
    forAll(slicesWithZeroStep)(_ must failToParse)
  }
}

object SliceParserTest {
  final case class Test(rawNoBraces: String, raw: String, range: Slice)

  object Test {
    def passing(rawNoBraces: String, range: Slice): Test =
      Test(rawNoBraces, s"[$rawNoBraces]", range)
  }

  def addSuffix(gt: Gen[Test]): Gen[(Test, String)] =
    for {
      test   <- gt
      suffix <- Gen.alphaChar.gen.string(0 to 10)
    } yield (Test(
               s"${test.rawNoBraces}$suffix",
               s"${test.raw}$suffix",
               test.range
             ),
             suffix
            )

  val stepGen: Gen[Int] =
    Gen.oneOf(Gen.chooseNum(Int.MinValue, -1), Gen.chooseNum(1, Int.MaxValue))

  val allGen: Gen[Test] =
    Gen.oneOf(
      Gen.const(Test.passing(":", All(1))),
      Gen.const(Test.passing("::", All(1))),
      stepGen.map { step => Test.passing(s"::$step", All(step)) }
    )

  val fromStartGen: Gen[Test] =
    Gen.oneOf(
      Arbitrary
        .arbitrary[Int].map(end => Test.passing(s":$end", FromStart(end, 1))),
      Arbitrary
        .arbitrary[Int].map(end => Test.passing(s":$end:", FromStart(end, 1))),
      for {
        end  <- Arbitrary.arbitrary[Int]
        step <- stepGen
      } yield Test.passing(s":$end:$step", FromStart(end, step))
    )

  val toEndGen: Gen[Test] =
    Gen.oneOf(
      Arbitrary
        .arbitrary[Int].map(start => Test.passing(s"$start:", ToEnd(start, 1))),
      Arbitrary
        .arbitrary[Int].map(start => Test.passing(s"$start::", ToEnd(start, 1))),
      for {
        start <- Arbitrary.arbitrary[Int]
        step  <- stepGen
      } yield Test.passing(s"$start::$step", ToEnd(start, step))
    )

  val subSliceGen: Gen[Test] =
    for {
      start <- Arbitrary.arbitrary[Int]
      end   <- Arbitrary.arbitrary[Int]
      step  <- stepGen
    } yield Test.passing(s"$start:$end:$step", SubSlice(start, end, step))

  val atGen: Gen[Test] =
    Arbitrary.arbitrary[Int].map { i => Test.passing(s"$i", At(i)) }

  val gensWithoutExpectations: Gen[String] =
    for {
      start    <- Arbitrary.arbitrary[Int].optional.map(_.fold("")(_.show))
      end      <- Arbitrary.arbitrary[Int].optional.map(_.fold("")(_.show))
      step     <- stepGen.optional.map(_.fold("")(_.show))
      collapse <- Arbitrary.arbitrary[Boolean]
    } yield {
      val raw = s"$start:$end:$step"
      if (collapse) raw.replace("::", ":")
      else raw
    }

  val slicesWithZeroStep: Gen[(String, ParseError)] =
    for {
      start <- Arbitrary.arbitrary[Int].optional.map(_.fold("")(_.show))
      end   <- Arbitrary.arbitrary[Int].optional.map(_.fold("")(_.show))
    } yield {
      val prefix    = s"[$start:$end:"
      val input     = s"${prefix}0]"
      val stepIndex = prefix.length
      input -> ParseError.StepCannotBeZero(
        Index(stepIndex),
        ErrorContext.extract(stepIndex, 5, input)
      )
    }
}
