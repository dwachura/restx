package io.dwsoft.restx

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
import io.dwsoft.restx.core.response.CompositeResponseGenerator
import io.dwsoft.restx.core.response.HttpStatus
import io.dwsoft.restx.core.response.ResponseGenerator
import io.dwsoft.restx.core.response.ResponseGeneratorRegistry
import io.dwsoft.restx.core.response.ResponseStatusProvider
import io.dwsoft.restx.core.response.SimpleResponseGenerator
import io.dwsoft.restx.core.response.TypeBasedResponseGeneratorRegistry
import kotlin.reflect.KClass

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
        var errorPayloadGeneratorFactoryBlock: ErrorPayloadGeneratorFactoryBlock<T>? = null
            private set
        var responseStatusProviderFactoryBlock: ResponseStatusProviderFactoryBlock? = null
            private set

        /**
         * Opens configuration block of [ErrorPayloadGenerator].
         */
        fun representing(factoryBlock: ErrorPayloadGeneratorFactoryBlock<T>) = this.apply {
            errorPayloadGeneratorFactoryBlock = factoryBlock
        }

        /**
         * Opens configuration block of [ResponseStatusProvider].
         */
        fun withStatus(factoryBlock: ResponseStatusProviderFactoryBlock) = this.apply {
            responseStatusProviderFactoryBlock = factoryBlock
        }

        /**
         * Overloaded version of [withStatus] taking [number][status] to create provider
         * returning it as a [status code][HttpStatus.code].
         */
        fun withStatus(status: Int) = withStatus { of(status) }
    }
}

typealias ErrorPayloadGeneratorFactoryBlock<T> =
        FactoryBlock<ErrorPayloadGeneratorBuilders<T>, ErrorPayloadGenerator<T, *>>
typealias ResponseStatusProviderFactoryBlock =
        FactoryBlock<ResponseStatusProvider.Factories, ResponseStatusProvider>

/**
 * Facade to single error payload generators builders
 */
sealed interface SingleErrorPayloadGeneratorBuilders<T : Any> {
    fun operationError(
        initBlock: InitBlock<OperationErrorPayloadGeneratorBuilder.Config<T>>
    ): SingleErrorPayloadGenerator<T> =
        OperationErrorPayloadGeneratorBuilder.buildFrom(
            OperationErrorPayloadGeneratorBuilder.Config<T>().apply(initBlock)
        )

    fun requestDataError(
        initBlock: InitBlock<RequestDataErrorPayloadGeneratorBuilder.Config<T>>
    ): SingleErrorPayloadGenerator<T> =
        RequestDataErrorPayloadGeneratorBuilder.buildFrom(
            RequestDataErrorPayloadGeneratorBuilder.Config<T>().apply(initBlock)
        )
}

private class InstantiableSingleErrorPayloadGeneratorBuilders<T : Any> : SingleErrorPayloadGeneratorBuilders<T>

/**
 * Facade to multi error payload generators builders
 */
class ErrorPayloadGeneratorBuilders<T : Any> : SingleErrorPayloadGeneratorBuilders<T> {
    fun <R : Any> compositeOf(
        initBlock: InitBlock<MultiErrorPayloadGeneratorBuilder.Config<T, R>>
    ): MultiErrorPayloadGenerator<T, R> =
        MultiErrorPayloadGeneratorBuilder.buildFrom(
            MultiErrorPayloadGeneratorBuilder.Config<T, R>().apply(initBlock)
        )

    /**
     * Delegate of [compositeOf]. May be more readable in some situations.
     */
    @Suppress("UNUSED_PARAMETER")
    fun <R : Any> compositeOfErrorsOfType(
        faultObjectsType: KClass<T>,
        initBlock: InitBlock<MultiErrorPayloadGeneratorBuilder.Config<T, R>>
    ): MultiErrorPayloadGenerator<T, R> = compositeOf(initBlock)
}

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
        var subErrorExtractor: SubErrorExtractor<T, R>? = null
            private set
        var subErrorPayloadGenerator: SingleErrorPayloadGenerator<R>? = null
            private set

        /**
         * Opens configuration block of [SubErrorExtractor].
         */
        fun extractedAs(extractor: SubErrorExtractor<T, R>) = this.apply {
            subErrorExtractor = extractor
        }

        /**
         * Sets [generator] that will be used to process
         * [extracted][MultiErrorPayloadGenerator.subErrorExtractor] sub-errors.
         */
        fun eachHandledBy(generator: SingleErrorPayloadGenerator<R>) = this.apply {
            subErrorPayloadGenerator = generator
        }

        /**
         * Opens configuration block of [SingleErrorPayloadGenerator].
         */
        fun eachRepresenting(factoryBlock: SingleErrorPayloadGeneratorBuildersFactoryBlock<R>) =
            eachHandledBy(factoryBlock(InstantiableSingleErrorPayloadGeneratorBuilders()))
    }
}

