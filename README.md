
# Warp

A Gradle "macro" for calling private code at runtime with zero overhead.




## Usage/Examples

This library is built for Kotlin but also works in Java with the same annotations and conventions.

```kt
// build.gradle.kts
plugins {
    id("rip.sunrise.warp") version "VERSION"
}

dependencies {
    implementation("rip.sunrise:Warp:VERSION")
}
```

```kt
// SomeInvokeUtils.kt

// Note: You can throw any exception, `error("unreachable")` would also work.
// You can return any value, it doesn't matter - the code is removed at during building.

// Instance (virtual) -> Not-Null
@Invoker
fun SomeInstance.virtualMethod(): SomeReturn = TODO()

// Instance (virtual) -> Null
@Invoker
fun SomeInstance.virtualMethod(): SomeReturn? = TODO()

// Static -> Not-Null
@Invoker
fun staticMethod(): SomeReturn = TODO()

// Static -> Null
@Invoker
fun staticMethod(): SomeReturn? = TODO()
```


## Performance

```
Benchmark                       Mode  Cnt           Score           Error  Units
Benchmark.benchDirect          thrpt    6  2289749848.755 ± 116839700.776  ops/s
Benchmark.benchPrivateProxy    thrpt    6  2300589405.526 ± 108254358.787  ops/s
Benchmark.benchPrivateReflect  thrpt    6   152724078.442 ±  26793266.464  ops/s
Benchmark.benchPrivateWarp     thrpt    6  2306826708.864 ± 134429044.662  ops/s
Benchmark.benchProxy           thrpt    6  2278999045.999 ±  57285615.505  ops/s
Benchmark.benchReflect         thrpt    6   152928846.069 ±  15590454.001  ops/s
Benchmark.benchWarp            thrpt    6  2312206648.556 ± 100829931.023  ops/s
```

A `final` `MethodHandle` is nearly as fast as direct access.  The runtime doesn't need to check access every time, unlike with reflection.

## Under the Hood

The compiled `.class` files get modified right after compilation.

For example, a method annotated with `@Invoker` goes from:
```kotlin
@Invoker(clazz = SomeClass::class, name = "somePrivateMethod", isStatic = true)
fun invokeSomePrivateMethod(): Int = TODO()
```
to something like:
```java

private static final MethodHandle INVOKER_somePrivateMethod;

public static int invokeSomePrivateMethod() {
    return INVOKER_somePrivateMethod.invokeExact();
}

static {
    Method m = SomeClass.class.getDeclaredMethod("somePrivateMethod");
    UnsafeUtils.setAccessibleUnsafe(m, true);
    INVOKER_somePrivateMethod = MethodHandles.lookup().unreflect(m);
}

```