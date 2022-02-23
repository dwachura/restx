package io.dwsoft.restx.core.cause.message

import io.dwsoft.restx.RestXException
import io.dwsoft.restx.core.Logging.initLog
import io.dwsoft.restx.core.cause.Cause
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
}
operator fun <T : Any> MessageResolver<T>.invoke(cause: Cause<T>) = messageFor(cause)

class MessageResolvingException(message: String) : RestXException(message)

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
