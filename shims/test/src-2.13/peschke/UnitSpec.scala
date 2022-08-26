package peschke

import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.{EitherValues, OptionValues}

trait UnitSpec
    extends AnyWordSpec
    with Matchers
    with EitherValues
    with OptionValues
