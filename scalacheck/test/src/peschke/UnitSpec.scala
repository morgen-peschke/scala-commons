package peschke

import org.scalacheck.Shrink
import org.scalatest.EitherValues
import org.scalatest.OptionValues
import org.scalatest.matchers.must.Matchers
import org.scalatest.prop.TableDrivenPropertyChecks
import org.scalatest.propspec.AnyPropSpec
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks

trait CommonSpec extends Matchers with EitherValues with OptionValues

trait UnitSpec extends AnyWordSpec with CommonSpec

trait PropSpec
    extends AnyPropSpec
    with CommonSpec
    with ScalaCheckDrivenPropertyChecks {
  implicit def noShrink[T]: Shrink[T] = Shrink.shrinkAny
}

trait TableSpec
    extends AnyWordSpec
    with CommonSpec
    with TableDrivenPropertyChecks {
  implicit def noShrink[T]: Shrink[T] = Shrink.shrinkAny
}
