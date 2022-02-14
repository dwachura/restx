package io.dwsoft.restx.core.cause

import io.dwsoft.restx.RestXException
import io.dwsoft.restx.core.Logging.initLog
import io.dwsoft.restx.core.cause.code.CodeResolver
import io.dwsoft.restx.core.cause.message.MessageResolver
import io.dwsoft.restx.core.payload.OperationError
import io.dwsoft.restx.core.payload.RequestDataError
import io.dwsoft.restx.core.payload.RequestDataError.Source
import io.dwsoft.restx.core.payload.SingleErrorPayload

/**
 * Interface for processors of [Cause] that convert them into [SingleErrorPayload] objects.
 *
 * @param T type of fault object that cause info is created for
 */
fun interface CauseProcessor<T : Any> {
    /**
     * Method responsible for converting [Cause] into corresponding [SingleErrorPayload].
     *
     * @throws CauseProcessingException in case of processing failure
     */
    fun process(cause: Cause<T>): SingleErrorPayload
}

class CauseProcessingException(throwable: Throwable) : RestXException(throwable)

/**
 * RestX's standard implementation of payload's [code][SingleErrorPayload.code]
 * and [message][SingleErrorPayload.message] generation.
 */
private fun <T : Any> createStandardCodeAndMessageResolver(
    codeResolver: CodeResolver<T>,
    messageResolver: MessageResolver<T>
) = { cause: Cause<T> ->
    runCatching {
        Pair(
            codeResolver.codeFor(cause),
            messageResolver.messageFor(cause)
        )
    }.fold(onSuccess = { it }, onFailure = { throw CauseProcessingException(it) })
}

/**
 * Implementation of [CauseProcessor] that generates [payloads of request processing errors][OperationError].
 */
class OperationErrorProcessor<T : Any>(
    codeResolver: CodeResolver<T>,
    messageResolver: MessageResolver<T>
) : CauseProcessor<T> {
    private val log = initLog()
    private val codeAndMessageOf = createStandardCodeAndMessageResolver(codeResolver, messageResolver)

    override fun process(cause: Cause<T>): OperationError {
        log.info { "Processing cause $cause" }
        val (code, message) = codeAndMessageOf(cause)
        return OperationError(code, message)
    }
}

/**
 * Implementation of [CauseProcessor] that generates [payloads of invalid request data errors][RequestDataError].
 */
class RequestDataErrorProcessor<T : Any>(
    codeResolver: CodeResolver<T>,
    messageResolver: MessageResolver<T>,
    private val dataErrorSourceResolver: DataErrorSourceResolver<T>
) : CauseProcessor<T> {
    private val log = initLog()
    private val codeAndMessageOf = createStandardCodeAndMessageResolver(codeResolver, messageResolver)

    override fun process(cause: Cause<T>): RequestDataError {
        log.info { "Processing cause $cause" }
        val (code, message) = codeAndMessageOf(cause)
        val source = runCatching { dataErrorSourceResolver.sourceOf(cause) }
            .fold(onSuccess = { it }, onFailure = { throw CauseProcessingException(it) })
        return RequestDataError(code, message, source)
    }
}

/**
 * Interface of [data error sources][Source] resolvers.
 */
fun interface DataErrorSourceResolver<T : Any> {
    fun sourceOf(cause: Cause<T>): Source

    /**
     * Factories of [DataErrorSourceResolver]s.
     * Additional factory methods should be added as an extension functions.
     */
    companion object Factories {
        fun <T : Any> resolvedBy(resolver: DataErrorSourceResolver<T>) = resolver
    }
}
operator fun <T : Any> DataErrorSourceResolver<T>.invoke(cause: Cause<T>) = this.sourceOf(cause)
