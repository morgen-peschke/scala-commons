package peschke.testing

import cats.Show

/** Bundles the various bits of source location information needed to interface with testing libraries.
  *
  * This makes them easier to pass along without accidentally resetting the source location.
  */
final case class At(path: sourcecode.File, name: sourcecode.FileName, line: sourcecode.Line) {
  override def toString: String = s"${path.value}:${line.value}"
}

object At {
  def here(implicit filePath: sourcecode.File, fileName: sourcecode.FileName, line: sourcecode.Line): At =
    At(filePath, fileName, line)

  implicit val show: Show[At] = Show.fromToString

  /** A typeclass that encodes the conversion between [[At]] and equivalent classes specific to each test framework
    *
    * Examples of this would be `munit.Location` or `org.scalactic.source.Position`
    */
  trait To[A] {
    def from(at: At): A
  }

  object To {
    implicit val toAt: At.To[At] = at => at
  }
}
