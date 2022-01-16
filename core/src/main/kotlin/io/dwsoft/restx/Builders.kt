package io.dwsoft.restx

import io.dwsoft.restx.core.cause.CauseResolver
import io.dwsoft.restx.core.cause.DataErrorSourceResolver
import io.dwsoft.restx.core.cause.OperationErrorProcessor
import io.dwsoft.restx.core.cause.RequestDataErrorProcessor
import io.dwsoft.restx.core.cause.code.CodeResolver
import io.dwsoft.restx.core.cause.message.MessageResolver
import io.dwsoft.restx.core.payload.ErrorPayloadGenerator
import io.dwsoft.restx.core.payload.MultiErrorPayloadGenerator
import io.dwsoft.restx.core.payload.OperationError
import io.dwsoft.restx.core.payload.RequestDataError
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

/**
 * Configuration DSL for [SingleErrorPayloadGenerator]s.
 */
sealed interface SingleErrorPayloadGeneratorDsl<T : Any> {
    val causeResolverFactoryBlock: CauseResolverFactoryBlock<T>
    val codeResolverFactoryBlock: CodeResolverFactoryBlock<T>
    val messageResolverFactoryBlock: MessageResolverFactoryBlock<T>?

    /**
     * Opens configuration block of [CauseResolver].
     */
    fun identifiedBy(factoryBlock: CauseResolverFactoryBlock<T>): SingleErrorPayloadGeneratorDsl<T>

    /**
     * Overloaded version of [identifiedBy] returning [fixed resolver][CauseResolver.fixedId].
     */
    fun identifiedBy(fixedId: String) = identifiedBy { fixedId(fixedId) }

    /**
     * Opens configuration block of [CodeResolver].
     */
    fun withCode(factoryBlock: CodeResolverFactoryBlock<T>): SingleErrorPayloadGeneratorDsl<T>
    /**
     * Overloaded version of [withCode] returning [fixed resolver][CodeResolver.fixed].
     */
    fun withCode(code: String) = withCode { fixed(code) }

    /**
     * Opens configuration block of [MessageResolver].
     */
    fun withMessage(factoryBlock: MessageResolverFactoryBlock<T>): SingleErrorPayloadGeneratorDsl<T>

    /**
     * Overloaded version of [withMessage] returning [fixed resolver][MessageResolver.fixed].
     */
    fun withMessage(message: String) = withMessage { fixed(message) }

    /**
     * Default implementation of [SingleErrorPayloadGeneratorDsl].
     *
     * Passed faults, by default:
     *  - are identified by [their runtime type name][CauseResolver.Factories.type]
     *  (if not explicitly [configured][identifiedBy])
     *  - have the code [the same as their cause's id][CodeResolver.Factories.sameAsCauseId]
     *  (if not explicitly [configured][withCode])
     */
    class Default<T : Any> : SingleErrorPayloadGeneratorDsl<T> {
        override var causeResolverFactoryBlock: CauseResolverFactoryBlock<T> = { type() }
            private set
        override var codeResolverFactoryBlock: CodeResolverFactoryBlock<T> = { sameAsCauseId() }
            private set
        override var messageResolverFactoryBlock: MessageResolverFactoryBlock<T>? = null
            private set

        override fun identifiedBy(factoryBlock: CauseResolverFactoryBlock<T>) = this.apply {
            causeResolverFactoryBlock = factoryBlock
        }

        override fun withCode(factoryBlock: CodeResolverFactoryBlock<T>) = this.apply {
            codeResolverFactoryBlock = factoryBlock
        }

        override fun withMessage(factoryBlock: MessageResolverFactoryBlock<T>) = this.apply {
            messageResolverFactoryBlock = factoryBlock
        }
    }
}

typealias CauseResolverFactoryBlock<T> = FactoryBlock<CauseResolver.Factories, CauseResolver<T>>
typealias CodeResolverFactoryBlock<T> = FactoryBlock<CodeResolver.Factories, CodeResolver<T>>
typealias MessageResolverFactoryBlock<T> = FactoryBlock<MessageResolver.Factories, MessageResolver<T>>

/**
 * Configuration DSL for [SingleErrorPayloadGenerator]s, which generate payloads for [OperationError]s.
 */
sealed interface OperationErrorPayloadGeneratorDsl<T : Any> : SingleErrorPayloadGeneratorDsl<T>

