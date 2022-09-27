package peschke.cats

import cats.Comparison
import cats.Eq
import cats.Order
import cats.data.Validated
import cats.syntax.all._
import cats.~>
import org.scalatest.prop.TableFor2
import peschke.TableSpec
import peschke.cats.syntax._
import peschke.cats.syntaxTest.Foo

class syntaxTest extends TableSpec {
  "F[G[A]].innerMap" should {
    "enable modifying the contained value" in {
      List(
        5.asRight,
        "ignored".asLeft,
        0.asRight
      ).innerMap(_ + 5) mustBe List(10.asRight, "ignored".asLeft, 5.asRight)
    }
  }

  "F[G[A]].innerFlatMap" should {
    "enable modifying the inner effect" in {
      List(
        5.asRight,
        "ignored".asLeft,
        0.asRight
      ).innerFlatMap(v => if (v eqv 0) "<0>".asLeft else v.asRight) mustBe List(
        5.asRight,
        "ignored".asLeft,
        "<0>".asLeft
      )
    }
  }

  "F[G[A]].mapK" should {
    "allow changing the outer effect, while retaining the inner effect" in {
      5.asRight[String].some.mapK(Lambda[Option ~> List](_.toList)) mustBe List(
        5.asRight[String]
      )

      List(5.asRight[String], "discarded".asLeft[Int])
        .mapK(Lambda[List ~> Option](_.headOption)) mustBe 5
        .asRight[String].some
    }
  }

  "F[G[A]].innerMapK" should {
    "allow changing the inner effect, while retaining the outer effect" in {
      List(
        5.asRight,
        "discarded".asLeft,
        0.asRight
      ).innerMapK(
        Lambda[Lambda[r => Either[String, r]] ~> Option](_.toOption)
      ) mustBe List(
        5.some,
        none,
        0.some
      )
    }
  }

  "Validated.andThenF" should {
    val t: TableFor2[Validated[String, Int], Int => Option[Validated[String, Int]]] =
      Table[Validated[String, Int], Int => Option[Validated[String, Int]]](
        ("input", "function"),
        (5.valid, _.valid.some),
        ("hi".invalid, _.valid.some),
        (5.valid, i => s"$i".invalid.some),
        ("hi".invalid, i => s"$i".invalid.some),
        (5.valid, _ => none),
        ("hi".invalid, _ => none)
      )

    "behave the same way as flatTraverse does for Either" in forAll(t) { (validatedInput, validatedFunction) =>
      val eitherInput: Either[String, Int] = validatedInput.toEither
      val eitherFunction: Int => Option[Either[String, Int]] =
        validatedFunction(_).map(_.toEither)

      val validatedOutput = validatedInput.andThenF(validatedFunction)
      val eitherOutput = eitherInput.flatTraverse(eitherFunction)

      validatedOutput.map(_.toEither) mustBe eitherOutput
      eitherOutput.map(_.toValidated) mustBe validatedOutput
    }
  }

  "Order.builder" should {
    "add subsequent instances as tiebreakers, in the correct order" in {
      val onlyS = Order.by[Foo, String](_.s)
      val onlyI = Order.builder[Foo].by(_.i).build
      val onlyIS = Order.builder[Foo].by(_.i).andThen(onlyS).build
      val all = Order.builder[Foo].by(_.i).by(_.s).by(_.c).build

      onlyI.comparison(
        Foo(4, "a", 'b'),
        Foo(4, "z", 'z')
      ) mustBe Comparison.EqualTo
      onlyI.comparison(
        Foo(4, "z", 'z'),
        Foo(5, "a", 'a')
      ) mustBe Comparison.LessThan
      onlyI.comparison(
        Foo(4, "a", 'a'),
        Foo(3, "z", 'z')
      ) mustBe Comparison.GreaterThan

      withClue("[Should defer to i]") {
        onlyIS.comparison(
          Foo(4, "z", 'z'),
          Foo(5, "a", 'a')
        ) mustBe Comparison.LessThan
        onlyIS.comparison(
          Foo(4, "a", 'a'),
          Foo(3, "z", 'z')
        ) mustBe Comparison.GreaterThan
      }
      onlyIS.comparison(
        Foo(4, "a", 'z'),
        Foo(4, "z", 'a')
      ) mustBe Comparison.LessThan
      onlyIS.comparison(
        Foo(4, "z", 'a'),
        Foo(4, "a", 'z')
      ) mustBe Comparison.GreaterThan
      onlyIS.comparison(
        Foo(4, "a", 'z'),
        Foo(4, "a", 'z')
      ) mustBe Comparison.EqualTo

      withClue("[Should defer to i]") {
        all.comparison(
          Foo(4, "z", 'z'),
          Foo(5, "a", 'a')
        ) mustBe Comparison.LessThan
        all.comparison(
          Foo(4, "a", 'a'),
          Foo(3, "z", 'z')
        ) mustBe Comparison.GreaterThan
      }
      withClue("[Should defer to s]") {
        all.comparison(
          Foo(4, "a", 'z'),
          Foo(4, "z", 'a')
        ) mustBe Comparison.LessThan
        all.comparison(
          Foo(4, "z", 'a'),
          Foo(4, "a", 'z')
        ) mustBe Comparison.GreaterThan
      }
      all.comparison(
        Foo(4, "a", 'a'),
        Foo(4, "a", 'z')
      ) mustBe Comparison.LessThan
      all.comparison(
        Foo(4, "a", 'z'),
        Foo(4, "a", 'a')
      ) mustBe Comparison.GreaterThan
      all.comparison(
        Foo(4, "a", 'a'),
        Foo(4, "a", 'a')
      ) mustBe Comparison.EqualTo
    }
  }

  "Eq.builder" should {
    "require all instances to agree something is equal to return true" in {
      val onlyS = Eq.by[Foo, String](_.s)
      val onlyI = Eq.builder[Foo].by(_.i).build
      val onlyIS = Eq.builder[Foo].by(_.i).and(onlyS).build
      val all = Eq.builder[Foo].by(_.i).by(_.s).by(_.c).build

      onlyI.eqv(Foo(4, "a", 'a'), Foo(3, "a", 'a')) mustBe false
      onlyI.eqv(Foo(4, "a", 'a'), Foo(4, "z", 'z')) mustBe true

      onlyIS.eqv(Foo(4, "a", 'a'), Foo(3, "a", 'a')) mustBe false
      onlyIS.eqv(Foo(4, "a", 'a'), Foo(4, "z", 'a')) mustBe false
      onlyIS.eqv(Foo(4, "a", 'z'), Foo(4, "a", 'a')) mustBe true

      all.eqv(Foo(4, "a", 'a'), Foo(3, "a", 'a')) mustBe false
      all.eqv(Foo(4, "a", 'a'), Foo(4, "z", 'a')) mustBe false
      all.eqv(Foo(4, "a", 'z'), Foo(4, "a", 'a')) mustBe false
      all.eqv(Foo(4, "a", 'a'), Foo(4, "a", 'a')) mustBe true
    }
  }
}
object syntaxTest {
  final case class Foo(i: Int, s: String, c: Char)
}
