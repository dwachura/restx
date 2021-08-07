package io.dwsoft.restx.fault.cause

import io.dwsoft.restx.FactoryBlock
import io.dwsoft.restx.InitBlock
import io.dwsoft.restx.RestXException
import io.dwsoft.restx.fault.cause.code.CauseCodeProvider
import io.dwsoft.restx.fault.cause.code.CauseCodeProviders
import io.dwsoft.restx.fault.cause.message.CauseMessageProvider
import io.dwsoft.restx.fault.cause.message.CauseMessageProviders
import io.dwsoft.restx.fault.payload.ApiError
import io.dwsoft.restx.initLog

/**
 * Interface for processors of [Cause] that convert them into [ApiError] objects.
 *
 * @param T type of fault object that cause info is created for
 */
fun interface CauseProcessor<T : Any> {
    /**
     * Method responsible for converting [Cause] into corresponding [ApiError].
     *
     * @throws CauseProcessingFailed in case of processing failure
     */
    fun process(cause: Cause<T>): ApiError
}

class CauseProcessingFailed(throwable: Throwable) : RestXException(throwable)

/**
 * RestX's standard implementation of [CauseProcessor].
 */
class StandardCauseProcessor<T : Any>(
    private val causeCodeProvider: CauseCodeProvider<T>,
    private val causeMessageProvider: CauseMessageProvider<T>
) : CauseProcessor<T> {
    private val log = CauseProcessor::class.initLog()

    override fun process(cause: Cause<T>): ApiError {
        log.info { "Processing cause $cause" }
        val (code, message) = runCatching {
            Pair(
                causeCodeProvider.codeFor(cause),
                causeMessageProvider.messageFor(cause)
            )
        }.fold(
            onSuccess = { it },
            onFailure = { throw CauseProcessingFailed(it) }
        )
        return ApiError(code, message)
    }

    companion object Builder {
        fun <T : Any> buildFrom(config: Config<T>): StandardCauseProcessor<T> {
            checkNotNull(config.causeMessageProviderFactoryBlock) { "Message provider factory block not set" }
            return StandardCauseProcessor(
                config.causeCodeProviderFactoryBlock(CauseCodeProviders),
                (config.causeMessageProviderFactoryBlock!!)(CauseMessageProviders)
            )
        }

        /**
         * Configuration of [StandardCauseProcessor]'s [Builder].
         *
         * If not explicitly [configured][code], objects produced by such processor have their
         * code equal to [id of the fault object][Cause.id] for which are generated.
         */
        class Config<T : Any> {
            var causeCodeProviderFactoryBlock: CauseCodeProviderFactoryBlock<T> = { generatedAs { id } }
                private set
            var causeMessageProviderFactoryBlock: (CauseMessageProviderFactoryBlock<T>)? = null
                private set

            fun code(factoryBlock: CauseCodeProviderFactoryBlock<T>) {
                this.causeCodeProviderFactoryBlock = factoryBlock
            }

            fun message(factoryBlock: CauseMessageProviderFactoryBlock<T>) {
                this.causeMessageProviderFactoryBlock = factoryBlock
            }
        }
    }
}

typealias CauseCodeProviderFactoryBlock<T> = FactoryBlock<CauseCodeProviders, CauseCodeProvider<T>>
typealias CauseMessageProviderFactoryBlock<T> = FactoryBlock<CauseMessageProviders, CauseMessageProvider<T>>

/**
 * Extension function serving as a shortcut to configure cause processor builder to create
 * processor with [fixed code provider][CauseCodeProviders.fixed].
 */
fun <T : Any> StandardCauseProcessor.Builder.Config<T>.code(fixed: String) = code { fixed(fixed) }

/**
 * Extension function serving as a shortcut to configure cause processor builder to create
 * processor with [fixed message provider][CauseMessageProviders.fixed].
 */
fun <T : Any> StandardCauseProcessor.Builder.Config<T>.message(fixed: String) = message { fixed(fixed) }

/**
 * Factories of [CauseProcessor]s.
 * Additional factory methods should be added as an extension functions.
 */
object CauseProcessors {
    /**
     * Factory method that creates [standard implementation][StandardCauseProcessor] of [CauseProcessor].
     */
    fun <T : Any> standard(
        initBlock: InitBlock<StandardCauseProcessor.Builder.Config<T>>
    ): CauseProcessor<T> = StandardCauseProcessor.buildFrom(
        StandardCauseProcessor.Builder.Config<T>().apply(initBlock)
    )
}