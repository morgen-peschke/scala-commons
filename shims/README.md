# Scala Commons: Shims

This module provides minimal dependency alternatives to better libraries.

If, however, you're not able to access the better version, this module aims 
to provide at least some comfort in the form of minimal dependency alternatives.

## Artifacts

### SBT
```
"com.github.morgen-peschke" % "commons-shims" % commonsVersion
```

### Mill
```
ivy"com.github.morgen-peschke::commons-shims:$commonsVersion"
```

## Documentation

### `peschke.shims.resource.Managed`

This solves the same general problem as Java's try-with-resource construct, and
is far better solved by `cats.effect.Resource`.
