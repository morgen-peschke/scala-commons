# Scala Commons: MUnit

Instances for the [MUnit](https://www.mulesoft.com/platform/munit-integration-testing) 
test framework.

## Artifacts

### SBT
```
"com.github.morgen-peschke" % "commons-munit" % commonsVersion
```

### Mill
```
ivy"com.github.morgen-peschke::commons-munit:$commonsVersion"
```

## Documentation

### `peschke.munit.MUnitValueExtractors`

Provides a trait adapting [`ValueExtractor`](../testing/src/peschke/testing/ValueExtractor.scala)
for the MUnit test framework.