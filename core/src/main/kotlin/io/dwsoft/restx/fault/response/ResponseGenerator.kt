package io.dwsoft.restx.fault.response

import io.dwsoft.restx.FactoryBlock
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
            checkNotNull(config.errorPayloadGeneratorFactoryBlock) { "Payload generator factory block not set" }
            checkNotNull(config.responseStatusProviderFactoryBlock) { "Status provider factory block not set" }
            return ResponseGenerator(
                (config.errorPayloadGeneratorFactoryBlock!!)(ErrorPayloadGenerator.Builders()),
                (config.responseStatusProviderFactoryBlock!!)(ResponseStatusProviders)
            )
        }

        class Config<T : Any> {
            var errorPayloadGeneratorFactoryBlock: (ErrorPayloadGeneratorFactoryBlock<T>)? = null
                private set
            var responseStatusProviderFactoryBlock: (ResponseStatusProviderFactoryBlock)? = null
                private set

            fun payload(factoryBlock: ErrorPayloadGeneratorFactoryBlock<T>) = this.apply {
                errorPayloadGeneratorFactoryBlock = factoryBlock
            }

            fun status(factoryBlock: ResponseStatusProviderFactoryBlock) = this.apply {
                responseStatusProviderFactoryBlock = factoryBlock
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
    fun of(status: Int) = ResponseStatusProvider { HttpStatus(status) }
    fun of(status: () -> Int) = ResponseStatusProvider { HttpStatus(status()) }
    fun providedBy(statusProvider: () -> HttpStatus) = ResponseStatusProvider { statusProvider() }
}

typealias ErrorPayloadGeneratorFactoryBlock<T> =
        FactoryBlock<ErrorPayloadGenerator.Builders<T>, ErrorPayloadGenerator<T, *>>
typealias ResponseStatusProviderFactoryBlock = FactoryBlock<ResponseStatusProviders, ResponseStatusProvider>

/**
 * Extension function serving as a shortcut to configure response generator builder to create
 * generator of responses with passed status value.
 */
fun <T : Any> ResponseGenerator.Builder.Config<T>.status(status: Int) = status { of(status) }