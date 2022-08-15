# scala-commons

Common utility libraries for Scala, mostly small stuff I don't want to have multiple copies of lying around.

## Installation

Currently it's not yet on maven, but it can be referenced from github using `JitPack`.

[JitPack](https://jitpack.io/) is another (possibly better) option:

```sbtshell
resolvers += "jitpack" at "https://jitpack.io"

libraryDependencies += "com.github.morgen-peschke" % "scala-commons" % "v1.0.1"	
```

## Highlights

### `TakeUntil`

An alternative to `IterableOnce#takeWhile`, which is primarily differentiated on two points:
1. The elements are consumed until the predicate is true, rather than while the predicate is true, so the logic is 
reversed. 
2. The final element is also taken.

### `Complete`

A placeholder indicating completion, which provides a work-around for the type issues which can arise when returning 
things like `Future[Unit]`. Check the docs for more details.

This is very similar to `akka.Done`, and exists as a lightweight alternative to the same. 
