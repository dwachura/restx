package io.dwsoft.restx

import io.dwsoft.restx.core.cause.Cause
import io.dwsoft.restx.core.cause.CauseProcessor
import io.dwsoft.restx.core.cause.CauseResolver
import io.dwsoft.restx.core.cause.DataErrorSourceResolver
import io.dwsoft.restx.core.cause.OperationErrorProcessor
import io.dwsoft.restx.core.cause.RequestDataErrorProcessor
import io.dwsoft.restx.core.cause.code.CodeResolver
import io.dwsoft.restx.core.cause.message.MessageResolver
import io.dwsoft.restx.core.payload.ErrorPayloadGenerator
import io.dwsoft.restx.core.payload.MultiErrorPayloadGenerator
import io.dwsoft.restx.core.payload.SingleErrorPayloadGenerator
import io.dwsoft.restx.core.payload.SubErrorExtractor
import io.dwsoft.restx.core.response.ResponseGenerator
import io.dwsoft.restx.core.response.ResponseStatusProvider
import io.dwsoft.restx.core.response.SimpleResponseGenerator

object SimpleResponseGeneratorBuilder {
    fun <T : Any> buildFrom(config: Config<T>): ResponseGenerator<T> {
        val errorPayloadGeneratorFactoryBlock =
            config.errorPayloadGeneratorFactoryBlock
                ?: throw IllegalArgumentException("Payload generator factory block not set")
        val responseStatusProviderFactoryBlock =
            config.responseStatusProviderFactoryBlock
                ?: throw IllegalArgumentException("Status provider factory block not set")
        return SimpleResponseGenerator(
            errorPayloadGeneratorFactoryBlock(ErrorPayloadGeneratorBuilders()),
            responseStatusProviderFactoryBlock(ResponseStatusProvider.Factories)
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

        fun processedAs(factoryBlock: ErrorPayloadGeneratorFactoryBlock<T>) = this.apply {
            errorPayloadGeneratorFactoryBlock = factoryBlock
        }

        fun status(factoryBlock: ResponseStatusProviderFactoryBlock) = this.apply {
            responseStatusProviderFactoryBlock = factoryBlock
        }
    }
}

typealias ErrorPayloadGeneratorFactoryBlock<T> =
        FactoryBlock<ErrorPayloadGeneratorBuilders<T>, ErrorPayloadGenerator<T, *>>
typealias ResponseStatusProviderFactoryBlock = FactoryBlock<ResponseStatusProvider.Factories, ResponseStatusProvider>

/**
 * Extension function serving as a shortcut to configure response generator builder to create
 * generator of responses with passed status value.
 */
fun <T : Any> SimpleResponseGeneratorBuilder.Config<T>.status(status: Int) = status { of(status) }


/**
 * Facade to payload generators builders
 */
class ErrorPayloadGeneratorBuilders<T : Any> {
    fun error(
        initBlock: InitBlock<SingleErrorPayloadGeneratorBuilder.Config<T>>
    ): SingleErrorPayloadGenerator<T> =
        SingleErrorPayloadGeneratorBuilder.buildFrom(
            SingleErrorPayloadGeneratorBuilder.Config<T>().apply(initBlock)
        )

    fun <R : Any> subErrors(
        initBlock: InitBlock<MultiErrorPayloadGeneratorBuilder.Config<T, R>>
    ): MultiErrorPayloadGenerator<T, R> =
        MultiErrorPayloadGeneratorBuilder.buildFrom(
            MultiErrorPayloadGeneratorBuilder.Config<T, R>().apply(initBlock)
        )
}

object SingleErrorPayloadGeneratorBuilder {
    fun <T : Any> buildFrom(config: Config<T>): SingleErrorPayloadGenerator<T> {
        val causeProcessorFactoryBlock =
            config.causeProcessorFactoryBlock
                ?: throw IllegalArgumentException("Cause processor factory block not set")
        return SingleErrorPayloadGenerator(
            config.causeResolverFactoryBlock(CauseResolver.Factories),
            causeProcessorFactoryBlock(CauseProcessors)
        )
    }

    /**
     * Configuration for [SingleErrorPayloadGenerator]'s [Builder].
     *
     * If not explicitly [configured][identifiedBy], passed faults are [identified by their runtime type name]
     * [CauseResolver.Factories.type].
     */
    class Config<T : Any> {
        var causeResolverFactoryBlock: (CauseResolverFactoryBlock<T>) = { CauseResolver.Factories.type() }
            private set
        var causeProcessorFactoryBlock: (CauseProcessorFactoryBlock<T>)? = null
            private set

        fun identifiedBy(factoryBlock: CauseResolverFactoryBlock<T>) = this.apply {
            causeResolverFactoryBlock = factoryBlock
        }

        fun withId(fixedId: String) = identifiedBy { CauseResolver.Factories.fixedId(fixedId) }