object OperationErrorPayloadGeneratorBuilder {
    fun <T : Any> buildFrom(config: OperationErrorPayloadGeneratorDsl<T>): SingleErrorPayloadGenerator<T> {
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

    /**
     * Default implementation of [OperationErrorPayloadGeneratorDsl].
     *
     * Delegates to [SingleErrorPayloadGeneratorDsl.Default].
     */
    class Dsl<T : Any> :
        OperationErrorPayloadGeneratorDsl<T>,
        SingleErrorPayloadGeneratorDsl<T> by SingleErrorPayloadGeneratorDsl.Default()
}

/**
 * Configuration DSL for [SingleErrorPayloadGenerator]s, which generate payloads for [RequestDataError]s.
 */
sealed interface RequestDataErrorPayloadGeneratorDsl<T : Any> : SingleErrorPayloadGeneratorDsl<T> {
    val dataErrorSourceResolverFactoryBlock: DataErrorSourceResolverFactoryBlock<T>?

    /**
     * Opens configuration block of [DataErrorSourceResolver].
     */
    fun pointingInvalidValue(
        factoryBlock: DataErrorSourceResolverFactoryBlock<T>
    ): RequestDataErrorPayloadGeneratorDsl<T>
}

typealias DataErrorSourceResolverFactoryBlock<T> =
        FactoryBlock<DataErrorSourceResolver.Factories, DataErrorSourceResolver<T>>

object RequestDataErrorPayloadGeneratorBuilder {
    fun <T : Any> buildFrom(config: RequestDataErrorPayloadGeneratorDsl<T>): SingleErrorPayloadGenerator<T> {
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

    /**
     * Default implementation of [RequestDataErrorPayloadGeneratorDsl].
     *
     * Delegates to [SingleErrorPayloadGeneratorDsl.Default].
     */
    class Dsl<T : Any> : RequestDataErrorPayloadGeneratorDsl<T>,
        SingleErrorPayloadGeneratorDsl<T> by SingleErrorPayloadGeneratorDsl.Default()
    {
        override var dataErrorSourceResolverFactoryBlock: DataErrorSourceResolverFactoryBlock<T>? = null
            private set

        override fun pointingInvalidValue(factoryBlock: DataErrorSourceResolverFactoryBlock<T>) = this.apply {
            dataErrorSourceResolverFactoryBlock = factoryBlock
        }
    }
}

/**
 * Facade to factories of [SingleErrorPayloadGenerator]s.
 */
sealed interface SingleErrorPayloadGeneratorBuilders<T : Any> {
    fun operationError(
        initBlock: InitBlock<OperationErrorPayloadGeneratorDsl<T>>
    ): SingleErrorPayloadGenerator<T> =
        OperationErrorPayloadGeneratorBuilder.buildFrom(
            OperationErrorPayloadGeneratorBuilder.Dsl<T>().apply(initBlock)
        )

    fun requestDataError(
        initBlock: InitBlock<RequestDataErrorPayloadGeneratorDsl<T>>
    ): SingleErrorPayloadGenerator<T> =
        RequestDataErrorPayloadGeneratorBuilder.buildFrom(
            RequestDataErrorPayloadGeneratorBuilder.Dsl<T>().apply(initBlock)
        )
}

/**
 * Configuration DSL for [MultiErrorPayloadGenerator]s.
 */
sealed interface MultiErrorPayloadGeneratorDsl<T : Any, R : Any> {
    val subErrorExtractor: SubErrorExtractor<T, R>?
    val subErrorPayloadGenerator: SingleErrorPayloadGenerator<R>?

    /**
     * Opens configuration block of [SubErrorExtractor].
     */
    fun extractedAs(extractor: SubErrorExtractor<T, R>): MultiErrorPayloadGeneratorDsl<T, R>

    /**
     * Sets [generator] that will be used to process [extracted][MultiErrorPayloadGenerator.subErrorExtractor]
     * sub-errors.
     */
    fun eachHandledBy(generator: SingleErrorPayloadGenerator<R>): MultiErrorPayloadGeneratorDsl<T, R>

    /**
     * Opens configuration block of [SingleErrorPayloadGenerator].
     */
    fun eachRepresenting(factoryBlock: SingleErrorPayloadGeneratorFactoryBlock<R>) =
        eachHandledBy(factoryBlock(InstantiableSingleErrorPayloadGeneratorBuilders()))

