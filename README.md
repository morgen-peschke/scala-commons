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

### `commons-collections`

Helpers and extensons for working with the Scala standard library.

#### `TakeUntil`

An alternative to `IterableOnce#takeWhile`, which is primarily differentiated on two points:
1. The elements are consumed until the predicate is true, rather than while the predicate is true, so the logic is 
reversed. 
2. The final element is also taken.


