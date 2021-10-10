package io.dwsoft.restx.fault.cause

import io.dwsoft.restx.FactoryBlock
import io.dwsoft.restx.InitBlock
import io.dwsoft.restx.RestXException
import io.dwsoft.restx.fault.cause.code.CauseCodeProvider
import io.dwsoft.restx.fault.cause.code.CauseCodeProviders
import io.dwsoft.restx.fault.cause.message.CauseMessageProvider
import io.dwsoft.restx.fault.cause.message.CauseMessageProviders
import io.dwsoft.restx.fault.payload.OperationError
import io.dwsoft.restx.fault.payload.RequestDataError
import io.dwsoft.restx.fault.payload.SingleErrorPayload
import io.dwsoft.restx.fault.payload.Source
import io.dwsoft.restx.Logging.initLog

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
private fun <T : Any> createStandardPayloadCodeAndMessageProvider(
    causeCodeProvider: CauseCodeProvider<T>,
    causeMessageProvider: CauseMessageProvider<T>
) = { cause: Cause<T> ->
    runCatching {
        Pair(
            causeCodeProvider.codeFor(cause),
            causeMessageProvider.messageFor(cause)
        )
    }.fold(
        onSuccess = { it },
        onFailure = { throw CauseProcessingFailure(it) }
    )
}

/**
 * Standard implementation of configuration for [CauseProcessor]'s [Builder]'s.
 *
 * If not explicitly [configured][code], objects produced by such processor have their
 * code equal to [id of the fault object][Cause.id] for which are generated.
 */
sealed class StandardConfig<T : Any> {
    var causeCodeProviderFactoryBlock: CauseCodeProviderFactoryBlock<T> = { generatedAs { id } }
        private set
    var causeMessageProviderFactoryBlock: (CauseMessageProviderFactoryBlock<T>)? = null
        private set

    fun code(factoryBlock: CauseCodeProviderFactoryBlock<T>) = this.apply {
        causeCodeProviderFactoryBlock = factoryBlock
    }

    fun message(factoryBlock: CauseMessageProviderFactoryBlock<T>) = this.apply {
        causeMessageProviderFactoryBlock = factoryBlock
    }
}

typealias CauseCodeProviderFactoryBlock<T> = FactoryBlock<CauseCodeProviders, CauseCodeProvider<T>>
typealias CauseMessageProviderFactoryBlock<T> = FactoryBlock<CauseMessageProviders, CauseMessageProvider<T>>

/**
 * Extension function serving as a shortcut to configure cause processor builder to create
 * processor with [fixed code provider][CauseCodeProviders.fixed].
 */
fun <T : Any> StandardConfig<T>.code(fixed: String) = code { fixed(fixed) }

/**
 * Extension function serving as a shortcut to configure cause processor builder to create
 * processor with [fixed message provider][CauseMessageProviders.fixed].
 */
fun <T : Any> StandardConfig<T>.message(fixed: String) = message { fixed(fixed) }

/**
 * Implementation of [CauseProcessor] that generates [payloads of request processing errors][OperationError].
 */
class OperationErrorProcessor<T : Any>(
    causeCodeProvider: CauseCodeProvider<T>,
    causeMessageProvider: CauseMessageProvider<T>
) : CauseProcessor<T> {
    private val log = initLog()
    private val codeAndMessageOf =
        createStandardPayloadCodeAndMessageProvider(causeCodeProvider, causeMessageProvider)

    override fun process(cause: Cause<T>): OperationError {
        log.info { "Processing cause $cause" }
        val (code, message) = codeAndMessageOf(cause)
        return OperationError(code, message)
    }

    companion object Builder {
        fun <T : Any> buildFrom(config: Config<T>): OperationErrorProcessor<T> {
            val causeMessageProviderFactoryBlock =
                config.causeMessageProviderFactoryBlock
                    ?: throw IllegalArgumentException("Message provider factory block not set")
            return OperationErrorProcessor(
                config.causeCodeProviderFactoryBlock(CauseCodeProviders),
                causeMessageProviderFactoryBlock(CauseMessageProviders)
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
    causeCodeProvider: CauseCodeProvider<T>,
    causeMessageProvider: CauseMessageProvider<T>,
    private val dataErrorSourceResolver: DataErrorSourceResolver<T>
) : CauseProcessor<T> {
    private val log = initLog()
    private val codeAndMessageOf =
        createStandardPayloadCodeAndMessageProvider(causeCodeProvider, causeMessageProvider)

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
            val causeMessageProviderFactoryBlock =
                config.causeMessageProviderFactoryBlock
                    ?: throw IllegalArgumentException("Message provider factory block not set")
            val dataErrorSourceResolverFactoryBlock =
                config.dataErrorSourceResolverFactoryBlock
                    ?: throw IllegalArgumentException("Data error source provider factory block not set")
            return RequestDataErrorProcessor(
                config.causeCodeProviderFactoryBlock(CauseCodeProviders),
                causeMessageProviderFactoryBlock(CauseMessageProviders),
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
