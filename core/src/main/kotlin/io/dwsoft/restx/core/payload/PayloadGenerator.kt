package io.dwsoft.restx.core.payload

import io.dwsoft.restx.RestXException
import io.dwsoft.restx.core.Logging.initLog
import io.dwsoft.restx.core.cause.CauseProcessor
import io.dwsoft.restx.core.cause.CauseResolver

/**
 * Base interface for generators of error response payloads.
 *
 * @param T type of fault objects that generators of this class supports
 * @param R specific type of [ErrorResponsePayload] generated by objects of this class
 */
sealed interface ErrorPayloadGenerator<in T : Any, out R : ErrorResponsePayload> {
    /**
     * Method that generates payload for given fault result
     *
     * @throws RestXException in case of errors during generation process
     */
    fun payloadOf(fault: T): R
}

/**
 * Generator creating payloads for fault results caused by single errors.
 */
class SingleErrorPayloadGenerator<T : Any>(
    private val causeResolver: CauseResolver<T>,
    private val processor: CauseProcessor<T>
) : ErrorPayloadGenerator<T, SingleErrorPayload> {
    override fun payloadOf(fault: T): SingleErrorPayload {
        return processor.process(causeResolver.causeOf(fault))
    }
}

/**
 * Generator creating payloads for fault results caused by multiple errors.
 *
 * @param R type of sub-errors, that the main fault will be [split][subErrorExtractor] into
 */
class MultiErrorPayloadGenerator<T : Any, R : Any>(
    private val subErrorExtractor: SubErrorExtractor<T, R>,
    private val subErrorPayloadGenerator: SingleErrorPayloadGenerator<R>
) : ErrorPayloadGenerator<T, ErrorResponsePayload> {
    private val log = initLog()

    /**
     * @throws NoSubErrorsExtracted in case [extractor][subErrorExtractor] returns no sub-errors
     */
    override fun payloadOf(fault: T): ErrorResponsePayload {
        return (subErrorExtractor.subErrorsOf(fault).takeIf { it.isNotEmpty() } ?: throw NoSubErrorsExtracted())
            .also { when (it.size) { 1 -> logSingleCauseWarning() } }
            .map { subErrorPayloadGenerator.payloadOf(it) }
            .toPayload()
    }

    private fun logSingleCauseWarning() {
        val singleErrorPayloadGeneratorClassName = SingleErrorPayloadGenerator::class.qualifiedName
        log.warn { "Consider using $singleErrorPayloadGeneratorClassName to handle single cause faults" }
    }
}

/**
 * Interface for objects responsible for extracting sub-errors from multi-cause fault results.
 *
 * @param T type of fault objects that the extractor supports
 * @param R type of sub-error objects that are generated by the extractor
 */
fun interface SubErrorExtractor<T, R> {
    fun subErrorsOf(fault: T): Collection<R>
}
operator fun <T : Any, R : Any> SubErrorExtractor<T, R>.invoke(fault: T): Collection<R> = subErrorsOf(fault)

class NoSubErrorsExtracted : RestXException()
