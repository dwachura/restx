package io.dwsoft.restx.core.response.payload

import io.dwsoft.restx.RestXException
import io.dwsoft.restx.core.Logging.initLog
import io.dwsoft.restx.core.response.payload.Message.Translator.LocaleNotSupported
import io.dwsoft.restx.core.response.payload.Message.Translator.LocalizationException
import java.util.Locale

/**
 * Representation of an error payload's message (human-readable description of an error that occurred) with optional
 * localization support.
 */
class Message private constructor(
    val value: String,
    private val translator: Translator?
) {
    constructor(value: String) : this(value, null)

    /**
     * Returns clone of this message with given [translator].
     */
    fun translatedBy(translator: Translator) = Message(value, translator)

    /**
     * Delegate of [translatedBy], returning message that uses [default translation strategy]
     * [DefaultValueTranslatorDecorator], which is configured by given [translator][decoratedTranslator] and optionally
     * [default display text][defaultDisplayText] (message [value] is  used if not set).
     */
    fun withDefaultTranslator(defaultDisplayText: String = value, decoratedTranslator: Translator) =
        translatedBy(DefaultValueTranslatorDecorator(defaultDisplayText, decoratedTranslator))

    /**
     * Returns [localized][locale] text of this message.
     *
     * If [translator][Translator] is [configured][translatedBy], it is [used][Translator.localize] to
     * provide translated value.
     *
     * By default, when no translation context is defined, message's [value] is returned.
     *
     * @throws LocaleNotSupported originally thrown from [Translator.localize]
     * @throws LocalizationException originally thrown from [Translator.localize]
     */
    fun localized(locale: Locale): String = translator?.localize(this.value, locale) ?: value

    /**
     * Interface of message translators.
     */
    fun interface Translator {
        /**
         * Returns passed [message] represented in a given [locale].
         *
         * @throws LocaleNotSupported in case given locale is not supported
         * @throws LocalizationException in case of any errors during conversion this message into given locale
         */
        fun localize(message: String, locale: Locale): String

        class LocaleNotSupported(locale: Locale) : RestXException("Locale ${locale.displayName} is not supported")

        class LocalizationException : RestXException {
            constructor(message: String, locale: Locale, failureReason: String) :
                super(createExceptionMessage(message, locale, failureReason))

            constructor(message: String, locale: Locale, cause: Throwable) :
                super(createExceptionMessage(message, locale, cause.message), cause)

            private companion object {
                private fun createExceptionMessage(message: String, locale: Locale, failureReason: String?) =
                    "Translation of message ($message) into locale ${locale.displayName} failed" +
                        with(failureReason) { if (this != null) ": $this" else "" }
            }
        }
    }

    /**
     * [Translator] that let the user define [default value][defaultDisplayText] that should be used in case
     * [decorated translator][translator] throws [LocaleNotSupported].
     */
    class DefaultValueTranslatorDecorator(
        private val defaultDisplayText: String,
        private val translator: Translator
    ) : Translator {
        private val log = initLog()

        /**
         * Translate passed [message] into given [locale], according to the configured [strategy][translator].
         * In case of unsupported locale, instead of throwing exception, [default text][defaultDisplayText] is returned.
         *
         * @throws LocalizationException in case of errors during localization process
         */
        override fun localize(message: String, locale: Locale): String =
            runCatching { translator.localize(message, locale) }
                .fold(
                    onSuccess = { it },
                    onFailure = {
                        when (it) {
                            is LocaleNotSupported ->
                                log.info { "Returning default value - ${it.localizedMessage}" }
                                    .let { defaultDisplayText }
                            else -> throw it
                        }
                    }
                )
    }
}

/**
 * Utility function that creates unlocalized [Message] always returning string passed as a receiver.
 */
internal fun String.asMessage() = Message(this)

/**
 * Interface of cause message resolvers.
 *
 * @param T type of fault object which causes are supported by created object
 */
fun interface MessageResolver<in T : Any> {
    /**
     * Method returning [message][Message] for given [Cause].
     *
     * @throws MessageResolvingException in case message for given [key][Cause.key] cannot be resolved
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
    fun getFor(cause: Cause<T>): String
}
operator fun <T : Any> MessageTextProvider<T>.invoke(cause: Cause<T>) = getFor(cause)
