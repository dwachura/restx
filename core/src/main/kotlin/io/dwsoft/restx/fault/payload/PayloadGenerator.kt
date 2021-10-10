package io.dwsoft.restx.fault.payload

import io.dwsoft.restx.FactoryBlock
import io.dwsoft.restx.InitBlock
import io.dwsoft.restx.RestXException
import io.dwsoft.restx.fault.cause.CauseProcessor
import io.dwsoft.restx.fault.cause.CauseProcessors
import io.dwsoft.restx.fault.cause.CauseResolver
import io.dwsoft.restx.fault.cause.CauseResolvers
import io.dwsoft.restx.Logging.initLog

/**
 * Base interface for generators of error response payloads.
 *
 * @param T type of fault objects that generators of this class supports
 * @param R specific type of [ErrorResponsePayload] generated by objects of this class
 */
sealed interface ErrorPayloadGenerator<in T : Any, out R : ErrorResponsePayload> {
    /**
     * Method that generates payload for given fault result
     *
     * @throws RestXException in case of errors during generation process
     */
    fun payloadOf(fault: T): R

    /**
     * Facade to payload generators builders
     */
    class Builders<T : Any> {
        fun error(
            initBlock: InitBlock<SingleErrorPayloadGenerator.Builder.Config<T>>
        ): SingleErrorPayloadGenerator<T> =
            SingleErrorPayloadGenerator.buildFrom(
                SingleErrorPayloadGenerator.Builder.Config<T>().apply(initBlock)
            )

        fun <R : Any> subErrors(
            initBlock: InitBlock<MultiErrorPayloadGenerator.Builder.Config<T, R>>
        ): MultiErrorPayloadGenerator<T, R> =
            MultiErrorPayloadGenerator.buildFrom(
                MultiErrorPayloadGenerator.Builder.Config<T, R>().apply(initBlock)
            )

        fun basic(
            generatorFactory: PayloadGenerators.() -> ErrorPayloadGenerator<T, *>
        ): ErrorPayloadGenerator<T, *> = generatorFactory(PayloadGenerators)
    }
}

/**
 * Generator creating payloads for fault results caused by single errors.
 */
class SingleErrorPayloadGenerator<T : Any>(
    private val causeResolver: CauseResolver<T>,
    private val processor: CauseProcessor<T>
) : ErrorPayloadGenerator<T, SingleErrorPayload> {
    override fun payloadOf(fault: T): SingleErrorPayload {
        return processor.process(causeResolver.causeOf(fault))
    }

    companion object Builder {
        fun <T : Any> buildFrom(config: Config<T>): SingleErrorPayloadGenerator<T> {
            val causeResolverFactoryBlock =
                config.causeResolverFactoryBlock
                    ?: throw IllegalArgumentException("Cause resolver factory block not set")
            val causeProcessorFactoryBlock =
                config.causeProcessorFactoryBlock
                    ?: throw IllegalArgumentException("Cause processor factory block not set")
            return SingleErrorPayloadGenerator(
                causeResolverFactoryBlock(CauseResolvers),
                causeProcessorFactoryBlock(CauseProcessors)
            )
        }

        class Config<T : Any> {
            var causeResolverFactoryBlock: (CauseResolverFactoryBlock<T>)? = null
                private set
            var causeProcessorFactoryBlock: (CauseProcessorFactoryBlock<T>)? = null
                private set

            fun identifiedBy(factoryBlock: CauseResolverFactoryBlock<T>) = this.apply {
                causeResolverFactoryBlock = factoryBlock
            }

            fun withId(fixedId: String) = identifiedBy { fixedId(fixedId) }

            fun processedAs(factoryBlock: CauseProcessorFactoryBlock<T>) = this.apply {
                causeProcessorFactoryBlock = factoryBlock
            }
        }
    }
}

/**
 * Generator creating payloads for fault results caused by multiple errors.
 *
 * @param R type of sub-errors, that the main fault will be [split][subErrorExtractor] into
 */
class MultiErrorPayloadGenerator<T : Any, R : Any>(
    private val subErrorExtractor: SubErrorExtractor<T, R>,
    private val subErrorPayloadGenerator: SingleErrorPayloadGenerator<R>
) : ErrorPayloadGenerator<T, ErrorResponsePayload> {
    private val log = initLog()

    /**
     * @throws NoSubErrorsExtracted in case [extractor][subErrorExtractor] returns no sub-errors
     */
    override fun payloadOf(fault: T): ErrorResponsePayload {
        return (subErrorExtractor.subErrorsOf(fault).takeIf { it.isNotEmpty() } ?: throw NoSubErrorsExtracted())
            .also { when (it.size) { 1 -> logSingleCauseWarning() } }
            .map { subErrorPayloadGenerator.payloadOf(it) }
            .toPayload()
    }

    private fun logSingleCauseWarning() {
        val singleErrorPayloadGeneratorClassName = SingleErrorPayloadGenerator::class.qualifiedName
        log.warn { "Consider using $singleErrorPayloadGeneratorClassName to handle single cause faults" }
    }

    companion object Builder {
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

            fun whichAre(initBlock: InitBlock<SingleErrorPayloadGenerator.Builder.Config<R>>) = handledBy(
                SingleErrorPayloadGenerator.Builder.Config<R>().apply(initBlock)
                    .let { SingleErrorPayloadGenerator.buildFrom(it) }
            )
        }
    }
}

/**
 * Interface for objects responsible for extracting sub-errors from multi-cause fault results.
 *
 * @param T type of fault objects that the extractor supports
 * @param R type of sub-error objects that are generated by the extractor
 */
fun interface SubErrorExtractor<T, R> {
    fun subErrorsOf(fault: T): Collection<R>
}
operator fun <T : Any, R : Any> SubErrorExtractor<T, R>.invoke(fault: T): Collection<R> = subErrorsOf(fault)

class NoSubErrorsExtracted : RestXException()

typealias CauseResolverFactoryBlock<T> = FactoryBlock<CauseResolvers, CauseResolver<T>>
typealias CauseProcessorFactoryBlock<T> = FactoryBlock<CauseProcessors, CauseProcessor<T>>

/**
 * Factories of common implementation of [ErrorPayloadGenerator]s.
 * Additional factory methods should be added as an extension functions.
 */
object PayloadGenerators