    private class InstantiableSingleErrorPayloadGeneratorBuilders<T : Any> : SingleErrorPayloadGeneratorBuilders<T>
}

typealias SingleErrorPayloadGeneratorFactoryBlock<T> =
        FactoryBlock<SingleErrorPayloadGeneratorBuilders<T>, SingleErrorPayloadGenerator<T>>

object MultiErrorPayloadGeneratorBuilder {
    fun <T : Any, R : Any> buildFrom(config: MultiErrorPayloadGeneratorDsl<T, R>): MultiErrorPayloadGenerator<T, R> {
        val subErrorExtractor =
            config.subErrorExtractor
                ?: throw IllegalArgumentException("Sub-error extractor must be provided")
        val subErrorPayloadGenerator =
            config.subErrorPayloadGenerator
                ?: throw IllegalArgumentException("Sub-error payload generator must be provided")
        return MultiErrorPayloadGenerator(subErrorExtractor, subErrorPayloadGenerator)
    }

    /**
     * Default implementation of [MultiErrorPayloadGeneratorDsl].
     */
    class Dsl<T : Any, R : Any> : MultiErrorPayloadGeneratorDsl<T, R> {
        override var subErrorExtractor: SubErrorExtractor<T, R>? = null
            private set
        override var subErrorPayloadGenerator: SingleErrorPayloadGenerator<R>? = null
            private set

        override fun extractedAs(extractor: SubErrorExtractor<T, R>) = this.apply {
            subErrorExtractor = extractor
        }

        override fun eachHandledBy(generator: SingleErrorPayloadGenerator<R>) = this.apply {
            subErrorPayloadGenerator = generator
        }
    }
}

/**
 * Facade to factories of [ErrorPayloadGenerator]s.
 */
class ErrorPayloadGeneratorBuilders<T : Any> : SingleErrorPayloadGeneratorBuilders<T> {
    fun <R : Any> compositeOf(
        initBlock: InitBlock<MultiErrorPayloadGeneratorDsl<T, R>>
    ): MultiErrorPayloadGenerator<T, R> =
        MultiErrorPayloadGeneratorBuilder.buildFrom(
            MultiErrorPayloadGeneratorBuilder.Dsl<T, R>().apply(initBlock)
        )

    /**
     * Delegate of [compositeOf]. May be more readable in some situations.
     */
    @Suppress("UNUSED_PARAMETER")
    fun <R : Any> compositeOfErrorsOfType(
        faultObjectsType: KClass<T>,
        initBlock: InitBlock<MultiErrorPayloadGeneratorDsl<T, R>>
    ): MultiErrorPayloadGenerator<T, R> = compositeOf(initBlock)
}

/**
 * Configuration DSL for [SimpleResponseGenerator]s.
 */
sealed interface SimpleResponseGeneratorDsl<T : Any> {
    val responseStatusProviderFactoryBlock: ResponseStatusProviderFactoryBlock?

    /**
     * Opens configuration block of [ResponseStatusProvider].
     */
    fun withStatus(factoryBlock: ResponseStatusProviderFactoryBlock): SimpleResponseGeneratorDsl<T>

    /**
     * Overloaded version of [withStatus] taking [number][status] to create provider
     * returning it as a [status code][HttpStatus.code].
     */
    fun withStatus(status: Int) = withStatus { of(status) }
}

typealias ResponseStatusProviderFactoryBlock = FactoryBlock<ResponseStatusProvider.Factories, ResponseStatusProvider>

/**
 * Configuration DSL for [SimpleResponseGenerator]s, which produce responses for [OperationError].
 */
sealed interface OperationErrorResponseGeneratorDsl<T : Any> :
    SimpleResponseGeneratorDsl<T>,
    OperationErrorPayloadGeneratorDsl<T>

/**
 * Configuration DSL for [SimpleResponseGenerator]s, which produce responses for [RequestDataError].
 */
sealed interface RequestDataErrorResponseGeneratorDsl<T : Any> :
    SimpleResponseGeneratorDsl<T>,
    RequestDataErrorPayloadGeneratorDsl<T>

/**
 * Configuration DSL for [SimpleResponseGenerator]s, which produce responses with
 * [multi-errors payloads][MultiErrorPayloadGenerator].
 */
sealed interface MultiErrorResponseGeneratorDsl<T : Any, R : Any> :
    SimpleResponseGeneratorDsl<T>,
    MultiErrorPayloadGeneratorDsl<T, R>

object SimpleResponseGeneratorBuilder {
    fun <T : Any> buildFrom(config: OperationErrorResponseGeneratorDsl<T>): SimpleResponseGenerator<T> {
        val responseStatusProviderFactoryBlock = getStatusProviderFactory(config)
        return SimpleResponseGenerator(
            OperationErrorPayloadGeneratorBuilder.buildFrom(config),
            responseStatusProviderFactoryBlock(ResponseStatusProvider.Factories)
        )
    }

    private fun <T : Any> getStatusProviderFactory(config: SimpleResponseGeneratorDsl<T>) =
        config.responseStatusProviderFactoryBlock
            ?: throw IllegalArgumentException("Status provider factory block not set")

