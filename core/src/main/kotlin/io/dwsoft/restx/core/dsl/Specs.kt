package io.dwsoft.restx.core.dsl

import io.dwsoft.restx.core.response.BasicResponseGenerator
import io.dwsoft.restx.core.response.CompositeResponseGenerator
import io.dwsoft.restx.core.response.HttpStatus
import io.dwsoft.restx.core.response.ResponseGenerator
import io.dwsoft.restx.core.response.ResponseGeneratorRegistry
import io.dwsoft.restx.core.response.ResponseStatusProvider
import io.dwsoft.restx.core.response.TypeBasedResponseGeneratorRegistry
import io.dwsoft.restx.core.response.payload.CauseResolver
import io.dwsoft.restx.core.response.payload.CodeResolver
import io.dwsoft.restx.core.response.payload.DataErrorSourceResolver
import io.dwsoft.restx.core.response.payload.MessageResolver
import io.dwsoft.restx.core.response.payload.MultiErrorPayloadGenerator
import io.dwsoft.restx.core.response.payload.OperationError
import io.dwsoft.restx.core.response.payload.RequestDataError
import io.dwsoft.restx.core.response.payload.SingleErrorPayloadGenerator
import io.dwsoft.restx.core.response.payload.SubErrorExtractor
import kotlin.reflect.KClass

/**
 * Configuration DSL for [BasicResponseGenerator]s.
 */
sealed interface BasicResponseGeneratorSpec<T : Any> {
    val responseStatusProviderFactoryBlock: (ResponseStatusProviders.() -> ResponseStatusProvider)?

    /**
     * Opens configuration block of [ResponseStatusProvider].
     */
    fun withStatus(factoryBlock: ResponseStatusProviders.() -> ResponseStatusProvider)
}

/**
 * Overloaded version of [withStatus] taking [number][status] to create provider
 * returning it as a [status code][HttpStatus.code].
 */
fun <T : Any> BasicResponseGeneratorSpec<T>.withStatus(status: Int) = withStatus { of(status) }

/**
 * Configuration DSL serving as a point of DSL's extension, common for [BasicResponseGenerator]s, which produce
 * responses containing single error payloads.
 */
sealed interface SingleErrorResponseGeneratorSpec<T : Any> :
    BasicResponseGeneratorSpec<T>,
    SingleErrorPayloadGeneratorSpec<T>

/**
 * Configuration DSL for [BasicResponseGenerator]s, which produce responses for [OperationError].
 */
sealed interface OperationErrorResponseGeneratorSpec<T : Any> :
    SingleErrorResponseGeneratorSpec<T>,
    OperationErrorPayloadGeneratorSpec<T>

internal class OperationErrorResponseGeneratorSpecDelegate<T : Any> :
    OperationErrorResponseGeneratorSpec<T>,
    OperationErrorPayloadGeneratorSpec<T> by OperationErrorPayloadGeneratorSpecDelegate() {

    override var responseStatusProviderFactoryBlock: (ResponseStatusProviders.() -> ResponseStatusProvider)? = null
        private set

    override fun withStatus(factoryBlock: ResponseStatusProviders.() -> ResponseStatusProvider) {
        responseStatusProviderFactoryBlock = factoryBlock
    }
}

/**
 * Configuration DSL for [BasicResponseGenerator]s, which produce responses for [RequestDataError].
 */
sealed interface RequestDataErrorResponseGeneratorSpec<T : Any> :
    SingleErrorResponseGeneratorSpec<T>,
    RequestDataErrorPayloadGeneratorSpec<T>

internal class RequestDataErrorResponseGeneratorSpecDelegate<T : Any> :
    RequestDataErrorResponseGeneratorSpec<T>,
    RequestDataErrorPayloadGeneratorSpec<T> by RequestDataErrorPayloadGeneratorSpecDelegate() {

    override var responseStatusProviderFactoryBlock: (ResponseStatusProviders.() -> ResponseStatusProvider)? = null
        private set

    override fun withStatus(factoryBlock: ResponseStatusProviders.() -> ResponseStatusProvider) {
        responseStatusProviderFactoryBlock = factoryBlock
    }
}

/**
 * Configuration DSL for [SingleErrorPayloadGenerator]s.
 */
sealed interface SingleErrorPayloadGeneratorSpec<T : Any> {
    val causeResolverFactoryBlock: (CauseResolvers<T>.() -> CauseResolver<T>)?
    val codeResolverFactoryBlock: (CodeResolvers<T>.() -> CodeResolver<T>)?
    val messageResolverFactoryBlock: (MessageResolvers<T>.() -> MessageResolver<T>)?

    /**
     * Opens configuration block of [CauseResolver].
     */
    fun identifiedBy(factoryBlock: CauseResolvers<T>.() -> CauseResolver<T>)

