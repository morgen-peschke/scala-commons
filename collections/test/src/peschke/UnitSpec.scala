package peschke

import org.scalacheck.Shrink
import org.scalatest.EitherValues
import org.scalatest.OptionValues
import org.scalatest.matchers.must.Matchers
import org.scalatest.propspec.AnyPropSpec
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks

trait UnitSpec extends AnyWordSpec with Matchers with EitherValues with OptionValues

trait PropSpec extends AnyPropSpec with Matchers with EitherValues with OptionValues with ScalaCheckDrivenPropertyChecks {
  implicit def noShrink[A]: Shrink[A] = Shrink.shrinkAny
}