    fun <T : Any> buildFrom(config: RequestDataErrorResponseGeneratorDsl<T>): SimpleResponseGenerator<T> {
        val responseStatusProviderFactoryBlock = getStatusProviderFactory(config)
        return SimpleResponseGenerator(
            RequestDataErrorPayloadGeneratorBuilder.buildFrom(config),
            responseStatusProviderFactoryBlock(ResponseStatusProvider.Factories)
        )
    }

    fun <T : Any, R : Any> buildFrom(config: MultiErrorResponseGeneratorDsl<T, R>): SimpleResponseGenerator<T> {
        val responseStatusProviderFactoryBlock = getStatusProviderFactory(config)
        return SimpleResponseGenerator(
            MultiErrorPayloadGeneratorBuilder.buildFrom(config),
            responseStatusProviderFactoryBlock(ResponseStatusProvider.Factories)
        )
    }

    /**
     * Default implementation of [SimpleResponseGeneratorDsl].
     */
    class Dsl<T : Any> : SimpleResponseGeneratorDsl<T> {
        override var responseStatusProviderFactoryBlock: ResponseStatusProviderFactoryBlock? = null
            private set

        override fun withStatus(factoryBlock: ResponseStatusProviderFactoryBlock) = this.apply {
            responseStatusProviderFactoryBlock = factoryBlock
        }
    }

    interface OperationErrorResponseGenerator {
        companion object {
            fun <T : Any> dsl() = Dsl<T>()
        }

        /**
         * Default implementation of [OperationErrorResponseGeneratorDsl].
         *
         * Delegates to:
         *  - [SimpleResponseGeneratorDsl.Default]
         *  - [OperationErrorPayloadGeneratorBuilder.Dsl]
         */
        class Dsl<T : Any> :
            OperationErrorResponseGeneratorDsl<T>,
            SimpleResponseGeneratorDsl<T> by SimpleResponseGeneratorBuilder.Dsl(),
            OperationErrorPayloadGeneratorDsl<T> by OperationErrorPayloadGeneratorBuilder.Dsl()
    }

    interface RequestDataErrorResponseGenerator {
        companion object {
            fun <T : Any> dsl() = Dsl<T>()
        }

        /**
         * Default implementation of [RequestDataErrorResponseGeneratorDsl].
         *
         * Delegates to:
         *  - [SimpleResponseGeneratorDsl.Default]
         *  - [RequestDataErrorPayloadGeneratorBuilder.Dsl]
         */
        class Dsl<T : Any> :
            RequestDataErrorResponseGeneratorDsl<T>,
            SimpleResponseGeneratorDsl<T> by SimpleResponseGeneratorBuilder.Dsl(),
            RequestDataErrorPayloadGeneratorDsl<T> by RequestDataErrorPayloadGeneratorBuilder.Dsl()
    }

    interface MultiErrorResponseGenerator {
        companion object {
            fun <T : Any, R : Any> dsl() = Dsl<T, R>()
        }

        /**
         * Default implementation of [MultiErrorResponseGeneratorDsl].
         *
         * Delegates to:
         *  - [SimpleResponseGeneratorDsl.Default]
         *  - [MultiErrorPayloadGeneratorBuilder.Dsl]
         */
        class Dsl<T : Any, R : Any> :
            MultiErrorResponseGeneratorDsl<T, R>,
            SimpleResponseGeneratorDsl<T> by SimpleResponseGeneratorBuilder.Dsl(),
            MultiErrorPayloadGeneratorDsl<T, R> by MultiErrorPayloadGeneratorBuilder.Dsl()
    }
}

class SimpleResponseGeneratorBuilders<T : Any> {
    fun asOperationError(
        initBlock: InitBlock<OperationErrorResponseGeneratorDsl<T>>
    ): SimpleResponseGenerator<T> =
        SimpleResponseGeneratorBuilder.buildFrom(
            SimpleResponseGeneratorBuilder.OperationErrorResponseGenerator.dsl<T>().apply(initBlock)
        )

    fun asRequestDataError(
        initBlock: InitBlock<RequestDataErrorResponseGeneratorDsl<T>>
    ): SimpleResponseGenerator<T> =
        SimpleResponseGeneratorBuilder.buildFrom(
            SimpleResponseGeneratorBuilder.RequestDataErrorResponseGenerator.dsl<T>().apply(initBlock)
        )

