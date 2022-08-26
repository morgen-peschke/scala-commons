package peschke.python

import cats.ApplicativeError
import cats.Order
import cats.Show
import cats.data.EitherNec
import cats.data.NonEmptyChain
import cats.data.ValidatedNec
import cats.parse.Numbers
import cats.parse.Parser
import cats.parse.Parser.Expectation
import cats.parse.Parser0
import cats.syntax.all._
import peschke.python.Slice._
import peschke.python.SliceParser.ParseError._

/** Parse a [[peschke.python.Slice]], using modified Python slice syntax.
  *
  * The primary modification is that the indices must be integers, rather than
  * expressions.
  *
  * Standard implementation is [[SliceParser.default]]
  */
trait SliceParser[F[_]] {

  /** Parse a braced slice, anchored to the start and end of the string
    */
  def parse(raw: String): F[Slice]

  /** Parse a braced slice, only anchored to the start of the string
    */
  def parsePrefix(raw: String): F[(Slice, String)]

  /** Parse a raw slice (e.g `1:10:2`), anchored to the start and end of the
    * string
    */
  def parseUnbraced(raw: String): F[Slice]

  /** Parse a raw slice (e.g `1:10:2`), only anchored to the start of the string
    */
  def parseUnbracedPrefix(raw: String): F[(Slice, String)]
}

object SliceParser {

  object ErrorContext extends supertagged.NewType[String] {

    private[python] def extract(start: Int, length: Int, raw: String): Type = {
      val len                      = length.max(3)
      val (before, targetAndAfter) = raw.splitAt(start)
      val (target, after)          = targetAndAfter.splitAt(1)

      val rawPrefix = before.takeRight(len)
      val prefix =
        if (rawPrefix.length <= len) rawPrefix
        else s"...${before.takeRight(len - 3)}"

      val rawSuffix = after.take(len + 1)
      val suffix =
        if (rawSuffix.length <= len) rawSuffix
        else s"${after.take(len - 3)}..."

      apply(s"$prefix<$target>$suffix")
    }

    private[SliceParser] object Names {
      val OpenBrace  = "open-brace"
      val CloseBrace = "close-brace"
      val Number     = "number"
      val Separator  = "separator"
      val StartGiven = "start-given"
      val StartSkip  = "start-skipped"
      val EndGiven   = "end-given"
      val EndSkip    = "end-skipped"
      val StepGiven  = "step-given"
      val StepSkip   = "step-skipped"
      val StepIsZero = "step-zero"
    }
  }

  type ErrorContext = ErrorContext.Type

  object Index extends supertagged.NewType[Int]
  type Index = Index.Type

  sealed abstract class ParseError extends Product with Serializable {
    def index:   Index
    def context: ErrorContext
  }

  object ParseError {
    type OrRight[A] = EitherNec[ParseError, A]
    type OrValid[A] = ValidatedNec[ParseError, A]

    final case class ExpectedOpenBrace
      (brace: String, index: Index, context: ErrorContext)
        extends ParseError
    final case class ExpectedCloseBrace
      (brace: String, index: Index, context: ErrorContext)
        extends ParseError
    final case class ExpectedEndOfString(index: Index, context: ErrorContext)
        extends ParseError
    final case class ExpectedNumber(index: Index, context: ErrorContext)
        extends ParseError
    final case class ExpectedSeparator(index: Index, context: ErrorContext)
        extends ParseError
    final case class UnexpectedInput(index: Index, context: ErrorContext)
        extends ParseError
    final case class StepCannotBeZero(index: Index, context: ErrorContext)
        extends ParseError

    private def priority(pe: ParseError): Int = pe match {
      case ExpectedOpenBrace(_, _, _)  => 1
      case ExpectedCloseBrace(_, _, _) => 2
      case ExpectedEndOfString(_, _)   => 3
      case ExpectedNumber(_, _)        => 4
      case ExpectedSeparator(_, _)     => 5
      case UnexpectedInput(_, _)       => 6
      case StepCannotBeZero(_, _)      => 7
    }

    implicit val order: Order[ParseError] =
      Order.by(pe => (Index.raw(pe.index), priority(pe)))
    implicit val show: Show[ParseError] = Show.show {
      case ExpectedOpenBrace(brace, index, context) =>
        s"$index :: expected '$brace' at: $context"
      case ExpectedCloseBrace(brace, index, context) =>
        s"$index :: expected '$brace' at: $context"
      case ExpectedEndOfString(index, context) =>
        s"$index :: expected string to end at: $context"
      case ExpectedNumber(index, context) =>
        s"$index :: expected number at: $context"
      case ExpectedSeparator(index, context) =>
        s"$index :: expected ':' at: $context"
      case UnexpectedInput(index, context) =>
        s"$index :: unexpected input at: $context"
      case StepCannotBeZero(index, context) =>
        s"$index :: step cannot be zero at: $context"
    }
  }

