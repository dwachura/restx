package io.dwsoft.restx.core.cause.code

import io.dwsoft.restx.RestXException
import io.dwsoft.restx.core.cause.Cause
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
     * @throws CodeResolvingFailure in case code for given [id][Cause] cannot be resolved
     */
    fun codeFor(cause: Cause<T>): String
}

class CodeResolvingFailure(message: String) : RestXException(message)

/**
 * Implementation of [CodeResolver] returning fixed code for any fault cause.
 */
class FixedCodeResolver(private val code: String) : CodeResolver<Any> {
    private val log = initLog()

    override fun codeFor(cause: Cause<Any>): String =
        code.also { log.info { "Returning fixed code [$it] for $cause" } }
}

/**
 * Implementation of [CodeResolver] returning code based on fault id from predefined map.
 *
 * @param mapping <fault cause id>:<fault code> map
 */
class MapBasedCodeResolver(private val mapping: Map<String, String>) : CodeResolver<Any> {
    private val log = initLog()

    init {
        require(mapping.isNotEmpty()) { "Fault code mappings not provided" }
    }

    override fun codeFor(cause: Cause<Any>): String {
        return mapping[cause.id]
            ?.also { log.info { "Found mapped code [$it] for $cause" } }
            ?: throw CodeResolvingFailure("None code mapping found for id '${cause.id}'")
    }
}

/**
 * Factories of [CodeResolver]s.
 * Additional factory methods should be added as an extension functions.
 */
object CodeResolvers {
    /**
     * Factory method that creates [CodeResolver] based on passed function
     */
    fun <T : Any> generatedAs(resolver: Cause<T>.() -> String): CodeResolver<T> =
        object : CodeResolver<T> {
            private val log = CodeResolver::class.initLog()

            override fun codeFor(cause: Cause<T>): String =
                cause.resolver().also { log.info { "Returning code [$it] from custom resolver for $cause" } }
        }

    /**
     * Factory method for [resolvers][CodeResolver] that returns code same as passed [cause id][Cause.id]
     */
    fun <T : Any> sameAsCauseId(): CodeResolver<T> = generatedAs { id }

    /**
     * Factory method for [FixedCodeResolver]
     */
    fun <T : Any> fixed(code: String): CodeResolver<T> = FixedCodeResolver(code)

    /**
     * Factory method for [MapBasedCodeResolver]
     */
    fun <T : Any> mapBased(mapping: Map<String, String>): CodeResolver<T> = MapBasedCodeResolver(mapping)

    /**
     * Factory method for [MapBasedCodeResolver]
     */
    fun <T : Any> mapBased(vararg mapEntries: Pair<String, String>): CodeResolver<T> =
        this.mapBased(mapping = mapOf(pairs = mapEntries))
}