        fun processedAs(factoryBlock: CauseProcessorFactoryBlock<T>) = this.apply {
            causeProcessorFactoryBlock = factoryBlock
        }
    }
}

typealias CauseResolverFactoryBlock<T> = FactoryBlock<CauseResolver.Factories, CauseResolver<T>>
typealias CauseProcessorFactoryBlock<T> = FactoryBlock<CauseProcessors, CauseProcessor<T>>

object MultiErrorPayloadGeneratorBuilder {
    fun <T : Any, R : Any> buildFrom(config: Config<T, R>): MultiErrorPayloadGenerator<T, R> {
        val subErrorExtractor =
            config.subErrorExtractor
                ?: throw IllegalArgumentException("Sub-error extractor must be provided")
        val subErrorPayloadGenerator =
            config.subErrorPayloadGenerator
                ?: throw IllegalArgumentException("Sub-error payload generator must be provided")
        return MultiErrorPayloadGenerator(subErrorExtractor, subErrorPayloadGenerator)
    }

    class Config<T : Any, R : Any> {
        var subErrorExtractor: (SubErrorExtractor<T, R>)? = null
            private set
        var subErrorPayloadGenerator: (SingleErrorPayloadGenerator<R>)? = null
            private set

        fun extractedAs(extractor: SubErrorExtractor<T, R>) = this.apply {
            subErrorExtractor = extractor
        }

        fun handledBy(generator: SingleErrorPayloadGenerator<R>) = this.apply {
            subErrorPayloadGenerator = generator
        }

        fun whichAre(initBlock: InitBlock<SingleErrorPayloadGeneratorBuilder.Config<R>>) = handledBy(
            SingleErrorPayloadGeneratorBuilder.Config<R>().apply(initBlock)
                .let { SingleErrorPayloadGeneratorBuilder.buildFrom(it) }
        )
    }
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

typealias CodeResolverFactoryBlock<T> = FactoryBlock<CodeResolver.Factories, CodeResolver<T>>
typealias MessageResolverFactoryBlock<T> = FactoryBlock<MessageResolver.Factories, MessageResolver<T>>

/**
 * Extension function serving as a shortcut to configure cause processor builder to create
 * processor with [fixed code resolver][CodeResolver.Factories.fixed].
 */
fun <T : Any> StandardConfig<T>.code(fixed: String) = code { fixed(fixed) }

/**
 * Extension function serving as a shortcut to configure cause processor builder to create
 * processor with [fixed message resolver][MessageResolver.Factories.fixed].
 */
fun <T : Any> StandardConfig<T>.message(fixed: String) = message { fixed(fixed) }

object OperationErrorProcessorBuilder {
    fun <T : Any> buildFrom(config: Config<T>): OperationErrorProcessor<T> {
        val messageResolverFactoryBlock =
            config.messageResolverFactoryBlock
                ?: throw IllegalArgumentException("Message resolver factory block not set")
        return OperationErrorProcessor(
            config.codeResolverFactoryBlock(CodeResolver.Factories),
            messageResolverFactoryBlock(MessageResolver.Factories)
        )
    }

    /**
     * Configuration of [OperationErrorProcessor]'s [Builder].
     */
    class Config<T : Any> : StandardConfig<T>()
}

object RequestDataErrorProcessorBuilder {
    fun <T : Any> buildFrom(config: Config<T>): RequestDataErrorProcessor<T> {
        val messageResolverFactoryBlock =
            config.messageResolverFactoryBlock
                ?: throw IllegalArgumentException("Message resolver factory block not set")
        val dataErrorSourceResolverFactoryBlock =
            config.dataErrorSourceResolverFactoryBlock
                ?: throw IllegalArgumentException("Data error source resolver factory block not set")
        return RequestDataErrorProcessor(
            config.codeResolverFactoryBlock(CodeResolver.Factories),
            messageResolverFactoryBlock(MessageResolver.Factories),
            dataErrorSourceResolverFactoryBlock(DataErrorSourceResolver.Factories)
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

typealias DataErrorSourceResolverFactoryBlock<T> = FactoryBlock<DataErrorSourceResolver.Factories, DataErrorSourceResolver<T>>

/**
 * Factories of [CauseProcessor]s.
 * Additional factory methods should be added as an extension functions.
 */
object CauseProcessors {
    /**
     * Factory method that creates [processor handling request processing errors][OperationErrorProcessor].
     */
    fun <T : Any> operationError(
        initBlock: InitBlock<OperationErrorProcessorBuilder.Config<T>>
    ): CauseProcessor<T> = OperationErrorProcessorBuilder.buildFrom(
        OperationErrorProcessorBuilder.Config<T>().apply(initBlock)
    )

    /**
     * Factory method that creates [processor handling invalid request data errors][RequestDataErrorProcessor].
     */
    fun <T : Any> requestDataError(
        initBlock: InitBlock<RequestDataErrorProcessorBuilder.Config<T>>
    ): CauseProcessor<T> = RequestDataErrorProcessorBuilder.buildFrom(
        RequestDataErrorProcessorBuilder.Config<T>().apply(initBlock)
    )
}
