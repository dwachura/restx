package io.dwsoft.restx.core.dsl

import io.dwsoft.restx.core.response.BasicResponseGenerator
import io.dwsoft.restx.core.response.CompositeResponseGenerator
import io.dwsoft.restx.core.response.TypeBasedResponseGeneratorRegistry
import io.dwsoft.restx.core.response.payload.CauseResolver
import io.dwsoft.restx.core.response.payload.CodeResolver
import io.dwsoft.restx.core.response.payload.MultiErrorPayloadGenerator
import io.dwsoft.restx.core.response.payload.OperationError
import io.dwsoft.restx.core.response.payload.OperationErrorPayloadGenerator
import io.dwsoft.restx.core.response.payload.RequestDataError
import io.dwsoft.restx.core.response.payload.RequestDataErrorPayloadGenerator

/**
 * Builder of [BasicResponseGenerator]s.
 */
object BasicResponseGeneratorBuilder {
    /**
     * Factory of generator of responses for [operation errors][OperationError].
     *
     * Uses default config of [OperationErrorPayloadGeneratorBuilder] to configure [payload generator]
     * [OperationErrorPayloadGenerator].
     */
    fun <T : Any> buildFrom(config: OperationErrorResponseGeneratorSpec<T>): BasicResponseGenerator<T> =
        BasicResponseGenerator(
            OperationErrorPayloadGeneratorBuilder.buildFrom(config),
            getStatusProviderFactory(config)(ResponseStatusProviders())
        )

    private fun <T : Any> getStatusProviderFactory(config: BasicResponseGeneratorSpec<T>) =
        config.responseStatusProviderFactoryBlock
            ?: throw IllegalArgumentException("Status provider not configured")

    /**
     * Factory of generator of responses for [request data errors][RequestDataError].
     *
     * Uses default config of [RequestDataErrorPayloadGeneratorBuilder] to configure [payload generator]
     * [RequestDataErrorPayloadGenerator].
     */
    fun <T : Any> buildFrom(config: RequestDataErrorResponseGeneratorSpec<T>): BasicResponseGenerator<T> =
        BasicResponseGenerator(
            RequestDataErrorPayloadGeneratorBuilder.buildFrom(config),
            getStatusProviderFactory(config)(ResponseStatusProviders())
        )

    /**
     * Factory of [generators of multi-error responses][MultiErrorPayloadGenerator].
     */
    fun <T : Any, R : Any> buildFrom(config: MultiErrorResponseGeneratorSpec<T, R>): BasicResponseGenerator<T> =
        BasicResponseGenerator(
            MultiErrorPayloadGeneratorBuilder.buildFrom(config),
            getStatusProviderFactory(config)(ResponseStatusProviders())
        )
}

/**
 * Builder of [OperationErrorPayloadGenerator].
 *
 * If not explicitly [configured][identifiedByKey], [cause resolver][CauseResolver] identifying faults by
 * [their runtime type][CauseResolvers.type] is used.
 *
 * If not explicitly [configured][withCode], [code resolver][CodeResolver] using [cause identifier as a code]
 * [CodeResolvers.sameAsCauseKey] is used.
 */
object OperationErrorPayloadGeneratorBuilder {
    fun <T : Any> buildFrom(config: OperationErrorPayloadGeneratorSpec<T>): OperationErrorPayloadGenerator<T> =
        OperationErrorPayloadGenerator(
            CauseResolvers<T>().let { config.causeResolverFactoryBlock?.invoke(it) ?: it.type() },
            CodeResolvers<T>().let { config.codeResolverFactoryBlock?.invoke(it) ?: it.sameAsCauseKey() },
            config.messageResolverFactoryBlock?.invoke(MessageResolvers())
                ?: throw IllegalArgumentException("Message resolver not configured")
        )
}

/**
 * Builder of [RequestDataErrorPayloadGenerator].
 *
 * If not explicitly [configured][identifiedByKey], [cause resolver][CauseResolver] identifying faults by
 * [their runtime type][CauseResolvers.type] is used.
 *
 * If not explicitly [configured][withCode], [code resolver][CodeResolver] using [cause identifier as a code]
 * [CodeResolvers.sameAsCauseKey] is used.
 */
object RequestDataErrorPayloadGeneratorBuilder {
    fun <T : Any> buildFrom(config: RequestDataErrorPayloadGeneratorSpec<T>): RequestDataErrorPayloadGenerator<T> =
        RequestDataErrorPayloadGenerator(
            CauseResolvers<T>().let { config.causeResolverFactoryBlock?.invoke(it) ?: it.type() },
            CodeResolvers<T>().let { config.codeResolverFactoryBlock?.invoke(it) ?: it.sameAsCauseKey() },
            config.messageResolverFactoryBlock?.invoke(MessageResolvers())
                ?: throw IllegalArgumentException("Message resolver not configured"),
            config.dataErrorSourceResolverFactoryBlock?.invoke(DataErrorSourceResolvers())
                ?: throw IllegalArgumentException("Request error source resolver not configured")
        )
}

/**
 * Builder of [MultiErrorPayloadGenerator].
 */
object MultiErrorPayloadGeneratorBuilder {
    fun <T : Any, R : Any> buildFrom(config: MultiErrorPayloadGeneratorSpec<T, R>): MultiErrorPayloadGenerator<T, R> =
        MultiErrorPayloadGenerator(
            config.subErrorExtractor
                ?: throw IllegalArgumentException("Sub-error extractor not configured"),
            config.subErrorPayloadGeneratorFactoryBlock?.invoke(SingleErrorPayloadGenerators())
                ?: throw IllegalArgumentException("Sub-error payload generator not configured")
        )
}

/**
 * Builder of [CompositeResponseGenerator].
 */
object CompositeResponseGeneratorBuilder {
    fun buildFrom(config: CompositeResponseGeneratorSpec): CompositeResponseGenerator =
        CompositeResponseGenerator(
            config.responseGeneratorRegistry
                ?: throw IllegalArgumentException("Response generator registry not configured")
        )
}

/**
 * Builder of [TypeBasedResponseGeneratorRegistry].
 */
object TypeBasedResponseGeneratorRegistryBuilder {
    fun buildFrom(config: TypeBasedResponseGeneratorRegistrySpec): TypeBasedResponseGeneratorRegistry =
        TypeBasedResponseGeneratorRegistry(
            config.generatorsByFaultType.takeIf { it.isNotEmpty() }
                ?: throw IllegalArgumentException("Response generator registry cannot be empty")
        )
}
