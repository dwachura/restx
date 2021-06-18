package io.dwsoft.restx

import io.dwsoft.restx.response.ResponseGenerator

typealias RestX = ResponseGenerator<Any>

/**
 * Base class for exceptions thrown by RestX library components
 */
abstract class RestXException : RuntimeException {
    constructor() : super()
    constructor(cause: Throwable) : super(cause)
    constructor(message: String) : super(message)
    constructor(message: String, cause: Throwable) : super(message, cause)
}