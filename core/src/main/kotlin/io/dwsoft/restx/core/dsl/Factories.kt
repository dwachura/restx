package io.dwsoft.restx.core.dsl

import io.dwsoft.restx.RestXException
import io.dwsoft.restx.core.Logging.initLog
import io.dwsoft.restx.core.response.BasicResponseGenerator
import io.dwsoft.restx.core.response.CompositeResponseGenerator
import io.dwsoft.restx.core.response.HttpStatus
import io.dwsoft.restx.core.response.ResponseGenerator
import io.dwsoft.restx.core.response.ResponseStatusProvider
import io.dwsoft.restx.core.response.payload.Cause
import io.dwsoft.restx.core.response.payload.CauseResolver
import io.dwsoft.restx.core.response.payload.CodeResolver
import io.dwsoft.restx.core.response.payload.DataErrorSourceResolver
import io.dwsoft.restx.core.response.payload.FixedCodeResolver
import io.dwsoft.restx.core.response.payload.MapBasedCodeResolver
import io.dwsoft.restx.core.response.payload.Message
import io.dwsoft.restx.core.response.payload.MessageResolver
import io.dwsoft.restx.core.response.payload.MessageTextProvider
import io.dwsoft.restx.core.response.payload.PlainTextMessageResolver
import io.dwsoft.restx.core.response.payload.SingleErrorPayloadGenerator
import kotlin.reflect.KClass
import kotlin.reflect.full.superclasses

class RestXConfigurationException(cause: Throwable) : RestXException(cause)

/**
 * Factories of [ResponseGenerator]s.
 */
class ResponseGenerators {
    /**
     * Entry method to fluently configure [BasicResponseGenerator]s.
     *
     * @throws RestXConfigurationException in case of any errors during creation of a generator
     */
    fun <T : Any> treat(factoryBlock: BasicResponseGenerators<T>.() -> BasicResponseGenerator<T>): BasicResponseGenerator<T> =
        buildGenerator { factoryBlock.invoke(BasicResponseGenerators()) }

    private fun <R : ResponseGenerator<T>, T : Any> buildGenerator(buildFunction: () -> R) =
        runCatching(buildFunction).fold(
            onSuccess = { it },
            onFailure = { throw RestXConfigurationException(it) }
        )

    /**
     * Delegate of [treat].
     */
    fun <T : Any> generatorFor(factoryBlock: BasicResponseGenerators<T>.() -> BasicResponseGenerator<T>) = treat(factoryBlock)

    /**
     * Entry method to fluently configure [CompositeResponseGenerator]s.
     *
     * @throws RestXConfigurationException in case of any errors during creation of a generator
     */
    fun compose(initBlock: CompositeResponseGeneratorSpec.() -> Unit): CompositeResponseGenerator = buildGenerator {
        CompositeResponseGeneratorBuilder.buildFrom(CompositeResponseGeneratorSpecDelegate().apply(initBlock))
    }
}

/**
 * Factories of [BasicResponseGenerator]s.
 */
class BasicResponseGenerators<T : Any> {
    fun asOperationError(initBlock: OperationErrorResponseGeneratorSpec<T>.() -> Unit) =
        BasicResponseGeneratorBuilder.buildFrom(OperationErrorResponseGeneratorSpecDelegate<T>().apply(initBlock))

    fun asRequestDataError(initBlock: RequestDataErrorResponseGeneratorSpec<T>.() -> Unit) =
        BasicResponseGeneratorBuilder.buildFrom(RequestDataErrorResponseGeneratorSpecDelegate<T>().apply(initBlock))

    fun <R : Any> asContainerOf(initBlock: MultiErrorResponseGeneratorSpec<T, R>.() -> Unit) =
        BasicResponseGeneratorBuilder.buildFrom(MultiErrorResponseGeneratorSpecDelegate<T, R>().apply(initBlock))
}

/**
 * Factories of [SingleErrorPayloadGenerator]s.
 */
class SingleErrorPayloadGenerators<T : Any> {
    fun operationError(initBlock: OperationErrorPayloadGeneratorSpec<T>.() -> Unit) =
        OperationErrorPayloadGeneratorBuilder.buildFrom(
            OperationErrorPayloadGeneratorSpecDelegate<T>().apply(initBlock)
        )

    fun requestDataError(initBlock: RequestDataErrorPayloadGeneratorSpec<T>.() -> Unit) =
        RequestDataErrorPayloadGeneratorBuilder.buildFrom(
            RequestDataErrorPayloadGeneratorSpecDelegate<T>().apply(initBlock)
        )
}

/**
 * Factories of [ResponseStatusProvider]s.
 */
class ResponseStatusProviders {
    fun of(status: Int) = ResponseStatusProvider { HttpStatus(status) }
    fun of(status: () -> Int) = ResponseStatusProvider { HttpStatus(status()) }
    fun providedBy(statusProvider: () -> HttpStatus) = ResponseStatusProvider { statusProvider() }
}

/**
 * Factories of [CauseResolver]s.
 */
