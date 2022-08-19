package peschke.python

import cats.syntax.all._
import org.scalactic.source.Position
import org.scalatest.{Assertions, EitherValues}
import peschke.python.SliceParser.ParseError
import peschke.python.SliceParser.ParseError.OrRight
import peschke.python.SliceTestSyntax.SliceParserTestSyntax

import scala.language.implicitConversions

trait SliceTestSyntax {
  implicit def enableScalaCommonsSliceParserTestSyntax(raw: String): SliceParserTestSyntax =
    new SliceParserTestSyntax(raw)
}
object SliceTestSyntax extends EitherValues {
  val parser: SliceParser[OrRight] = SliceParser.default[ParseError.OrRight]("[", "]", 5)

  final class SliceParserTestSyntax(private val raw: String) extends AnyVal {
    def parseSlice(implicit P: Position): Slice = {
      parser.parse(raw).valueOr { error =>
       Assertions.fail(s"Unable to parse <<$raw>> as a Slice:\n  ${error.mkString_("\n  ")}")
      }
    }
  }
}