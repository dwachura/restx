package io.dwsoft.restx.core.response.payload

import io.dwsoft.restx.RestXException
import io.dwsoft.restx.core.Logging.initLog

/**
 * Interface for cause code resolvers.
 *
 * @param T type of fault object which causes are supported by created object
 */
fun interface CodeResolver<in T : Any> {
    /**
     * Method returning code for given [Cause].
     *
     * @throws CodeResolvingException in case code for given [key][Cause.key] cannot be resolved
     */
    fun codeFor(cause: Cause<T>): String
}

class CodeResolvingException(message: String) : RestXException(message)

/**
 * Implementation of [CodeResolver] returning fixed code for any fault cause.
 */
class FixedCodeResolver(private val code: String) : CodeResolver<Any> {
    private val log = initLog()

    override fun codeFor(cause: Cause<Any>): String =
        code.also { log.info { "Returning fixed code [$it] for $cause" } }
}

/**
 * Implementation of [CodeResolver] returning code based on fault key from predefined map.
 *
 * @param mapping <fault cause key>:<fault code> map
 */
class MapBasedCodeResolver(private val mapping: Map<String, String>) : CodeResolver<Any> {
    private val log = initLog()

    init {
        require(mapping.isNotEmpty()) { "Fault code mappings not provided" }
    }

    override fun codeFor(cause: Cause<Any>): String {
        return mapping[cause.key]
            ?.also { log.info { "Found mapped code [$it] for $cause" } }
            ?: throw CodeResolvingException("None code mapping found for key '${cause.key}'")
    }
}