typealias SingleErrorPayloadGeneratorBuildersFactoryBlock<T> =
        FactoryBlock<SingleErrorPayloadGeneratorBuilders<T>, SingleErrorPayloadGenerator<T>>

/**
 * Base configuration for [SingleErrorPayloadGenerator]'s builders.
 *
 * Passed faults, by default:
 *  - are identified by [their runtime type name][CauseResolver.Factories.type] 
 *  (if not explicitly [configured][identifiedBy])
 *  - have the code [the same as their cause's id][CodeResolver.Factories.sameAsCauseId] 
 *  (if not explicitly [configured][withCode])
 */
sealed class SingleErrorPayloadGeneratorBuilderConfig<T : Any> {
    var causeResolverFactoryBlock: CauseResolverFactoryBlock<T> = { type() }
        private set
    var codeResolverFactoryBlock: CodeResolverFactoryBlock<T> = { sameAsCauseId() }
        private set
    var messageResolverFactoryBlock: MessageResolverFactoryBlock<T>? = null
        private set

    /**
     * Opens configuration block of [CauseResolver].
     */
    fun identifiedBy(factoryBlock: CauseResolverFactoryBlock<T>) = this.apply {
        causeResolverFactoryBlock = factoryBlock
    }

    /**
     * Overloaded version of [identifiedBy] returning [fixed resolver][CauseResolver.fixedId].
     */
    fun identifiedBy(fixedId: String) = identifiedBy { fixedId(fixedId) }

    /**
     * Opens configuration block of [CodeResolver].
     */
    fun withCode(factoryBlock: CodeResolverFactoryBlock<T>) = this.apply {
        codeResolverFactoryBlock = factoryBlock
    }

    /**
     * Overloaded version of [withCode] returning [fixed resolver][CodeResolver.fixed].
     */
    fun withCode(code: String) = this.apply {
        codeResolverFactoryBlock = { fixed(code) }
    }

    /**
     * Opens configuration block of [MessageResolver].
     */
    fun withMessage(factoryBlock: MessageResolverFactoryBlock<T>) = this.apply {
        messageResolverFactoryBlock = factoryBlock
    }

    /**
     * Overloaded version of [withMessage] returning [fixed resolver][MessageResolver.fixed].
     */
    fun withMessage(message: String) = this.apply {
        messageResolverFactoryBlock = { fixed(message) }
    }
}

typealias CauseResolverFactoryBlock<T> = FactoryBlock<CauseResolver.Factories, CauseResolver<T>>
typealias CodeResolverFactoryBlock<T> = FactoryBlock<CodeResolver.Factories, CodeResolver<T>>
typealias MessageResolverFactoryBlock<T> = FactoryBlock<MessageResolver.Factories, MessageResolver<T>>

object OperationErrorPayloadGeneratorBuilder {
    fun <T : Any> buildFrom(config: Config<T>): SingleErrorPayloadGenerator<T> {
        val messageResolverFactoryBlock =
            config.messageResolverFactoryBlock
                ?: throw IllegalArgumentException("Message resolver factory block not set")
        return SingleErrorPayloadGenerator(
            config.causeResolverFactoryBlock(CauseResolver.Factories),
            OperationErrorProcessor(
                config.codeResolverFactoryBlock(CodeResolver.Factories),
                messageResolverFactoryBlock(MessageResolver.Factories)
            )
        )
    }

    class Config<T : Any> : SingleErrorPayloadGeneratorBuilderConfig<T>()
}

