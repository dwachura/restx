package io.dwsoft.restx

import io.dwsoft.restx.core.Reflection
import io.dwsoft.restx.core.dsl.ResponseGenerators
import io.dwsoft.restx.core.response.ResponseGenerator
import io.dwsoft.restx.core.dsl.RestXConfigurationException
import kotlin.reflect.KClass

/**
 * Entry point to [ResponseGenerator] construction.
 */
object RestX {
    /**
     * Entry method to fluently configure [ResponseGenerator]s.
     *
     * @throws RestXConfigurationException in case of any errors during creation of a generator
     */
    fun <T : Any> config(factoryBlock: ResponseGenerators.() -> ResponseGenerator<T>) =
        factoryBlock(ResponseGenerators())
}

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
