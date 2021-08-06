package io.dwsoft.restx.fault.cause.message

import io.dwsoft.restx.RestXException
import io.dwsoft.restx.fault.cause.Cause
import io.dwsoft.restx.fault.cause.code.CauseCodeProvider
import io.dwsoft.restx.fault.cause.code.CauseCodeProviders
import io.dwsoft.restx.fault.cause.code.MapBasedCauseCodeProvider
import io.dwsoft.restx.fault.cause.code.mapBased
import io.dwsoft.restx.initLog

/**
 * Interface of cause message providers.
 *
 * @param T type of fault object which causes are supported by created object
 */
fun interface CauseMessageProvider<in T : Any> {
    /**
     * Method returning message for given [Cause].
     *
     * @throws CauseMessageProvisioningFailure in case message for given [id][Cause]
     *      cannot be provided
     */
    fun messageFor(cause: Cause<T>): String
}

class CauseMessageProvisioningFailure(message: String) : RestXException(message)

/**
 * Factories of [CauseMessageProvider]s.
 * Additional factory methods should be added as an extension functions.
 */
/*
 * TODO: check (especially in java) whether moving standard factory methods to this object is better.
 */
object CauseMessageProviders

/**
 * Factory method that creates [CauseMessageProvider] based on passed function
 */
fun <T : Any> CauseMessageProviders.generatedAs(provider: Cause<T>.() -> String): CauseMessageProvider<T> =
    object : CauseMessageProvider<T> {
        private val log = CauseMessageProvider::class.initLog()

        override fun messageFor(cause: Cause<T>): String =
            cause.provider().also {
                log.info { "Returning message from custom provider for $cause" }
                log.debug { "Message: $it" }
            }
    }

/**
 * Implementation of [CauseMessageProvider] returning fixed message for any fault cause.
 */
class FixedCauseMessageProvider(private val message: String) : CauseMessageProvider<Any> {
    private val log = initLog()

    override fun messageFor(cause: Cause<Any>): String =
        message.also {
            log.info { "Returning fixed message for $cause" }
            log.debug { "Message: $it" }
        }
}

/**
 * Factory method for [FixedCauseMessageProvider]
 */
fun <T : Any> CauseMessageProviders.fixed(message: String): CauseMessageProvider<T> =
    FixedCauseMessageProvider(message)

/**
 * Implementation of [CauseMessageProvider] returning message based on fault id from predefined map.
 *
 * @param mapping <fault cause id>:<fault message> map
 */
class MapBasedCauseMessageProvider(private val mapping: Map<String, String>) : CauseMessageProvider<Any> {
    private val log = initLog()

    init {
        require(mapping.isNotEmpty()) { "Fault message mappings not provided" }
    }

    override fun messageFor(cause: Cause<Any>): String {
        return (
                mapping[cause.id]
                    ?: throw CauseMessageProvisioningFailure(
                        "None message mapping found for id '${cause.id}'"
                    )
        ).also {
                log.info { "Found mapped message for $cause" }
                log.debug { "Message: $it" }
        }
    }
}

/**
 * Factory method for [MapBasedCauseMessageProvider]
 */
fun <T : Any> CauseMessageProviders.mapBased(mapping: Map<String, String>): CauseMessageProvider<T> =
    MapBasedCauseMessageProvider(mapping)

/**
 * Factory method for [MapBasedCauseMessageProvider]
 */
fun <T : Any> CauseMessageProviders.mapBased(vararg mapEntries: Pair<String, String>): CauseMessageProvider<T> =
    this.mapBased(mapping = mapOf(pairs = mapEntries))