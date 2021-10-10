package io.dwsoft.restx.core.cause.code

import io.dwsoft.restx.RestXException
import io.dwsoft.restx.core.cause.Cause
import io.dwsoft.restx.core.Logging.initLog

/**
 * Interface for cause code providers.
 *
 * @param T type of fault object which causes are supported by created object
 */
fun interface CauseCodeProvider<in T : Any> {
    /**
     * Method returning code for given [Cause].
     *
     * @throws CauseCodeProvisioningFailure in case code for given [id][Cause] cannot
     *      be provided
     */
    fun codeFor(cause: Cause<T>): String
}

class CauseCodeProvisioningFailure(message: String) : RestXException(message)

/**
 * Implementation of [CauseCodeProvider] returning fixed code for any fault cause.
 */
class FixedCauseCodeProvider(private val code: String) : CauseCodeProvider<Any> {
    private val log = initLog()

    override fun codeFor(cause: Cause<Any>): String =
        code.also { log.info { "Returning fixed code [$it] for $cause" } }
}

/**
 * Implementation of [CauseCodeProvider] returning code based on fault id from
 * predefined map.
 *
 * @param mapping <fault cause id>:<fault code> map
 */
class MapBasedCauseCodeProvider(private val mapping: Map<String, String>) : CauseCodeProvider<Any> {
    private val log = initLog()

    init {
        require(mapping.isNotEmpty()) { "Fault code mappings not provided" }
    }

    override fun codeFor(cause: Cause<Any>): String {
        return (
                mapping[cause.id]
                    ?: throw CauseCodeProvisioningFailure(
                        "None code mapping found for id '${cause.id}'"
                    )
        ).also { log.info { "Found mapped code [$it] for $cause" } }
    }
}

/**
 * Factories of [CauseCodeProvider]s.
 * Additional factory methods should be added as an extension functions.
 */
object CauseCodeProviders {
    /**
     * Factory method that creates [CauseCodeProvider] based on passed function
     */
    fun <T : Any> generatedAs(provider: Cause<T>.() -> String): CauseCodeProvider<T> =
        object : CauseCodeProvider<T> {
            private val log = CauseCodeProvider::class.initLog()

            override fun codeFor(cause: Cause<T>): String =
                cause.provider().also { log.info { "Returning code [$it] from custom provider for $cause" } }
        }

    /**
     * Factory method for [providers][CauseCodeProvider] that returns code same as passed [cause id][Cause.id]
     */
    fun <T : Any> sameAsCauseId(): CauseCodeProvider<T> = generatedAs { id }

    /**
     * Factory method for [FixedCauseCodeProvider]
     */
    fun <T : Any> fixed(code: String): CauseCodeProvider<T> = FixedCauseCodeProvider(code)

    /**
     * Factory method for [MapBasedCauseCodeProvider]
     */
    fun <T : Any> mapBased(mapping: Map<String, String>): CauseCodeProvider<T> = MapBasedCauseCodeProvider(mapping)

    /**
     * Factory method for [MapBasedCauseCodeProvider]
     */
    fun <T : Any> mapBased(vararg mapEntries: Pair<String, String>): CauseCodeProvider<T> =
        this.mapBased(mapping = mapOf(pairs = mapEntries))
}
