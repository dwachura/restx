package io.dwsoft.restx

import io.dwsoft.restx.core.dsl.ResponseGenerators
import io.dwsoft.restx.core.dsl.RestXConfigurationException
import io.dwsoft.restx.core.response.ResponseGenerator

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