    /**
     * Opens configuration block of [CodeResolver].
     */
    fun withCode(factoryBlock: CodeResolvers<T>.() -> CodeResolver<T>)

    /**
     * Opens configuration block of [MessageResolver].
     */
    fun withMessage(factoryBlock: MessageResolvers<T>.() -> MessageResolver<T>)
}

/**
 * Delegate of [SingleErrorPayloadGeneratorSpec.identifiedBy] returning [fixed resolver][CauseResolvers.fixedKey].
 */
fun <T : Any> SingleErrorPayloadGeneratorSpec<T>.identifiedByKey(key: String) = identifiedBy { fixedKey(key) }

/**
 * Overloaded version of [SingleErrorPayloadGeneratorSpec.withCode] returning [fixed resolver][CodeResolvers.fixed].
 */
fun <T : Any> SingleErrorPayloadGeneratorSpec<T>.withCode(code: String) = withCode { fixed(code) }

/**
 * Overloaded version of [SingleErrorPayloadGeneratorSpec.withMessage] returning [plain text resolver]
 * [MessageResolvers.fromText].
 */
fun <T : Any> SingleErrorPayloadGeneratorSpec<T>.withMessage(message: String) = withMessage { fromText(message) }

/**
 * Configuration DSL for [SingleErrorPayloadGenerator]s, which generate payloads for [OperationError]s.
 */
sealed interface OperationErrorPayloadGeneratorSpec<T : Any> : SingleErrorPayloadGeneratorSpec<T>

internal class OperationErrorPayloadGeneratorSpecDelegate<T : Any> : OperationErrorPayloadGeneratorSpec<T> {
    override var causeResolverFactoryBlock: (CauseResolvers<T>.() -> CauseResolver<T>)? = null
        private set
    override var codeResolverFactoryBlock: (CodeResolvers<T>.() -> CodeResolver<T>)? = null
        private set
    override var messageResolverFactoryBlock: (MessageResolvers<T>.() -> MessageResolver<T>)? = null
        private set

    override fun identifiedBy(factoryBlock: CauseResolvers<T>.() -> CauseResolver<T>) {
        causeResolverFactoryBlock = factoryBlock
    }

    override fun withCode(factoryBlock: CodeResolvers<T>.() -> CodeResolver<T>) {
        codeResolverFactoryBlock = factoryBlock
    }

    override fun withMessage(factoryBlock: MessageResolvers<T>.() -> MessageResolver<T>) {
        messageResolverFactoryBlock = factoryBlock
    }
}

/**
 * Configuration DSL for [SingleErrorPayloadGenerator]s, which generate payloads for [RequestDataError]s.
 */
sealed interface RequestDataErrorPayloadGeneratorSpec<T : Any> : SingleErrorPayloadGeneratorSpec<T> {
    val dataErrorSourceResolverFactoryBlock: (DataErrorSourceResolvers<T>.() -> DataErrorSourceResolver<T>)?

    /**
     * Opens configuration block of [DataErrorSourceResolver].
     */
    fun causedByInvalidInput(factoryBlock: DataErrorSourceResolvers<T>.() -> DataErrorSourceResolver<T>)
}

internal class RequestDataErrorPayloadGeneratorSpecDelegate<T : Any> : RequestDataErrorPayloadGeneratorSpec<T> {
    override var causeResolverFactoryBlock: (CauseResolvers<T>.() -> CauseResolver<T>)? = null
        private set
    override var codeResolverFactoryBlock: (CodeResolvers<T>.() -> CodeResolver<T>)? = null
        private set
    override var messageResolverFactoryBlock: (MessageResolvers<T>.() -> MessageResolver<T>)? = null
        private set
    override var dataErrorSourceResolverFactoryBlock:
        (DataErrorSourceResolvers<T>.() -> DataErrorSourceResolver<T>)? = null
            private set

    override fun identifiedBy(factoryBlock: CauseResolvers<T>.() -> CauseResolver<T>) {
        causeResolverFactoryBlock = factoryBlock
    }

    override fun withCode(factoryBlock: CodeResolvers<T>.() -> CodeResolver<T>) {
        codeResolverFactoryBlock = factoryBlock
    }

    override fun withMessage(factoryBlock: MessageResolvers<T>.() -> MessageResolver<T>) {
        messageResolverFactoryBlock = factoryBlock
    }

    override fun causedByInvalidInput(factoryBlock: DataErrorSourceResolvers<T>.() -> DataErrorSourceResolver<T>) {
        dataErrorSourceResolverFactoryBlock = factoryBlock
    }
}

/**
 * Configuration DSL for [BasicResponseGenerator]s, which produce responses with
 * [multi-errors payloads][MultiErrorPayloadGenerator].
 */
sealed interface MultiErrorResponseGeneratorSpec<T : Any, R : Any> :
    BasicResponseGeneratorSpec<T>,
    MultiErrorPayloadGeneratorSpec<T, R>

