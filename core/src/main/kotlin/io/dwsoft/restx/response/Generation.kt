package io.dwsoft.restx.response

import io.dwsoft.restx.fault.FaultResult
import io.dwsoft.restx.RestXException
import io.dwsoft.restx.initLog
import io.dwsoft.restx.payload.ErrorPayloadGenerator

/**
 * RestX library's entry point to handle fault results. That is, to convert
 * fault result emitted by the application's logic into HTTP response model.
 *
 * @param T type of fault objects that generator is able to work with
 */
class ResponseGenerator<T : FaultResult>(
    private val generator: ErrorPayloadGenerator<T, *>,
    private val statusProvider: ResponseStatusProvider
) {
    private val log = initLog()

    /**
     * Main method that performs fault result handling.
     *
     * @param fault fault object for which an error response should be calculated
     * @throws RestXException in case of errors during response generation
     */
    fun responseOf(fault: T): ErrorResponse {
        log.info { "Handling fault: $fault" }
        val payload = generator.payloadOf(fault)
        return ErrorResponse(statusProvider.get(), payload)
    }
}

/**
 * Interface of [HttpStatus] providers
 */
fun interface ResponseStatusProvider {
    fun get(): HttpStatus
}
