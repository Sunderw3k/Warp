package rip.sunrise.warp.annotations

import kotlin.reflect.KClass

@SuppressWarnings("unused")
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION)
annotation class Invoker(val clazz: KClass<*>, val methodName: String, val isStatic: Boolean)
