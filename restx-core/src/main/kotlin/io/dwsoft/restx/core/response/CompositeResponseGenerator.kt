package io.dwsoft.restx.core.response

import io.dwsoft.restx.core.Collections.syncedMap
import io.dwsoft.restx.FactoryBlock
import io.dwsoft.restx.InitBlock
import io.dwsoft.restx.RestX
import io.dwsoft.restx.RestXException
import io.dwsoft.restx.core.Logging.initLog
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlin.reflect.KClass

/**
 * Implementation of [ResponseGenerator] that supports defining different strategies of handling faults yet still
 * preserves 'single entry' characteristic (in other words - typical application of composite pattern).
 */
class CompositeResponseGenerator(private val registry: ResponseGeneratorRegistry)
        : ResponseGenerator<Any> {
    private val log = initLog()

    /**
     * See [ResponseGenerator.responseOf].
     *
     * @throws NoSubGeneratorFound in case sub-generator for given [fault] type was not found
     */
    override fun responseOf(fault: Any): ErrorResponse {
        log.info { "Searching generator for fault [$fault]" }
        return registry.searchFor(fault)?.responseOf(fault) ?: throw NoSubGeneratorFound(fault)
    }

    companion object Builder {
        fun buildFrom(config: Config): CompositeResponseGenerator {
            val responseGeneratorRegistryFactoryBlock =
                config.responseGeneratorRegistryFactoryBlock
                    ?: throw IllegalArgumentException("Sub-generator registry factory block not set")
            return CompositeResponseGenerator(responseGeneratorRegistryFactoryBlock(Unit))
        }

        class Config {
            var responseGeneratorRegistryFactoryBlock: FactoryBlock<Unit, ResponseGeneratorRegistry>? = null
                private set

            /**
             * Configures [TypeBasedResponseGeneratorRegistry] to be used as [generator's][CompositeResponseGenerator]
             * source of sub-generators.
             */
            fun registeredByFaultType(
                initBlock: InitBlock<TypeBasedResponseGeneratorRegistry.Builder.Config>
            ) = this.apply {
                responseGeneratorRegistryFactoryBlock = {
                    TypeBasedResponseGeneratorRegistry.Builder.Config()
                        .apply(initBlock)
                        .let { TypeBasedResponseGeneratorRegistry.buildFrom(it) }
                }
            }
        }
    }
}

class NoSubGeneratorFound(fault: Any) : RestXException("No sub-generator found for fault [$fault]")

/**
 * Interface of registers of [ResponseGenerator]s.
 */
interface ResponseGeneratorRegistry {
    /**
     * Function used to search for [ResponseGenerator] able to process given [fault].
     *
     * @return [ResponseGenerator] or null, if there is no generator able to process [fault] defined
     */
    fun <T : Any> searchFor(fault: T): ResponseGenerator<T>?
}

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
    private val cache = syncedMap<KClass<*>, Deferred<ResponseGenerator<*>?>>()

    override fun <T : Any> searchFor(fault: T): ResponseGenerator<T>? {
        @Suppress("UNCHECKED_CAST")
        return runBlocking {
            log.info { "Searching generator for $fault" }
            log.debug { "Cache: $cache" }
            cache.getOrPut(fault::class) {
                async { searchRecursively(ArrayDeque(listOf(fault::class))) }
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

    companion object Builder {
        fun buildFrom(config: Config): TypeBasedResponseGeneratorRegistry =
            TypeBasedResponseGeneratorRegistry(
                config.generatorsByFaultType.takeIf { it.isNotEmpty() }
                    ?: throw IllegalArgumentException("Response generator registry cannot be empty")
            )

        class Config {
            var generatorsByFaultType: MutableMap<KClass<*>, ResponseGenerator<*>> = mutableMapOf()
                private set

            fun <T : Any> map(pair: Pair<KClass<T>, ResponseGenerator<T>>) =
                this.apply { generatorsByFaultType.plusAssign(pair) }

            fun <T : Any> map(faultType: KClass<T>, generatorFactoryBlock: FactoryBlock<RestX, ResponseGenerator<T>>) =
                map(faultType to generatorFactoryBlock(RestX))

            inline fun <reified T : Any> register(generatorFactoryBlock: FactoryBlock<RestX, ResponseGenerator<T>>) =
                map(T::class to generatorFactoryBlock(RestX))
        }
    }
}
