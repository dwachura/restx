package io.dwsoft.restx.payload

/**
 * Base interface used for flag classes that represents valid error response payload.
 */
sealed interface ErrorResponsePayload

/**
 * Representation of single error response payload.
 */
data class ApiError(val code: String, val message: String) : ErrorResponsePayload

/**
 * Representation of a response payload generated in case of multiple errors happen during
 * execution of application's logic.
 */
data class MultiErrorPayload(val errors: List<ApiError>) : ErrorResponsePayload

fun List<ApiError>.toPayload() = when (size) {
    1 -> first()
    else -> MultiErrorPayload(this)
}