# scala-commons

Common utility libraries for Scala, mostly small stuff I don't want to have multiple copies of lying around.

## Installation

Currently it's not yet on maven, but it can be referenced from github using `sbt`.

```sbtshell
lazy val root =
  (project in file("."))
    .dependsOn(scalaCommons)
    ...

lazy val scalaCommons = RootProject(uri("git://github.com/morgen-peschke/scala-commons.git#v1.0.0"))
```

## Highlights

### `TakeUntil`

An alternative to `GenTraversable#takeWhile`, which is primarily differentiated on two points:
1. The elements are consumed until the predicate is true, rather than while the predicate is true, so the logic is 
reversed. 
2. The final element is also taken.

### `Complete`

A placeholder indicating completion, which provides a work-around for the type issues which can arise when returning 
things like `Future[Unit]`. Check the docs for more details.

This is very similar to `akka.Done`, and exists as a lightweight alternative to the same. 