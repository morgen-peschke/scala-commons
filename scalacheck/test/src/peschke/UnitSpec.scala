package peschke

import org.scalacheck.Shrink
import org.scalatest.EitherValues
import org.scalatest.OptionValues
import org.scalatest.matchers.must.Matchers
import org.scalatest.prop.TableDrivenPropertyChecks
import org.scalatest.propspec.AnyPropSpec
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks

trait CommonSpec extends Matchers with EitherValues with OptionValues

trait PropSpec
    extends AnyPropSpec
    with Matchers
    with EitherValues
    with OptionValues
    with ScalaCheckDrivenPropertyChecks
    with TableDrivenPropertyChecks {
  implicit def noShrink[T]: Shrink[T] = Shrink.shrinkAny
}