internal class MultiErrorResponseGeneratorSpecDelegate<T : Any, R : Any> :
    MultiErrorResponseGeneratorSpec<T, R>,
    MultiErrorPayloadGeneratorSpec<T, R> by MultiErrorPayloadGeneratorSpecDelegate() {

    override var responseStatusProviderFactoryBlock: (ResponseStatusProviders.() -> ResponseStatusProvider)? = null
        private set

    override fun withStatus(factoryBlock: ResponseStatusProviders.() -> ResponseStatusProvider) {
        responseStatusProviderFactoryBlock = factoryBlock
    }
}

/**
 * Configuration DSL for [MultiErrorPayloadGenerator]s.
 */
sealed interface MultiErrorPayloadGeneratorSpec<T : Any, R : Any> {
    val subErrorExtractor: SubErrorExtractor<T, R>?
    val subErrorPayloadGeneratorFactoryBlock: (SingleErrorPayloadGenerators<R>.() -> SingleErrorPayloadGenerator<R>)?

    /**
     * Opens configuration block of [SubErrorExtractor].
     */
    fun extractedAs(extractor: SubErrorExtractor<T, R>)

    /**
     * Opens configuration block of [SingleErrorPayloadGenerator].
     */
    fun eachRepresenting(factoryBlock: SingleErrorPayloadGenerators<R>.() -> SingleErrorPayloadGenerator<R>)
}

/**
 * Sets [generator] that will be used to process [extracted][MultiErrorPayloadGenerator.subErrorExtractor]
 * sub-errors.
 */
fun <T : Any, R : Any> MultiErrorPayloadGeneratorSpec<T, R>.eachHandledBy(generator: SingleErrorPayloadGenerator<R>) =
    eachRepresenting { generator }

internal class MultiErrorPayloadGeneratorSpecDelegate<T : Any, R : Any> : MultiErrorPayloadGeneratorSpec<T, R> {
    override var subErrorExtractor: SubErrorExtractor<T, R>? = null
        private set
    override var subErrorPayloadGeneratorFactoryBlock:
        (SingleErrorPayloadGenerators<R>.() -> SingleErrorPayloadGenerator<R>)? = null
            private set

    override fun extractedAs(extractor: SubErrorExtractor<T, R>) { subErrorExtractor = extractor }

    override fun eachRepresenting(factoryBlock: SingleErrorPayloadGenerators<R>.() -> SingleErrorPayloadGenerator<R>) {
        subErrorPayloadGeneratorFactoryBlock = factoryBlock
    }
}

/**
 * Configuration DSL for [CompositeResponseGenerator]s.
 */
sealed interface CompositeResponseGeneratorSpec {
    val responseGeneratorRegistry: ResponseGeneratorRegistry?

    /**
     * Configures [TypeBasedResponseGeneratorRegistry] to be used as [generator's][CompositeResponseGenerator]
     * source of sub-generators.
     */
    fun registeredByFaultType(initBlock: TypeBasedResponseGeneratorRegistrySpec.() -> Unit)
}

internal class CompositeResponseGeneratorSpecDelegate : CompositeResponseGeneratorSpec {
    override var responseGeneratorRegistry: ResponseGeneratorRegistry? = null
        private set

    override fun registeredByFaultType(initBlock: TypeBasedResponseGeneratorRegistrySpec.() -> Unit) {
        responseGeneratorRegistry = TypeBasedResponseGeneratorRegistryBuilder.buildFrom(
            TypeBasedResponseGeneratorRegistrySpecDelegate().apply(initBlock)
        )
    }
}

/**
 * Configuration DSL for [CompositeResponseGenerator]s.
 */
sealed interface TypeBasedResponseGeneratorRegistrySpec {
    val generatorsByFaultType: MutableMap<KClass<*>, ResponseGenerator<*>>

    /**
     * Registers passed [mapping][pair] between fault's type and [ResponseGenerator].
     */
    fun <T : Any> map(pair: Pair<KClass<T>, ResponseGenerator<T>>)
}

internal class TypeBasedResponseGeneratorRegistrySpecDelegate : TypeBasedResponseGeneratorRegistrySpec {
    override var generatorsByFaultType: MutableMap<KClass<*>, ResponseGenerator<*>> = mutableMapOf()
        private set

    override fun <T : Any> map(pair: Pair<KClass<T>, ResponseGenerator<T>>) {
        generatorsByFaultType += pair
    }
}

/**
 * Overloaded, inline version of [TypeBasedResponseGeneratorRegistrySpec.map].
 */
inline fun <reified T : Any> TypeBasedResponseGeneratorRegistrySpec.register(
    generatorFactoryBlock: ResponseGenerators.() -> ResponseGenerator<T>
) = map(T::class to generatorFactoryBlock(ResponseGenerators()))