object RequestDataErrorPayloadGeneratorBuilder {
    fun <T : Any> buildFrom(config: Config<T>): SingleErrorPayloadGenerator<T> {
        val messageResolverFactoryBlock =
            config.messageResolverFactoryBlock
                ?: throw IllegalArgumentException("Message resolver factory block not set")
        val dataErrorSourceResolverFactoryBlock =
            config.dataErrorSourceResolverFactoryBlock
                ?: throw IllegalArgumentException("Data error source resolver factory block not set")
        return SingleErrorPayloadGenerator(
            config.causeResolverFactoryBlock(CauseResolver.Factories),
            RequestDataErrorProcessor(
                config.codeResolverFactoryBlock(CodeResolver.Factories),
                messageResolverFactoryBlock(MessageResolver.Factories),
                dataErrorSourceResolverFactoryBlock(DataErrorSourceResolver.Factories)
            )
        )
    }

    class Config<T : Any> : SingleErrorPayloadGeneratorBuilderConfig<T>() {
        var dataErrorSourceResolverFactoryBlock: DataErrorSourceResolverFactoryBlock<T>? = null
            private set

        /**
         * Opens configuration block of [DataErrorSourceResolver].
         */
        fun pointingInvalidValue(factoryBlock: DataErrorSourceResolverFactoryBlock<T>) = this.apply {
            dataErrorSourceResolverFactoryBlock = factoryBlock
        }
    }
}

typealias DataErrorSourceResolverFactoryBlock<T> =
        FactoryBlock<DataErrorSourceResolver.Factories, DataErrorSourceResolver<T>>

object CompositeResponseGeneratorBuilder {
    fun buildFrom(config: Config): CompositeResponseGenerator {
        val responseGeneratorRegistryFactoryBlock =
            config.responseGeneratorRegistryFactoryBlock
                ?: throw IllegalArgumentException("Sub-generator registry factory block not set")
        return CompositeResponseGenerator(responseGeneratorRegistryFactoryBlock(Unit))
    }

    class Config {
        var responseGeneratorRegistryFactoryBlock: ResponseGeneratorRegistryFactoryBlock? = null
            private set

        /**
         * Configures [TypeBasedResponseGeneratorRegistry] to be used as [generator's][CompositeResponseGenerator]
         * source of sub-generators.
         */
        fun registeredByFaultType(
            initBlock: InitBlock<TypeBasedResponseGeneratorRegistryBuilder.Config>
        ) = this.apply {
            responseGeneratorRegistryFactoryBlock = {
                TypeBasedResponseGeneratorRegistryBuilder.Config()
                    .apply(initBlock)
                    .let { TypeBasedResponseGeneratorRegistryBuilder.buildFrom(it) }
            }
        }
    }
}

typealias ResponseGeneratorRegistryFactoryBlock = FactoryBlock<Unit, ResponseGeneratorRegistry>

object TypeBasedResponseGeneratorRegistryBuilder {
    fun buildFrom(config: Config): TypeBasedResponseGeneratorRegistry =
        TypeBasedResponseGeneratorRegistry(
            config.generatorsByFaultType.takeIf { it.isNotEmpty() }
                ?: throw IllegalArgumentException("Response generator registry cannot be empty")
        )

    class Config {
        var generatorsByFaultType: MutableMap<KClass<*>, ResponseGenerator<*>> = mutableMapOf()
            private set

        /**
         * Registers passed [mapping][pair] between fault's type and [ResponseGenerator].
         */
        fun <T : Any> map(pair: Pair<KClass<T>, ResponseGenerator<T>>) =
            this.apply { generatorsByFaultType.plusAssign(pair) }

        /**
         * Overloaded version of [map], that opens configuration block of mapping
         * between [fault's type][faultType] and [ResponseGenerator].
         */
        fun <T : Any> map(faultType: KClass<T>, generatorFactoryBlock: ResponseGeneratorFactoryBlock<T>) =
            map(faultType to generatorFactoryBlock(RestX))

        /**
         * Overloaded, inline version of [map].
         */
        inline fun <reified T : Any> register(generatorFactoryBlock: ResponseGeneratorFactoryBlock<T>) =
            map(T::class to generatorFactoryBlock(RestX))
    }
}

typealias ResponseGeneratorFactoryBlock<T> = FactoryBlock<RestX, ResponseGenerator<T>>
