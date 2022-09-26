package peschke.scalatest

import org.scalactic.source.Position
import peschke.testing.At
import peschke.testing.Fail
import peschke.testing.ValueExtractor
import sourcecode.Text

/** Provides instances so [[peschke.testing.ValueExtractor.Syntax]] can be used in scalatest tests
  */
trait ScalaTestValueExtractors  extends ValueExtractor.Syntax[Position] {
  _: org.scalatest.Assertions =>
  implicit val failUsingScalaTestAssertions: Fail[Position] =
    new Fail[Position] {
      override def apply[A](msg: String, sourceInfo: Text[A], position: Position): Nothing = {
        fail(s"${position.filePathname}:${position.lineNumber} ${sourceInfo.source} $msg")(
          position
        )
      }

      override def apply[A](msg: String, sourceInfo: Text[A], cause: Throwable, position: Position): Nothing = {
        fail(
          s"${position.filePathname}:${position.lineNumber} ${sourceInfo.source} $msg",
          cause
        )(position)
      }
    }

  implicit val AtToPosition: At.To[Position] = at => new Position(at.name.value, at.path.value, at.line.value)
}
object ScalaTestValueExtractors extends ScalaTestValueExtractors with org.scalatest.Assertions
