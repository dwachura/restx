package io.dwsoft.restx.core.cause.message

import io.dwsoft.restx.RestXException
import io.dwsoft.restx.core.Logging.initLog
import io.dwsoft.restx.core.cause.Cause
import io.dwsoft.restx.core.cause.message.MessageResolver.Factories.fromTextGeneratedBy
import io.dwsoft.restx.core.cause.message.MessageResolver.Factories.resolvedBy
import io.dwsoft.restx.core.payload.Message
import io.dwsoft.restx.core.payload.asMessage

/**
 * Interface of cause message resolvers.
 *
 * @param T type of fault object which causes are supported by created object
 */
fun interface MessageResolver<in T : Any> {
    /**
     * Method returning [message][Message] for given [Cause].
     *
     * @throws MessageResolvingException in case message for given [id][Cause] cannot be resolved
     */
    fun messageFor(cause: Cause<T>): Message

    /**
     * Factories of [MessageResolver]s.
     * Additional factory methods should be added as an extension functions.
     */
    companion object Factories {
        /**
         * Factory method that simply returns passed [resolver].
         */
        fun <T : Any> resolvedBy(resolver: MessageResolver<T>): MessageResolver<T> = resolver

        /**
         * Delegate of [resolvedBy] returning [PlainTextMessageResolver].
         */
        fun <T : Any> fromTextGeneratedBy(messageTextProvider: MessageTextProvider<T>): MessageResolver<T> =
            resolvedBy(PlainTextMessageResolver(messageTextProvider))

        /**
         * Factory method that creates [resolver][MessageResolver] returning [Message]s with given [text][message]
         * as their content.
         */
        fun <T : Any> fromText(message: String): MessageResolver<T> = fromTextGeneratedBy { message }
    }
}
operator fun <T : Any> MessageResolver<T>.invoke(cause: Cause<T>) = messageFor(cause)

class MessageResolvingException(message: String) : RestXException(message)

/**
 * Delegate of [resolvedBy] returning [resolver][MessageResolver] based on passed [function][resolver].
 */
fun <T : Any> MessageResolver.Factories.generatedAs(resolver: Cause<T>.() -> Message): MessageResolver<T> =
    resolvedBy(object : MessageResolver<T> {
        private val log = MessageResolver::class.initLog()

        override fun messageFor(cause: Cause<T>): Message =
            cause.resolver().also {
                log.info { "Returning message from custom resolver for $cause" }
                log.debug { "Message: $it" }
            }
    })

/**
 * Delegate of [fromTextGeneratedBy] utilizing function with receiver for more concise syntax.
 */
fun <T : Any> MessageResolver.Factories.fromTextGeneratedAs(
    messageTextProvider: Cause<T>.() -> String
): MessageResolver<T> = fromTextGeneratedBy(MessageTextProvider(messageTextProvider))

/**
 * Implementation of [MessageResolver] returning plain text message.
 */
class PlainTextMessageResolver<T : Any>(private val messageTextProvider: MessageTextProvider<T>) : MessageResolver<T> {
    private val log = initLog()

    override fun messageFor(cause: Cause<T>): Message =
        messageTextProvider(cause).asMessage()
            .also {
                log.info { "Returning plain text message for $cause" }
                log.debug { "Message: $it" }
            }
}

/**
 * Interface of providers of [plain text messages'][PlainTextMessageResolver] content.
 */
fun interface MessageTextProvider<T : Any> {
    /**
     * Returns message text for given [cause].
     *
     * @throws MessageResolvingException in case of errors during provisioning message text
     */
    fun getFor(cause : Cause<T>): String
}
operator fun <T : Any> MessageTextProvider<T>.invoke(cause: Cause<T>) = getFor(cause)