class CauseResolvers<T : Any> {
    /**
     * Factory method for [CauseResolver]s that provide causes identified by values provided by given supplier
     * (possible retrieved from fault result instance).
     */
    fun function(supplier: (T) -> String): CauseResolver<T> =
        CauseResolver { faultResult -> Cause(supplier(faultResult), faultResult) }

    /**
     * Factory method for [CauseResolver]s that provide causes identified by given constant value.
     */
    fun fixedKey(key: String): CauseResolver<T> = function { key }

    /**
     * Factory method for [CauseResolver]s that provide causes identified by fault's [runtime type name]
     * [KClass.qualifiedName].
     *
     * In case of type cannot be resolved (e.g. anonymous or local class instance passed) fault's type hierarchy
     * is looked-up.
     */
    fun type(): CauseResolver<T> =
        object : CauseResolver<T> {
            private val log = CauseResolver::class.initLog()

            override fun causeOf(fault: T): Cause<T> {
                val typeName = listOf(fault::class).findTypeName()
                    .also { log.debug { "Type name found: $it" } }
                return Cause(typeName, fault)
            }

            private fun List<KClass<*>>.findTypeName(): String =
                also { log.debug { "Searching type name for classes: $it" } }
                    .firstNotNullOfOrNull { it.qualifiedName }
                    ?: also { log.debug { "Resolvable type name not found - parent types will be searched" } }
                        .flatMap { it.superclasses }.findTypeName()
        }
}

/**
 * Factories of [CodeResolver]s.
 * Additional factory methods should be added as an extension functions.
 */
class CodeResolvers<T : Any> {
    /**
     * Factory method that simply returns passed [resolver].
     */
    fun resolvedBy(resolver: CodeResolver<T>): CodeResolver<T> = resolver

    /**
     * Factory method for [resolvers][CodeResolver] that returns code same as passed [cause key][Cause.key].
     */
    fun sameAsCauseKey(): CodeResolver<T> = resolvedBy { it.key }

    /**
     * Factory method for [FixedCodeResolver]
     */
    fun fixed(code: String): CodeResolver<T> = FixedCodeResolver(code)

    /**
     * Factory method for [MapBasedCodeResolver].
     */
    fun mapBased(mapping: Map<String, String>): CodeResolver<T> = MapBasedCodeResolver(mapping)
}

/**
 * Delegate of [CodeResolvers.resolvedBy] returning [resolver][CodeResolver] based on passed [function][resolver].
 */
fun <T : Any> CodeResolvers<T>.generatedAs(resolver: Cause<T>.() -> String): CodeResolver<T> =
    resolvedBy(object : CodeResolver<T> {
        private val log = CodeResolver::class.initLog()

        override fun codeFor(cause: Cause<T>): String =
            cause.resolver().also { log.info { "Returning code [$it] from custom resolver for $cause" } }
    })

/**
 * Overloaded version of [CodeResolvers.mapBased].
 */
fun <T : Any> CodeResolvers<T>.mapBased(vararg mapEntries: Pair<String, String>): CodeResolver<T> =
    mapBased(mapping = mapOf(pairs = mapEntries))

/**
 * Factories of [MessageResolver]s.
 * Additional factory methods should be added as an extension functions.
 */
class MessageResolvers<T : Any> {
    /**
     * Factory method that simply returns passed [resolver].
     */
    fun resolvedBy(resolver: MessageResolver<T>): MessageResolver<T> = resolver

    /**
     * Delegate of [resolvedBy] returning [PlainTextMessageResolver].
     */
    fun fromTextGeneratedBy(messageTextProvider: MessageTextProvider<T>): MessageResolver<T> =
        resolvedBy(PlainTextMessageResolver(messageTextProvider))

    /**
     * Factory method that creates [resolver][MessageResolver] returning [Message]s with given [text][message]
     * as their content.
     */
    fun fromText(message: String): MessageResolver<T> = fromTextGeneratedBy { message }
}

/**
 * Delegate of [MessageResolvers.resolvedBy] returning [resolver][MessageResolver] based on passed [function][resolver].
 */
fun <T : Any> MessageResolvers<T>.generatedAs(resolver: Cause<T>.() -> Message) =
    resolvedBy(object : MessageResolver<T> {
        private val log = MessageResolver::class.initLog()

        override fun messageFor(cause: Cause<T>): Message =
            cause.resolver().also {
                log.info { "Returning message from custom resolver for $cause" }
                log.debug { "Message: $it" }
            }
    })

/**
 * Delegate of [MessageResolvers.fromTextGeneratedBy] utilizing function with receiver for more concise syntax.
 */
fun <T : Any> MessageResolvers<T>.fromTextGeneratedAs(messageTextProvider: Cause<T>.() -> String) =
    fromTextGeneratedBy(MessageTextProvider(messageTextProvider))

/**
 * Factories of [DataErrorSourceResolver]s.
 * Additional factory methods should be added as an extension functions.
 */
class DataErrorSourceResolvers<T : Any> {
    fun resolvedBy(resolver: DataErrorSourceResolver<T>) = resolver
}
