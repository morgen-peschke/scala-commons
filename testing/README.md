# Scala Commons: Testing

This module provides the core definitions and implementations for test
helpers which are then adapted to the appropriate frameworks in 
`commons-munit` and `commons-scalatest`

## Artifacts

### SBT
```
"com.github.morgen-peschke" % "commons-testing" % commonsVersion
```

### Mill
```
ivy"com.github.morgen-peschke::commons-testing:$commonsVersion"
```

## Documentation

### `peschke.testing.ValueExtractor`

This typeclass encodes the operation of removing a value from an effect.
This is conceptually the reverse of `Applicative.pure`, however it has
been specialized to the needs of testing so that errors can be reported
in such a way as to integrate with a test framework.

### `peschke.testing.AssertionValueExtractors`

Provides a trait adapting `ValueExtractors` for bare Scala assertions, 
rather than any particular test framework.