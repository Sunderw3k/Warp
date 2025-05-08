package rip.sunrise.warp

import org.openjdk.jmh.annotations.Benchmark
import org.openjdk.jmh.annotations.Fork
import org.openjdk.jmh.annotations.Measurement
import org.openjdk.jmh.annotations.Scope
import org.openjdk.jmh.annotations.State
import org.openjdk.jmh.annotations.Warmup
import org.openjdk.jmh.infra.Blackhole
import rip.sunrise.warp.annotations.Invoker
import java.util.concurrent.TimeUnit

@Suppress("unused")
@State(Scope.Benchmark)
@Warmup(iterations = 3, time = 2, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 3, time = 2, timeUnit = TimeUnit.SECONDS)
@Fork(2)
open class Benchmark {
    @Benchmark
    fun benchDirect(bh: Blackhole) {
        bh.consume(SomeClass.publicMethod())
    }

    @Benchmark
    fun benchProxy(bh: Blackhole) {
        bh.consume(SomeClass.publicProxy())
    }

    @Benchmark
    fun benchWarp(bh: Blackhole) {
        bh.consume(invokePublic())
    }

    private val publicReflect = SomeClass::class.java.getDeclaredMethod("publicMethod").also {
        UnsafeUtils.setAccessibleUnsafe(it, true)
    }
    @Benchmark
    fun benchReflect(bh: Blackhole) {
        bh.consume(publicReflect.invoke(null) as Int)
    }

    @Benchmark
    fun benchPrivateProxy(bh: Blackhole) {
        bh.consume(SomeClass.proxyPrivate())
    }

    @Benchmark
    fun benchPrivateWarp(bh: Blackhole) {
        bh.consume(invokePrivate())
    }

    private val privateReflect = SomeClass::class.java.getDeclaredMethod("privateMethod").also {
        UnsafeUtils.setAccessibleUnsafe(it, true)
    }
    @Benchmark
    fun benchPrivateReflect(bh: Blackhole) {
        bh.consume(privateReflect.invoke(null) as Int)
    }
}

@Invoker(clazz = SomeClass::class, methodName = "publicMethod", isStatic = true)
fun invokePublic(): Int = 0

@Invoker(clazz = SomeClass::class, methodName = "privateMethod", isStatic = true)
fun invokePrivate(): Int = 0


object SomeClass {
    @JvmStatic
    fun publicMethod(): Int = 1
    fun publicProxy(): Int = publicMethod()

    @JvmStatic
    private fun privateMethod(): Int = 2
    fun proxyPrivate(): Int = privateMethod()
}