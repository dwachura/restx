package io.dwsoft.restx.core

import mu.KLogger
import mu.KotlinLogging
import java.util.Collections
import kotlin.jvm.internal.Reflection
import kotlin.reflect.KClass

internal object Logging {
    /**
     * Extension function used to create initialize [logger][KLogger] for given object's type.
     *
     * In case of runtime type of the object cannot be determined (e.g. in case of anonymous objects) explicit type
     * defined by type parameter [T] is used.
     */
    inline fun <reified T> T.initLog(): KLogger =
        takeIf { this != null }
            ?.let { it::class.qualifiedName }?.toLogger()
            ?: T::class.initLog()

    private fun String.toLogger(): KLogger {
        return KotlinLogging.logger(this)
    }

    /**
     * Overloaded version of [initLog] for object of type [KClass].
     */
    inline fun <reified T : KClass<*>> T.initLog(): KLogger =
        this.qualifiedName?.toLogger()
            ?: throw IllegalArgumentException("logger id must not be null")
}

internal object Collections {
    /**
     * Constructs [empty, synchronized map][Collections.synchronizedMap].
     */
    fun <K, V> syncedMap(): MutableMap<K, V> = Collections.synchronizedMap(mutableMapOf())
}

internal object Reflection {
    @Suppress("UNCHECKED_CAST")
    fun <T : Any> type(javaClass: Class<T>): KClass<T> = Reflection.getOrCreateKotlinClass(javaClass) as KClass<T>
    fun <T : Any> of(javaClass: Class<T>): KClass<T> = type(javaClass)
}
