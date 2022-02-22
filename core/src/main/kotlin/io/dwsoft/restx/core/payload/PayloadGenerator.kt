package io.dwsoft.restx.core.payload

import io.dwsoft.restx.RestXException
import io.dwsoft.restx.core.Logging.initLog
import io.dwsoft.restx.core.cause.CauseResolver
import io.dwsoft.restx.core.cause.DataErrorSourceResolver
import io.dwsoft.restx.core.cause.code.CodeResolver
import io.dwsoft.restx.core.cause.message.MessageResolver

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
     * @throws PayloadGenerationException in case of errors during generation process
     */
    fun payloadOf(fault: T): R
}

class PayloadGenerationException(cause: Throwable) : RestXException(cause)

private fun <T : Any, R : ErrorResponsePayload> ErrorPayloadGenerator<T, R>.processRethrowingPayloadGenerationException(
    task: ErrorPayloadGenerator<T, R>.() -> R
) : R = runCatching(task).fold(onSuccess = { it }, onFailure = { throw PayloadGenerationException(it) })

/**
 * Generator creating [payloads][SingleErrorPayload] for faults caused by single errors.
 */
abstract class SingleErrorPayloadGenerator<T : Any>(
    protected val causeResolver: CauseResolver<T>,
    protected val codeResolver: CodeResolver<T>,
    protected val messageResolver: MessageResolver<T>
) : ErrorPayloadGenerator<T, SingleErrorPayload>

/**
 * [Single-error payloads generator][SingleErrorPayloadGenerator] creating payloads for faults caused by
 * [operation errors][OperationError].
 */
class OperationErrorPayloadGenerator<T : Any>(
    causeResolver: CauseResolver<T>,
    codeResolver: CodeResolver<T>,
    messageResolver: MessageResolver<T>
) : SingleErrorPayloadGenerator<T>(causeResolver, codeResolver, messageResolver) {
    private val log = initLog()

    override fun payloadOf(fault: T): OperationError =
        runCatching {
            causeResolver.causeOf(fault)
                .also { log.info { "Processing cause $it" } }
                .let { Pair(codeResolver.codeFor(it), messageResolver.messageFor(it)) }
                .let { (code, message) -> OperationError(code, message) }
        }.fold(onSuccess = { it }, onFailure = { throw PayloadGenerationException(it) })
}

/**
 * [Single-error payloads generator][SingleErrorPayloadGenerator] creating payloads for faults caused by
 * [request input errors][RequestDataError].
 */
class RequestDataErrorPayloadGenerator<T : Any>(
    causeResolver: CauseResolver<T>,
    codeResolver: CodeResolver<T>,
    messageResolver: MessageResolver<T>,
    private val dataErrorSourceResolver: DataErrorSourceResolver<T>
) : SingleErrorPayloadGenerator<T>(causeResolver, codeResolver, messageResolver) {
    private val log = initLog()

    override fun payloadOf(fault: T): RequestDataError =
        runCatching {
            causeResolver.causeOf(fault)
                .also { log.info { "Processing cause $it" } }
                .let { Triple(
                    codeResolver.codeFor(it),
                    messageResolver.messageFor(it),
                    dataErrorSourceResolver.sourceOf(it)
                ) }
                .let { (code, message, dataErrorSource) -> RequestDataError(code, message, dataErrorSource) }
        }.fold(onSuccess = { it }, onFailure = { throw PayloadGenerationException(it) })
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

    override fun payloadOf(fault: T): ErrorResponsePayload =
        runCatching {
            (subErrorExtractor.subErrorsOf(fault).takeIf { it.isNotEmpty() } ?: throw NoSubErrorsExtracted())
                .also { when (it.size) { 1 -> logSingleCauseWarning() } }
                .map { subErrorPayloadGenerator.payloadOf(it) }
                .toPayload()
        }.fold(onSuccess = { it }, onFailure = { throw PayloadGenerationException(it) })

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
