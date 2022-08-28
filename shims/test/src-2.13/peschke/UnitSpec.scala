package peschke

import org.scalatest.EitherValues
import org.scalatest.OptionValues
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec

trait UnitSpec
    extends AnyWordSpec
    with Matchers
    with EitherValues
    with OptionValues
