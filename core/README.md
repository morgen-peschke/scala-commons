# Scala Commons: Core

The most generic utilities live here.

## Artifacts

### SBT
```
"com.github.morgen-peschke" % "commons-core" % commonsVersion
```

### Mill
```
ivy"com.github.morgen-peschke::commons-core:$commonsVersion"
```

## Documentation

### `peschke.cats.syntax._`

Provides quality of life syntax enhancements for the [cats](https://typelevel.org/cats/) 
library. 

Most of these do not provide additional functionality, rather they make existing capabilities
a bit easier to access. For example, there are helpers to avoid having to wrap a value in a 
`cats.data.Nested`, if only an occasional `map` or `flatMap` on the inner value is needed. 
There are also some builders for more complex `Eq` and `Order` instances.

Currently, the only utility that adds a net-new capability is `andThenF`, which is very similar
to `parFlatTraverse`. As the name implies, it's intended primarily for use with 
`cats.data.Validated`, which cannot provide a lawful `FlatMap` instance.

### `peschke.numeric.Bounded`

This typeclass encodes the upper and lower limits of a type. This is primarily useful when 
paired with `Numeric` or `Integral`.

### `peschke.python.{Slice, SliceParser, Slicer}`

These provide a pure-Scala implementation of Python's slice notation and a parser for this
notation.

This can be very handy when defining CLI interfaces.

### `peschke.Complete`

`Complete` provides a work-around for the issues caused by value discarding. See
[the scaladoc](src/peschke/Complete.scala) for specifics.

### `peschke.Convertible`

This typeclass provides a way to declare that a conversion from one type to another 
exists, in a way that side-steps many of the pitfalls of implicit conversions.