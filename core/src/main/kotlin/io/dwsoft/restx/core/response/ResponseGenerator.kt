package io.dwsoft.restx.core.response

import io.dwsoft.restx.RestXException
import io.dwsoft.restx.core.Logging.initLog
import io.dwsoft.restx.core.payload.ErrorPayloadGenerator

/**
 * Interface of the library's main component serving as an entry point to handle fault results of given type. That is,
 * to convert fault result emitted by the application's logic into HTTP response model.
 *
 * @param T type of fault objects that generator is able to work with
 */
sealed interface ResponseGenerator<T : Any> {
    /**
     * Main method that performs fault result handling.
     *
     * @param fault fault object for which an error response should be calculated
     * @throws RestXException in case of errors during response generation
     */
    fun responseOf(fault: T): ErrorResponse
}

/**
 * Default implementation of [ResponseGenerator].
 */
class SimpleResponseGenerator<T : Any>(
    private val payloadGenerator: ErrorPayloadGenerator<T, *>,
    private val statusProvider: ResponseStatusProvider
) : ResponseGenerator<T> {
    private val log = initLog()

    override fun responseOf(fault: T): ErrorResponse {
        log.info { "Handling fault: $fault" }
        val payload = payloadGenerator.payloadOf(fault)
        return ErrorResponse(statusProvider.get(), payload)
    }
}

/**
 * Interface of [HttpStatus] providers.
 */
fun interface ResponseStatusProvider {
    fun get(): HttpStatus

    /**
     * Factories of [ResponseStatusProvider]s.
     */
    companion object Factories {
        fun of(status: Int) = ResponseStatusProvider { HttpStatus(status) }
        fun of(status: () -> Int) = ResponseStatusProvider { HttpStatus(status()) }
        fun providedBy(statusProvider: () -> HttpStatus) = ResponseStatusProvider { statusProvider() }
    }
}
