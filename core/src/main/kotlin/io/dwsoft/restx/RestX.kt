package io.dwsoft.restx

import io.dwsoft.restx.core.Reflection
import io.dwsoft.restx.core.response.CompositeResponseGenerator
import io.dwsoft.restx.core.response.ResponseGenerator
import io.dwsoft.restx.core.response.SimpleResponseGenerator
import kotlin.reflect.KClass

/**
 * Entry point to [ResponseGenerator] construction.
 */
object RestX {
    /**
     * Entry method to fluently configure [SimpleResponseGenerator]s.
     *
     * @throws RestXConfigurationFailure in case of any errors during creation of a generator
     */
    fun <T : Any> respondTo(initBlock: InitBlock<SimpleResponseGeneratorBuilder.Config<T>>) = buildGenerator {
        SimpleResponseGeneratorBuilder.Config<T>()
            .apply(initBlock)
            .let { SimpleResponseGeneratorBuilder.buildFrom(it) }
    }

    private fun <R : ResponseGenerator<T>, T : Any> buildGenerator(buildFunction: () -> R) =
        runCatching(buildFunction).onFailure { RestXConfigurationFailure(it) }.getOrThrow()

    /**
     * Delegate of [respondTo].
     */
    fun <T : Any> generator(initBlock: InitBlock<SimpleResponseGeneratorBuilder.Config<T>>) = respondTo(initBlock)

    /**
     * Delegate of [respondTo]. May be more readable in some situations.
     */
    @Suppress("UNUSED_PARAMETER")
    fun <T : Any> respondToFaultOfType(
        faultObjectsType: KClass<T>,
        initBlock: InitBlock<SimpleResponseGeneratorBuilder.Config<T>>
    ): ResponseGenerator<T> = respondTo(initBlock)

    /**
     * Delegate of [respondToFaultOfType].
     */
    fun <T : Any> generator(
        faultObjectsType: KClass<T>,
        initBlock: InitBlock<SimpleResponseGeneratorBuilder.Config<T>>
    ) = respondToFaultOfType(faultObjectsType, initBlock)

    /**
     * Entry method to fluently configure [CompositeResponseGenerator]s.
     *
     * @throws RestXConfigurationFailure in case of any errors during creation of a generator
     */
    fun compose(initBlock: InitBlock<CompositeResponseGenerator.Builder.Config>) = buildGenerator {
        CompositeResponseGenerator.Builder.Config()
            .apply(initBlock)
            .let { CompositeResponseGenerator.buildFrom(it) }
    }
}

class RestXConfigurationFailure(cause: Throwable) : RestXException("RestX configuration failed", cause)

/**
 * Base class for exceptions thrown by RestX library components
 */
abstract class RestXException : RuntimeException {
    constructor() : super()
    constructor(cause: Throwable) : super(cause)
    constructor(message: String) : super(message)
    constructor(message: String, cause: Throwable) : super(message, cause)
}

internal typealias InitBlock<T> = T.() -> Unit
fun interface Initializer<T> {
    operator fun invoke(config: T)
}
fun <T> Initializer<T>.initBy(): InitBlock<T> = { this@initBy(this) }
fun <T> Initializer<T>.init(): InitBlock<T> = initBy()

internal typealias FactoryBlock<T, R> = T.() -> R
fun interface Factory<T, R> {
    operator fun invoke(factories: T): R
}
fun <T, R> Factory<T, R>.madeBy(): FactoryBlock<T, R> = { this@madeBy(this) }
fun <T, R> Factory<T, R>.make(): FactoryBlock<T, R> = madeBy()

fun <T : Any> Class<T>.kotlinClass(): KClass<T> = Reflection.type(this)
