# scala-commons

Common utility libraries for Scala, mostly small stuff I don't want to have multiple copies of lying around.

## Installation

### SBT
```
libraryDependencies += Seq(
  "com.github.morgen-peschke" % "commons-core" % "0.0.1",
  "com.github.morgen-peschke" % "commons-collections" % "0.0.1"
)
```

### Mill
```
def ivyDeps = Agg(
  ivy"com.github.morgen-peschke::commons-core:0.0.1",
  ivy"com.github.morgen-peschke::commons-collections:0.0.1"
)
```

## SubModules

### `commons-core`

This is a home for the most generic of utilities.

#### `Complete`

A placeholder indicating completion, which provides a work-around for the type issues which can arise when returning
things like `Future[Unit]`. 

This is very similar to `akka.Done`, and exists as a lightweight alternative to the same.

#### `Slice`

A pure Scala, lightweight implementation of python's slice syntax (e.g `[start:stop:step]`). 

### `commons-collections`

Helpers and extensions for working with the Scala standard library.

#### `TakeUntil`

An alternative to `IterableOnce#takeWhile`, which is primarily differentiated on two points:
1. The elements are consumed until the predicate is true, rather than while the predicate is true, so the logic is 
reversed. 
2. The final element is also taken.

### `commons-scalacheck`

A collection of utilities and syntax to make working with Scalacheck `Gen` smoother.

Highlights include:
- `(0 to 10).choose` as a more flexible alternative to `Gen.chooseNum(0, 10)`
  This is particularly handy because `(a until b by c).choose` is equivalent to something closer to this:
  ```scala
  Gen.chooseNum(0, (b-a) - 1).map(l => a + (c * l))
  ```
- `Combinators.ranges(min, max)` generates `Range`s within those bounds
- `(g: Gen[A]).as.list(a to b)` as an alternative to `Gen.chooseNum(a, b).flatMap(Gen.listOfN(g, foo))`
  Variants also exist to produce `Vector`, `Chain`, and the `NonEmpty*` equivalents, as well as one to 
  lift a `Gen[Char]` into a `Gen[String]`
- `(g: Gen[A]).optional` as an chaining alternative to `Gen.option(g)`

### `commons-decline`

Instances for Decline, notably one for `Slice` as it tends to very handy for CLI utilities.