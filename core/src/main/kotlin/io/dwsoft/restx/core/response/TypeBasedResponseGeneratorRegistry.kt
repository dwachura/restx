package io.dwsoft.restx.core.response

import io.dwsoft.restx.core.Collections
import io.dwsoft.restx.core.Logging.initLog
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlin.reflect.KClass

/**
 * [ResponseGeneratorRegistry] that provides lookup of generators based on fault's type.
 *
 * It obeys type hierarchy during lookup, i.e. in case of the type of given fault doesn't have explicitly mapped
 * generator, register is searched for mappings defined for one of base types of the fault's type.
 *
 * Such behavior provides support for defining generators for 'generic' fault types and override them for some
 * specialized subtypes (e.g. [IllegalArgumentException] can be mapped to different generator than whole [Exception]
 * family).
 */
class TypeBasedResponseGeneratorRegistry(
    private val faultTypeToGenerator: Map<KClass<*>, ResponseGenerator<*>>
) : ResponseGeneratorRegistry {
    private val log = initLog()
    private val cache = Collections.syncedMap<KClass<*>, Deferred<ResponseGenerator<*>?>>()

    override fun <T : Any> searchFor(fault: T): ResponseGenerator<T>? {
        @Suppress("UNCHECKED_CAST")
        return runBlocking {
            log.info { "Searching generator for $fault" }
            synchronized(cache) {
                log.debug { "Cache: $cache" }
                cache.getOrPut(fault::class) {
                    async { searchRecursively(ArrayDeque(listOf(fault::class))) }
                }
            }.await()
        } as ResponseGenerator<T>?
    }

    private fun searchRecursively(queue: ArrayDeque<KClass<*>>): ResponseGenerator<*>? {
        return queue.removeFirstOrNull()?.run {
            log.info { "Looking up mapping for ${this.java.canonicalName}" }
            return (faultTypeToGenerator[this]
                ?: this.run {
                    this.supertypes.forEach { queue.addLast(it.classifier as KClass<*>) }
                    searchRecursively(queue)
                }
            )
        }
    }
}
