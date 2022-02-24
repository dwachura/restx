package io.dwsoft.restx.core.dsl

import io.dwsoft.restx.core.payload.MultiErrorPayloadGenerator
import io.dwsoft.restx.core.payload.OperationErrorPayloadGenerator
import io.dwsoft.restx.core.payload.RequestDataErrorPayloadGenerator
import io.dwsoft.restx.core.response.BasicResponseGenerator
import io.dwsoft.restx.core.response.CompositeResponseGenerator
import io.dwsoft.restx.core.response.TypeBasedResponseGeneratorRegistry

// TODO: define defaults here in each builder as a inner class of sth

object BasicResponseGeneratorBuilder {
    fun <T : Any> buildFrom(config: OperationErrorResponseGeneratorSpec<T>): BasicResponseGenerator<T> =
        BasicResponseGenerator(
            OperationErrorPayloadGeneratorBuilder.buildFrom(config),
            getStatusProviderFactory(config)(ResponseStatusProviders())
        )

    private fun <T : Any> getStatusProviderFactory(config: BasicResponseGeneratorSpec<T>) =
        config.responseStatusProviderFactoryBlock
            ?: throw IllegalArgumentException("Status provider not configured")

    fun <T : Any> buildFrom(config: RequestDataErrorResponseGeneratorSpec<T>): BasicResponseGenerator<T> =
        BasicResponseGenerator(
            RequestDataErrorPayloadGeneratorBuilder.buildFrom(config),
            getStatusProviderFactory(config)(ResponseStatusProviders())
        )

    fun <T : Any, R : Any> buildFrom(config: MultiErrorResponseGeneratorSpec<T, R>): BasicResponseGenerator<T> =
        BasicResponseGenerator(
            MultiErrorPayloadGeneratorBuilder.buildFrom(config),
            getStatusProviderFactory(config)(ResponseStatusProviders())
        )
}

object OperationErrorPayloadGeneratorBuilder {
    fun <T : Any> buildFrom(config: OperationErrorPayloadGeneratorSpec<T>): OperationErrorPayloadGenerator<T> =
        OperationErrorPayloadGenerator(
            config.causeResolverFactoryBlock?.invoke(CauseResolvers()) ?: CauseResolvers<T>().type(),
            config.codeResolverFactoryBlock?.invoke(CodeResolvers())
                ?: throw IllegalArgumentException("Code resolver not configured"),
            config.messageResolverFactoryBlock?.invoke(MessageResolvers())
                ?: throw IllegalArgumentException("Message resolver not configured")
        )
}

object RequestDataErrorPayloadGeneratorBuilder {
    fun <T : Any> buildFrom(config: RequestDataErrorPayloadGeneratorSpec<T>): RequestDataErrorPayloadGenerator<T> =
        RequestDataErrorPayloadGenerator(
            config.causeResolverFactoryBlock?.invoke(CauseResolvers())
                ?: throw IllegalArgumentException("Cause resolver not configured"),
            config.codeResolverFactoryBlock?.invoke(CodeResolvers())
                ?: throw IllegalArgumentException("Code resolver not configured"),
            config.messageResolverFactoryBlock?.invoke(MessageResolvers())
                ?: throw IllegalArgumentException("Message resolver not configured"),
            config.dataErrorSourceResolverFactoryBlock?.invoke(DataErrorSourceResolvers())
                ?: throw IllegalArgumentException("Data error source resolver not configured")
        )
}

object MultiErrorPayloadGeneratorBuilder {
    fun <T : Any, R : Any> buildFrom(config: MultiErrorPayloadGeneratorSpec<T, R>): MultiErrorPayloadGenerator<T, R> =
        MultiErrorPayloadGenerator(
            config.subErrorExtractor
                ?: throw IllegalArgumentException("Sub-error extractor not configured"),
            config.subErrorPayloadGeneratorFactoryBlock?.invoke(SingleErrorPayloadGenerators())
                ?: throw IllegalArgumentException("Sub-error payload generator not configured")
        )
}

object CompositeResponseGeneratorBuilder {
    fun buildFrom(config: CompositeResponseGeneratorSpec): CompositeResponseGenerator =
        CompositeResponseGenerator(
            config.responseGeneratorRegistry
                ?: throw IllegalArgumentException("Sub-generator registry not configured")
        )
}

object TypeBasedResponseGeneratorRegistryBuilder {
    fun buildFrom(config: TypeBasedResponseGeneratorRegistrySpec): TypeBasedResponseGeneratorRegistry =
        TypeBasedResponseGeneratorRegistry(
            config.generatorsByFaultType.takeIf { it.isNotEmpty() }
                ?: throw IllegalArgumentException("Response generator registry cannot be empty")
        )
}
