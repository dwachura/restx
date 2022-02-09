package io.dwsoft.restx.core.samples

import io.dwsoft.restx.RestX
import io.dwsoft.restx.core.cause.message.generatedAs
import io.dwsoft.restx.core.payload.Message
import io.dwsoft.restx.core.payload.Message.Translator.LocaleNotSupported
import io.dwsoft.restx.core.payload.SingleErrorPayload
import java.util.Locale

fun main() {
    val generator = RestX.respondTo<Exception> { asOperationError {
        withMessage { generatedAs {
            Message("Default error message")
                .withDefaultTranslator { _, locale ->
                    when (locale) {
                        Locale.ENGLISH -> "English: ${this.context.localizedMessage}"
                        Locale.GERMAN -> "German: ${this.context.localizedMessage}"
                        else -> throw LocaleNotSupported(locale)
                    }
                }
        } } // generate payloads with exception message supporting localization
        withStatus(500)
    } }

    val response = generator.responseOf(RuntimeException("Service failure"))
    val message = (response.payload as SingleErrorPayload).message
    println(message.localized(Locale.ENGLISH))
    println(message.localized(Locale.GERMAN))
    println(message.localized(Locale.ITALIAN)) // default text displayed for unsupported locale
}