    fun <R : Any> asCompositeOf(
        initBlock: InitBlock<MultiErrorResponseGeneratorDsl<T, R>>
    ): SimpleResponseGenerator<T> =
        SimpleResponseGeneratorBuilder.buildFrom(
            SimpleResponseGeneratorBuilder.MultiErrorResponseGenerator.dsl<T, R>().apply(initBlock)
        )

    /**
     * Delegate of [asCompositeOf]. May be more readable in some situations.
     */
    @Suppress("UNUSED_PARAMETER")
    fun <R : Any> asCompositeOfErrorsOfType(
        faultObjectsType: KClass<T>,
        initBlock: InitBlock<MultiErrorResponseGeneratorDsl<T, R>>
    ) = asCompositeOf(initBlock)
}

/**
 * Configuration DSL for [CompositeResponseGenerator]s.
 */
sealed interface TypeBasedResponseGeneratorRegistryDsl {
    val generatorsByFaultType: MutableMap<KClass<*>, ResponseGenerator<*>>

    /**
     * Registers passed [mapping][pair] between fault's type and [ResponseGenerator].
     */
    fun <T : Any> map(pair: Pair<KClass<T>, ResponseGenerator<T>>): TypeBasedResponseGeneratorRegistryDsl

    /**
     * Overloaded version of [map], that opens configuration block of mapping
     * between [fault's type][faultType] and [ResponseGenerator].
     */
    fun <T : Any> map(faultType: KClass<T>, generatorFactoryBlock: ResponseGeneratorFactoryBlock<T>) =
        map(faultType to generatorFactoryBlock(RestX))
}

typealias ResponseGeneratorFactoryBlock<T> = FactoryBlock<RestX, ResponseGenerator<T>>

/**
 * Overloaded, inline version of [map].
 */
inline fun <reified T : Any> TypeBasedResponseGeneratorRegistryDsl.register(
    generatorFactoryBlock: ResponseGeneratorFactoryBlock<T>
) = map(T::class to generatorFactoryBlock(RestX))

object TypeBasedResponseGeneratorRegistryBuilder {
    fun buildFrom(config: TypeBasedResponseGeneratorRegistryDsl): TypeBasedResponseGeneratorRegistry =
        TypeBasedResponseGeneratorRegistry(
            config.generatorsByFaultType.takeIf { it.isNotEmpty() }
                ?: throw IllegalArgumentException("Response generator registry cannot be empty")
        )

    /**
     * Default implementation of [TypeBasedResponseGeneratorRegistryDsl].
     */
    class Dsl : TypeBasedResponseGeneratorRegistryDsl {
        override var generatorsByFaultType: MutableMap<KClass<*>, ResponseGenerator<*>> = mutableMapOf()
            private set

        override fun <T : Any> map(pair: Pair<KClass<T>, ResponseGenerator<T>>) =
            this.apply { generatorsByFaultType.plusAssign(pair) }
    }
}

/**
 * Configuration DSL for [CompositeResponseGenerator]s.
 */
sealed interface CompositeResponseGeneratorDsl {
    val responseGeneratorRegistryFactoryBlock: ResponseGeneratorRegistryFactoryBlock?

    /**
     * Configures [TypeBasedResponseGeneratorRegistry] to be used as [generator's][CompositeResponseGenerator]
     * source of sub-generators.
     */
    fun registeredByFaultType(
        initBlock: InitBlock<TypeBasedResponseGeneratorRegistryDsl>
    ): CompositeResponseGeneratorDsl
}

typealias ResponseGeneratorRegistryFactoryBlock = FactoryBlock<Unit, ResponseGeneratorRegistry>

object CompositeResponseGeneratorBuilder {
    fun buildFrom(config: CompositeResponseGeneratorDsl): CompositeResponseGenerator {
        val responseGeneratorRegistryFactoryBlock =
            config.responseGeneratorRegistryFactoryBlock
                ?: throw IllegalArgumentException("Sub-generator registry factory block not set")
        return CompositeResponseGenerator(responseGeneratorRegistryFactoryBlock(Unit))
    }

    /**
     * Default implementation of [CompositeResponseGeneratorDsl].
     */
    class Dsl : CompositeResponseGeneratorDsl {
        override var responseGeneratorRegistryFactoryBlock: ResponseGeneratorRegistryFactoryBlock? = null
            private set

        override fun registeredByFaultType(
            initBlock: InitBlock<TypeBasedResponseGeneratorRegistryDsl>
        ) = this.apply {
            responseGeneratorRegistryFactoryBlock = {
                TypeBasedResponseGeneratorRegistryBuilder.Dsl()
                    .apply(initBlock)
                    .let { TypeBasedResponseGeneratorRegistryBuilder.buildFrom(it) }
            }
        }
    }
}