  def default[F[_]](openBrace:   String, closeBrace: String, contextLength: Int)
                   (implicit AE: ApplicativeError[F, NonEmptyChain[ParseError]])
    : SliceParser[F] = new SliceParser[F] {
    private val indexParser: Parser[Long] =
      Numbers
        .signedIntString
        .mapFilter { raw =>
          Either.catchOnly[NumberFormatException](raw.toLong).toOption
        }
        .withContext(ErrorContext.Names.Number)

    private val openBraceParser: Parser[Unit] =
      Parser.string(openBrace).withContext(ErrorContext.Names.OpenBrace)
    private val closeBraceParser: Parser[Unit] =
      Parser.string(closeBrace).withContext(ErrorContext.Names.CloseBrace)

    private val unbracedSliceParser: Parser0[Slice] = {
      import ErrorContext.Names._

      def opt[A](parser: Parser[A], someContext: String, noneContext: String)
        : Parser0[Option[A]] =
        Parser.oneOf0(
          parser.map(_.some).withContext(someContext) ::
            Parser.pure(none[A]).withContext(noneContext) ::
            Nil
        )

      val separator: Parser[Unit] = Parser.char(':').withContext(Separator)

      val startP: Parser0[Option[Long]] = opt(indexParser, StartGiven, StartSkip)
      val endP: Parser0[Option[Long]] = opt(indexParser, EndGiven, EndSkip)
      val stepP: Parser0[Option[Long]] =
        opt(indexParser, StepGiven, StepSkip).flatMap {
          case Some(0L) => Parser.fail.withContext(StepIsZero)
          case nonZero  => Parser.pure(nonZero)
        }

      val stepExp: Parser[Long] = (separator *> stepP).map(_.getOrElse(1))

      ((startP <* separator) ~ (endP ~ stepExp.?).?)
        .map {
          case (Some(index), None) => At(index)
          case (startOpt, Some((endOpt, stepOpt))) =>
            Slice(startOpt, endOpt, stepOpt)
          case (startOpt, None) => Slice(startOpt, None, None)
        }
    }

    private val bracedSliceParser: Parser0[Slice] =
      unbracedSliceParser.between(openBraceParser, closeBraceParser)

    private val unbracedIndexParser: Parser[Slice] = indexParser.map(At)

    private val bracedIndexParser: Parser[Slice] =
      unbracedIndexParser.between(openBraceParser, closeBraceParser)

    private def adaptExpectation(input: String)(expectation: Parser.Expectation)
      : NonEmptyChain[ParseError] = {
      import ErrorContext.Names._
      def index: Index = Index(expectation.offset)
      def context: ErrorContext =
        ErrorContext.extract(expectation.offset, contextLength, input)

      NonEmptyChain
        .fromSeq(expectation.context.flatMap {
          case OpenBrace => ExpectedOpenBrace(openBrace, index, context) :: Nil
          case CloseBrace =>
            ExpectedCloseBrace(closeBrace, index, context) :: Nil
          case Number | StartGiven | EndGiven | StepGiven =>
            ExpectedNumber(index, context) :: Nil
          case Separator | StartSkip | EndSkip | StepSkip =>
            ExpectedSeparator(index, context) :: Nil
          case StepIsZero =>
            // Because this parser fails during post-parse validation, the offset needs to be adjusted
            // for the error to make sense to the user.
            StepCannotBeZero(
              Index(expectation.offset - 1),
              ErrorContext.extract(expectation.offset - 1, contextLength, input)
            ) :: Nil
          case _ => Nil
        }).getOrElse(NonEmptyChain.one {
          expectation match {
            case Expectation.EndOfString(_, _) =>
              ExpectedEndOfString(index, context)
            case _ => UnexpectedInput(index, context)
          }
        })
    }

    private def adaptError(input: String)(error: Parser.Error)
      : NonEmptyChain[ParseError] =
      NonEmptyChain
        .fromNonEmptyList(error.expected)
        .flatMap(adaptExpectation(input))
        .distinct

    private def selectBracedParser(raw: String): Parser0[Slice] =
      if (raw.contains(':')) bracedSliceParser else bracedIndexParser

    private def selectUnbracedParser(raw: String): Parser0[Slice] =
      if (raw.contains(':')) unbracedSliceParser else unbracedIndexParser

    override def parse(raw: String): F[Slice] =
      AE.fromEither(
        selectBracedParser(raw).parseAll(raw).leftMap(adaptError(raw))
      )

    override def parsePrefix(raw: String): F[(Slice, String)] =
      AE.fromEither(
        selectBracedParser(raw).parse(raw).map(_.swap).leftMap(adaptError(raw))
      )

    override def parseUnbraced(raw: String): F[Slice] =
      AE.fromEither(
        selectUnbracedParser(raw).parseAll(raw).leftMap(adaptError(raw))
      )

    override def parseUnbracedPrefix(raw: String): F[(Slice, String)] =
      AE.fromEither(
        selectUnbracedParser(raw).parse(raw).map(_.swap).leftMap(adaptError(raw))
      )
  }
}
