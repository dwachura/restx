package io.dwsoft.restx.core.cause.code

import io.dwsoft.restx.RestXException
import io.dwsoft.restx.core.Logging.initLog
import io.dwsoft.restx.core.cause.Cause
import io.dwsoft.restx.core.cause.code.CodeResolver.Factories.mapBased
import io.dwsoft.restx.core.cause.message.MessageResolver.Factories.resolvedBy

/**
 * Interface for cause code resolvers.
 *
 * @param T type of fault object which causes are supported by created object
 */
fun interface CodeResolver<in T : Any> {
    /**
     * Method returning code for given [Cause].
     *
     * @throws CodeResolvingException in case code for given [id][Cause] cannot be resolved
     */
    fun codeFor(cause: Cause<T>): String

    /**
     * Factories of [CodeResolver]s.
     * Additional factory methods should be added as an extension functions.
     */
    companion object Factories {
        /**
         * Factory method that simply returns passed [resolver].
         */
        fun <T : Any> resolvedBy(resolver: CodeResolver<T>): CodeResolver<T> = resolver

        /**
         * Factory method for [resolvers][CodeResolver] that returns code same as passed [cause id][Cause.id].
         */
        fun <T : Any> sameAsCauseId(): CodeResolver<T> = resolvedBy { it.id }

        /**
         * Factory method for [FixedCodeResolver]
         */
        fun <T : Any> fixed(code: String): CodeResolver<T> = FixedCodeResolver(code)

        /**
         * Factory method for [MapBasedCodeResolver].
         */
        fun <T : Any> mapBased(mapping: Map<String, String>): CodeResolver<T> = MapBasedCodeResolver(mapping)
    }
}

class CodeResolvingException(message: String) : RestXException(message)

/**
 * Delegate of [resolvedBy] returning [resolver][CodeResolver] based on passed [function][resolver].
 */
fun <T : Any> CodeResolver.Factories.generatedAs(resolver: Cause<T>.() -> String): CodeResolver<T> =
    resolvedBy(object : CodeResolver<T> {
        private val log = CodeResolver::class.initLog()

        override fun codeFor(cause: Cause<T>): String =
            cause.resolver().also { log.info { "Returning code [$it] from custom resolver for $cause" } }
    })

/**
 * Overloaded version of [mapBased].
 */
fun <T : Any> CodeResolver.Factories.mapBased(vararg mapEntries: Pair<String, String>): CodeResolver<T> =
    mapBased(mapping = mapOf(pairs = mapEntries))

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
            ?: throw CodeResolvingException("None code mapping found for id '${cause.id}'")
    }
}
