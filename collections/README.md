# Scala Commons: Collections

Helpers and utilities for working with various collections from the Scala standard library.

## Artifacts

### SBT
```
"com.github.morgen-peschke" % "commons-collections" % commonsVersion
```

### Mill
```
ivy"com.github.morgen-peschke::commons-collections:$commonsVersion"
```

## Documentation

### `peschke.collections.TakeUntil.syntax._`

Provides `TraversibleLike#takeUntil`, which is alternative to `TraversibleLike#takeWhile`. 
The most useful distinction between the two is how the final value is handled. 

```scala
assert((0 to 10).toList.takeWhile(_ != 5) == List(0, 1, 2, 3, 4))
assert((0 to 10).toList.takeUntil(_ == 5) == List(0, 1, 2, 3, 4, 5))
```

### `peschke.collections.range.syntax._`

Provides utility methods for manipulating `Range` and `NumericRange`.

- `grow` :: This extends a range such that (ignoring underflow) `range.grow(n).drop(n) == range`
- `growRight` :: This extends a range such that (ignoring overflow) `range.growRight(n).dropRight(n) == range`
- `shift` :: This shifts a range towards `Int.MaxValue` by a multiple of the range step
- `unshift` :: This shifts a range towards `Int.MinValue` by a multiple of the range step
- `sliceRange` :: This specialization of `NumericRange#slice` returns a `NumericRange` instead of an `IndexedSeq`