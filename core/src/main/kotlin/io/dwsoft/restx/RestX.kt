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
     * @throws RestXConfigurationException in case of any errors during creation of a generator
     */
    fun <T : Any> treat(
        factoryBlock: FactoryBlock<SimpleResponseGeneratorBuilders<T>, SimpleResponseGenerator<T>>
    ) = buildGenerator { factoryBlock.invoke(SimpleResponseGeneratorBuilders()) }

    private fun <R : ResponseGenerator<T>, T : Any> buildGenerator(buildFunction: () -> R) =
        runCatching(buildFunction).fold(
            onSuccess = { it },
            onFailure = { throw RestXConfigurationException(it) }
        )

    /**
     * Delegate of [treat]. May be more readable in some situations.
     */
    @Suppress("UNUSED_PARAMETER")
    fun <T : Any> respondToFaultOfType(
        faultObjectsType: KClass<T>,
        factoryBlock: FactoryBlock<SimpleResponseGeneratorBuilders<T>, SimpleResponseGenerator<T>>
    ) = treat(factoryBlock)

    /**
     * Delegate of [treat].
     */
    fun <T : Any> generatorFor(
        factoryBlock: FactoryBlock<SimpleResponseGeneratorBuilders<T>, SimpleResponseGenerator<T>>
    ) = treat(factoryBlock)

    /**
     * Delegate of [generatorFor].
     */
    @Suppress("UNUSED_PARAMETER")
    fun <T : Any> generatorForFaultsOfType(
        faultObjectsType: KClass<T>,
        factoryBlock: FactoryBlock<SimpleResponseGeneratorBuilders<T>, SimpleResponseGenerator<T>>
    ) = generatorFor(factoryBlock)

    /**
     * Entry method to fluently configure [CompositeResponseGenerator]s.
     *
     * @throws RestXConfigurationException in case of any errors during creation of a generator
     */
    fun compose(initBlock: InitBlock<CompositeResponseGeneratorDsl>) = buildGenerator {
        CompositeResponseGeneratorBuilder.Dsl()
            .apply(initBlock)
            .let { CompositeResponseGeneratorBuilder.buildFrom(it) }
    }
}

class RestXConfigurationException(cause: Throwable) : RestXException("RestX configuration failed", cause)

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
