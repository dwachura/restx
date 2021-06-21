package io.dwsoft.restx.fault.response

import io.dwsoft.restx.RestXException
import io.dwsoft.restx.fault.payload.ErrorPayloadGenerator
import io.dwsoft.restx.initLog

/**
 * RestX library's entry point to handle fault results. That is, to convert
 * fault result emitted by the application's logic into HTTP response model.
 *
 * @param T type of fault objects that generator is able to work with
 */
class ResponseGenerator<T : Any>(
    private val payloadGenerator: ErrorPayloadGenerator<T, *>,
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
        val payload = payloadGenerator.payloadOf(fault)
        return ErrorResponse(statusProvider.get(), payload)
    }

    companion object Builder {
        fun <T : Any> buildFrom(config: Config<T>): ResponseGenerator<T> {
            checkNotNull(config.errorPayloadGeneratorFactory) { "Payload generator factory must be provided" }
            checkNotNull(config.responseStatusProviderFactory) { "Status provider factory must be provided" }
            return ResponseGenerator(
                (config.errorPayloadGeneratorFactory!!)(ErrorPayloadGenerator.Builders),
                (config.responseStatusProviderFactory!!)(ResponseStatusProviders)
            )
        }

        class Config<T : Any> {
            var errorPayloadGeneratorFactory: (ErrorPayloadGeneratorFactory<T>)? = null
                private set
            var responseStatusProviderFactory: (ResponseStatusProviderFactory)? = null
                private set

            fun payloadOf(errorPayloadGeneratorFactory: ErrorPayloadGeneratorFactory<T>) {
                this.errorPayloadGeneratorFactory = errorPayloadGeneratorFactory
            }

            fun status(responseStatusProviderFactory: ResponseStatusProviderFactory) {
                this.responseStatusProviderFactory = responseStatusProviderFactory
            }
        }
    }
}

/**
 * Interface of [HttpStatus] providers.
 */
fun interface ResponseStatusProvider {
    fun get(): HttpStatus
}

/**
 * Factories of [ResponseStatusProvider]s.
 */
object ResponseStatusProviders {
    fun of(status: Int) = ResponseStatusProvider { httpStatus(status) }
    fun of(status: () -> Int) = ResponseStatusProvider { httpStatus(status()) }

    fun providedBy(statusProvider: () -> HttpStatus) = ResponseStatusProvider { statusProvider() }
}

typealias ErrorPayloadGeneratorFactory<T> =
        ErrorPayloadGenerator.Builders.() -> ErrorPayloadGenerator<T, *>
typealias ResponseStatusProviderFactory = ResponseStatusProviders.() -> ResponseStatusProvider
