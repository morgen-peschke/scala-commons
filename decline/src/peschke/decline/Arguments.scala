package peschke.decline

import cats.syntax.show._
import com.monovore.decline.Argument
import peschke.python

/**
 * Useful [[com.monovore.decline.Argument]] instances and helpers
 */
object Arguments {
  implicit val pythonSliceArgument: Argument[python.Slice] = {
    val parser = python.SliceParser.default[python.SliceParser.ParseError.OrValid]("[", "]", 5)
    Argument.from("[<start>:<end>:<step>]") { raw =>
      parser.parse(raw).leftMap(_.map(_.show).toNonEmptyList)
    }
  }g
}
