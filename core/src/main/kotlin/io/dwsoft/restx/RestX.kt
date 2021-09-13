package io.dwsoft.restx

import io.dwsoft.restx.fault.response.ResponseGenerator
import kotlin.jvm.internal.Reflection
import kotlin.reflect.KClass

/**
 * Class serving as an entry point to [ResponseGenerator] construction.
 */
class RestX {
    companion object {
        /**
         * Entry method to fluently configure [ResponseGenerator]s.
         *
         * @throws RestXConfigurationFailure in case of any errors during creation of a generator
         */
        fun <T : Any> respondTo(
            initBlock: InitBlock<ResponseGenerator.Builder.Config<T>>
        ): ResponseGenerator<T> = runCatching {
            ResponseGenerator.Builder.Config<T>()
                .apply(initBlock)
                .let { ResponseGenerator.buildFrom(it) }
        }
            .onFailure { RestXConfigurationFailure(it) }
            .getOrThrow()

        /**
         * Overloaded version of [respondTo]. May be more readable in some situations.
         */
        @Suppress("UNUSED_PARAMETER")
        fun <T : Any> respondTo(
            faultObjectsType: KClass<T>,
            initBlock: InitBlock<ResponseGenerator.Builder.Config<T>>
        ): ResponseGenerator<T> = respondTo(initBlock)
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

@Suppress("UNCHECKED_CAST")
fun <T : Any> type(javaClass: Class<T>): KClass<T> = Reflection.getOrCreateKotlinClass(javaClass) as KClass<T>
fun <T : Any> of(javaClass: Class<T>): KClass<T> = type(javaClass)
