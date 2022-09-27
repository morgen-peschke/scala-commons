# Scala Commons: ScalaCheck

Instances and utilities for the [ScalaCheck](https://scalacheck.org/) property-based testing
framework.

## Artifacts

### SBT
```
"com.github.morgen-peschke" % "commons-scalacheck" % commonsVersion
```

### Mill
```
ivy"com.github.morgen-peschke::commons-scalacheck:$commonsVersion"
```

## Documentation

### `peschke.scalacheck.syntax`

Most of the functionality in this module is surfaced here.

- `Range#choose` is a more flexible alternative to `Gen.chooseNum`
  This is particularly notable when the `Range` has a `step` value other than 1, at which point these are roughly
  equivalent:

  ```scala
  (a until b by c).choose
  Gen.chooseNum(0, ((b - a) - 1) / c).map(s => a + (c * s))
  ```
  
- `Range#chooseSteps` generates a value in the range `[0, range.length)`
- `Range#slices` generates subsets of a reference `Range`
- `Gen#optional` and `Gen#zip` forward to the static methods provided on `Gen`, and can be simpler to use in some cases
- `Gen#gen` provides an entry point for custom behavior, often using the base `Gen` to build collections of various 
  kinds through repeated application.

### `peschke.scalacheck.Combinators`

These are similar to `Gen#listOfN`, however they are specialized to work with `Range`
rather than a fixed length. This can make specifying `Gen` instances considerably less
noisy.

### `peschke.scalacheck.RangeableGen`

This typeclass provides much of the backbone of `Combinators` and `RangeGens`

### `peschke.scalacheck.RangeGens` and `peschke.scalacheck.NumericRangeGens`

These provide various flavors of `Gen` for `Range` and `NumericRange`.