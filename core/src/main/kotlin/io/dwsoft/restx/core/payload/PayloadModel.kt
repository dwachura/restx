package io.dwsoft.restx.core.payload

import io.dwsoft.restx.RestXException
import io.dwsoft.restx.core.Logging.initLog
import io.dwsoft.restx.core.payload.Message.Translator.LocaleNotSupported
import io.dwsoft.restx.core.payload.Message.Translator.LocalizationException
import java.util.Locale

/**
 * Base interface used to flag classes that represent valid error response payload.
 */
sealed interface ErrorResponsePayload

/**
 * Base representation of a single error response payload.
 */
sealed interface SingleErrorPayload : ErrorResponsePayload {
    val code: String
    val message: Message
}

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
 * Representation of an error response payload happened during request processing.
 */
data class OperationError(override val code: String, override val message: Message) : SingleErrorPayload

/**
 * Representation of an error response payload caused by invalid request data.
 */
data class RequestDataError(
    override val code: String,
    override val message: Message,
    val source: Source
) : SingleErrorPayload {
    /**
     * Representation of a 'source' of invalid request data.
     * It's used to provide additional information for API client regarding [type][type] and [place][location] of
     * bad data, e.g. name of query param or path to body property.
     */
    class Source private constructor(val type: Type, val location: String) {
        init {
            require(location.isNotBlank()) { "Invalid data source location must be set" }
        }

        override fun toString(): String {
            return "${javaClass.simpleName}(type=$type, location=$location)"
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Source

            if (type != other.type) return false
            if (location != other.location) return false

            return true
        }

        override fun hashCode(): Int {
            var result = type.hashCode()
            result = 31 * result + location.hashCode()
            return result
        }

        companion object Factories {
            fun query(param: String) = Source(Type.QUERY, param)
            fun header(name: String) = Source(Type.HEADER, name)
            fun body(path: String) = Source(Type.BODY, path)
        }

        /**
         * Type of invalid request data source.
         */
        enum class Type {
            /**
             * Invalid value of a query parameter.
             */
            QUERY,

            /**
             * Invalid value of a header.
             */
            HEADER,

            /**
             * Invalid value of a body property.
             */
            BODY;

            fun toSource(location: String) = Source(this, location)
        }
    }
}

/**
 * Representation of a response payload generated in case of multiple errors happen during
 * execution of application's logic.
 */
data class MultiErrorPayload(val errors: List<SingleErrorPayload>) : ErrorResponsePayload

fun List<SingleErrorPayload>.toPayload() = when (size) {
    1 -> first()
    else -> MultiErrorPayload(this)
}
