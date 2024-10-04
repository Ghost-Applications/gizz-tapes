package gizz.tapes

import java.lang.reflect.Proxy

/**
 * Stub out interfaces for previews or tests
 * Will throw NotImplementedError on any functions that aren't directly implemented.
 *
 * usage: `val a: Interface = stub()`
 * or
 * ```
 * val a: Interface = object : Interface by stub() {
 *   override fun someMethod() = "Yay implemented for previews"
 * }
 * ```
 */
inline fun <reified T : Any> stub(): T =
    Proxy.newProxyInstance(
        T::class.java.classLoader,
        arrayOf<Class<*>>(T::class.java)
    ) { _, _, _ -> TODO() } as T
