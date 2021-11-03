package io.dwsoft.restx.core.cause

import io.dwsoft.restx.FactoryBlock
import io.dwsoft.restx.InitBlock
import io.dwsoft.restx.RestXException
import io.dwsoft.restx.core.cause.code.CodeResolver
import io.dwsoft.restx.core.cause.code.CodeResolvers
import io.dwsoft.restx.core.cause.message.MessageResolver
import io.dwsoft.restx.core.cause.message.MessageResolvers
import io.dwsoft.restx.core.payload.OperationError
import io.dwsoft.restx.core.payload.RequestDataError
import io.dwsoft.restx.core.payload.SingleErrorPayload
import io.dwsoft.restx.core.payload.Source
import io.dwsoft.restx.core.Logging.initLog

/**
 * Interface for processors of [Cause] that convert them into [SingleErrorPayload] objects.
 *
 * @param T type of fault object that cause info is created for
 */
fun interface CauseProcessor<T : Any> {
    /**
     * Method responsible for converting [Cause] into corresponding [SingleErrorPayload].
     *
     * @throws CauseProcessingFailure in case of processing failure
     */
    fun process(cause: Cause<T>): SingleErrorPayload
}

class CauseProcessingFailure(throwable: Throwable) : RestXException(throwable)

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
    }.fold(
        onSuccess = { it },
        onFailure = { throw CauseProcessingFailure(it) }
    )
}

/**
 * Standard implementation of configuration for [CauseProcessor]'s builders.
 *
 * If not explicitly [configured][code], objects produced by such processor have their
 * code equal to [id of the fault object][Cause.id] for which are generated.
 */
sealed class StandardConfig<T : Any> {
    var codeResolverFactoryBlock: CodeResolverFactoryBlock<T> = { sameAsCauseId() }
        private set
    var messageResolverFactoryBlock: (MessageResolverFactoryBlock<T>)? = null
        private set

    fun code(factoryBlock: CodeResolverFactoryBlock<T>) = this.apply {
        codeResolverFactoryBlock = factoryBlock
    }

    fun message(factoryBlock: MessageResolverFactoryBlock<T>) = this.apply {
        messageResolverFactoryBlock = factoryBlock
    }
}

typealias CodeResolverFactoryBlock<T> = FactoryBlock<CodeResolvers, CodeResolver<T>>
typealias MessageResolverFactoryBlock<T> = FactoryBlock<MessageResolvers, MessageResolver<T>>

/**
 * Extension function serving as a shortcut to configure cause processor builder to create
 * processor with [fixed code resolver][CodeResolvers.fixed].
 */
fun <T : Any> StandardConfig<T>.code(fixed: String) = code { fixed(fixed) }

/**
 * Extension function serving as a shortcut to configure cause processor builder to create
 * processor with [fixed message resolver][MessageResolvers.fixed].
 */
fun <T : Any> StandardConfig<T>.message(fixed: String) = message { fixed(fixed) }

/**
 * Implementation of [CauseProcessor] that generates [payloads of request processing errors][OperationError].
 */
class OperationErrorProcessor<T : Any>(
    codeResolver: CodeResolver<T>,
    messageResolver: MessageResolver<T>
) : CauseProcessor<T> {
    private val log = initLog()
    private val codeAndMessageOf =
        createStandardCodeAndMessageResolver(codeResolver, messageResolver)

    override fun process(cause: Cause<T>): OperationError {
        log.info { "Processing cause $cause" }
        val (code, message) = codeAndMessageOf(cause)
        return OperationError(code, message)
    }

    companion object Builder {
        fun <T : Any> buildFrom(config: Config<T>): OperationErrorProcessor<T> {
            val messageResolverFactoryBlock =
                config.messageResolverFactoryBlock
                    ?: throw IllegalArgumentException("Message resolver factory block not set")
            return OperationErrorProcessor(
                config.codeResolverFactoryBlock(CodeResolvers),
                messageResolverFactoryBlock(MessageResolvers)
            )
        }

        /**
         * Configuration of [OperationErrorProcessor]'s [Builder].
         */
        class Config<T : Any> : StandardConfig<T>()
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
    private val codeAndMessageOf =
        createStandardCodeAndMessageResolver(codeResolver, messageResolver)

    override fun process(cause: Cause<T>): RequestDataError {
        log.info { "Processing cause $cause" }
        val (code, message) = codeAndMessageOf(cause)
        val source = runCatching {
            dataErrorSourceResolver.sourceOf(cause)
        }.fold(
            onSuccess = { it },
            onFailure = { throw CauseProcessingFailure(it) }
        )
        return RequestDataError(code, message, source)
    }

    companion object Builder {
        fun <T : Any> buildFrom(config: Config<T>): RequestDataErrorProcessor<T> {
            val messageResolverFactoryBlock =
                config.messageResolverFactoryBlock
                    ?: throw IllegalArgumentException("Message resolver factory block not set")
            val dataErrorSourceResolverFactoryBlock =
                config.dataErrorSourceResolverFactoryBlock
                    ?: throw IllegalArgumentException("Data error source resolver factory block not set")
            return RequestDataErrorProcessor(
                config.codeResolverFactoryBlock(CodeResolvers),
                messageResolverFactoryBlock(MessageResolvers),
                dataErrorSourceResolverFactoryBlock(DataErrorSourceResolvers)
            )
        }

        /**
         * Configuration of [RequestDataErrorProcessor]'s [Builder].
         */
        class Config<T : Any> : StandardConfig<T>() {
            var dataErrorSourceResolverFactoryBlock: DataErrorSourceResolverFactoryBlock<T>? = null
                private set

            fun invalidValue(factoryBlock: DataErrorSourceResolverFactoryBlock<T>) = this.apply {
                dataErrorSourceResolverFactoryBlock = factoryBlock
            }
        }
    }
}

/**
 * Interface of [data error sources][Source] resolvers.
 */
fun interface DataErrorSourceResolver<T : Any> {
    fun sourceOf(cause: Cause<T>): Source
}
operator fun <T : Any> DataErrorSourceResolver<T>.invoke(cause: Cause<T>) = this.sourceOf(cause)

/**
 * Factories of [DataErrorSourceResolver]s.
 * Additional factory methods should be added as an extension functions.
 */
object DataErrorSourceResolvers {
    fun <T : Any> resolvedBy(resolver: DataErrorSourceResolver<T>) = resolver
}

typealias DataErrorSourceResolverFactoryBlock<T> = FactoryBlock<DataErrorSourceResolvers, DataErrorSourceResolver<T>>

/**
 * Factories of [CauseProcessor]s.
 * Additional factory methods should be added as an extension functions.
 */
object CauseProcessors {
    /**
     * Factory method that creates [processor handling request processing errors][OperationErrorProcessor].
     */
    fun <T : Any> operationError(
        initBlock: InitBlock<OperationErrorProcessor.Builder.Config<T>>
    ): CauseProcessor<T> = OperationErrorProcessor.buildFrom(
        OperationErrorProcessor.Builder.Config<T>().apply(initBlock)
    )

    /**
     * Factory method that creates [processor handling invalid request data errors][RequestDataErrorProcessor].
     */
    fun <T : Any> requestDataError(
        initBlock: InitBlock<RequestDataErrorProcessor.Builder.Config<T>>
    ): CauseProcessor<T> = RequestDataErrorProcessor.buildFrom(
        RequestDataErrorProcessor.Builder.Config<T>().apply(initBlock)
    )
}
