package peschke.munit

import munit.Location
import peschke.testing.At
import peschke.testing.Fail
import peschke.testing.ValueExtractor
import sourcecode.Text

/** Provides instances so [[peschke.testing.ValueExtractor.Syntax]] can be used
  * in munit tests
  */
trait MUnitValueExtractors extends ValueExtractor.Syntax[Location] {
  _: munit.Assertions =>
  implicit val failUsingMunitAssertions: Fail[Location] = new Fail[Location] {
    override def apply[A](msg: String, sourceInfo: Text[A], location: Location)
      : Nothing = {
      fail(s"${sourceInfo.source} $msg")(location)
    }

    override def apply[A]
      (msg: String, sourceInfo: Text[A], cause: Throwable, location: Location)
      : Nothing = {
      fail(s"${sourceInfo.source} $msg", cause)(location)
    }
  }

  implicit val AtToLocation: At.To[Location] = at =>
    new Location(at.path.value, at.line.value)
}
object MUnitValueExtractors extends MUnitValueExtractors with munit.Assertions
