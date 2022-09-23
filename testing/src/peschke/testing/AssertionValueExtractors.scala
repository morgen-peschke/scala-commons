package peschke.testing

import cats.syntax.show._

/** Provides instances so [[ValueExtractor.Syntax]] can be used with raw
  * assertions.
  *
  * This is mostly a bridge for when fully adapting [[ValueExtractor]] doesn't
  * make sense
  */
trait AssertionValueExtractors extends ValueExtractor.Syntax[At] {
  implicit val failUsingRawAssertions: Fail[At] = new Fail[At] {
    override def apply[A](msg: String, sourceInfo: sourcecode.Text[A], at: At)
      : Nothing =
      throw new AssertionError(show"$at ${sourceInfo.source} $msg") // scalafix:ok DisableSyntax.throw

    override def apply[A]
      (msg: String, sourceInfo: sourcecode.Text[A], cause: Throwable, at: At)
      : Nothing =
      throw new AssertionError(s"$at ${sourceInfo.source} $msg", cause) // scalafix:ok DisableSyntax.throw
  }
}
object AssertionValueExtractors extends AssertionValueExtractors
