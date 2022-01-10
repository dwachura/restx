package io.dwsoft.restx.core.cause.message

import io.dwsoft.restx.RestXException
import io.dwsoft.restx.core.Logging.initLog
import io.dwsoft.restx.core.cause.Cause

/**
 * Interface of cause message resolvers.
 *
 * @param T type of fault object which causes are supported by created object
 */
fun interface MessageResolver<in T : Any> {
    /**
     * Method returning message for given [Cause].
     *
     * @throws MessageResolvingFailure in case message for given [id][Cause] cannot be resolved
     */
    fun messageFor(cause: Cause<T>): String

    /**
     * Factories of [MessageResolver]s.
     * Additional factory methods should be added as an extension functions.
     */
    companion object Factories {
        /**
         * Factory method that creates [MessageResolver] based on passed function
         */
        fun <T : Any> generatedAs(resolver: Cause<T>.() -> String): MessageResolver<T> =
            object : MessageResolver<T> {
                private val log = MessageResolver::class.initLog()

                override fun messageFor(cause: Cause<T>): String =
                    cause.resolver().also {
                        log.info { "Returning message from custom resolver for $cause" }
                        log.debug { "Message: $it" }
                    }
            }

        /**
         * Factory method for [FixedMessageResolver]
         */
        fun <T : Any> fixed(message: String): MessageResolver<T> = FixedMessageResolver(message)

        /**
         * Factory method for [MapBasedMessageResolver]
         */
        fun <T : Any> mapBased(mapping: Map<String, String>): MessageResolver<T> =
            MapBasedMessageResolver(mapping)

        /**
         * Factory method for [MapBasedMessageResolver]
         */
        fun <T : Any> mapBased(vararg mapEntries: Pair<String, String>): MessageResolver<T> =
            this.mapBased(mapping = mapOf(pairs = mapEntries))
    }
}

class MessageResolvingFailure(message: String) : RestXException(message)

/**
 * Implementation of [MessageResolver] returning fixed message for any fault cause.
 */
class FixedMessageResolver(private val message: String) : MessageResolver<Any> {
    private val log = initLog()

    override fun messageFor(cause: Cause<Any>): String =
        message.also {
            log.info { "Returning fixed message for $cause" }
            log.debug { "Message: $it" }
        }
}

/**
 * Implementation of [MessageResolver] returning message based on fault id from predefined map.
 *
 * @param mapping <fault cause id>:<fault message> map
 */
class MapBasedMessageResolver(private val mapping: Map<String, String>) : MessageResolver<Any> {
    private val log = initLog()

    init {
        require(mapping.isNotEmpty()) { "Fault message mappings not provided" }
    }

    override fun messageFor(cause: Cause<Any>): String {
        return mapping[cause.id]
            ?.also {
                log.info { "Found mapped message for $cause" }
                log.debug { "Message: $it" }
            }
            ?: throw MessageResolvingFailure("None message mapping found for id '${cause.id}'")
    }
}
