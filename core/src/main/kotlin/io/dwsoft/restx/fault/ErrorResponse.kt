package io.dwsoft.restx.fault

class ErrorResponse(val status: HttpStatus, errors: List<ApiError>) {
    val payload: ErrorResponsePayload = errors.toPayload()

    constructor(status: HttpStatus, error: ApiError) : this(status, listOf(error))

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ErrorResponse

        if (status != other.status) return false
        if (payload != other.payload) return false

        return true
    }

    override fun hashCode(): Int {
        var result = status.hashCode()
        result = 31 * result + payload.hashCode()
        return result
    }

    override fun toString(): String {
        return "ErrorResponse(status=$status, payload=$payload)"
    }
}

private fun List<ApiError>.toPayload() = when (size) {
    1 -> first()
    else -> MultiErrorPayload(this)
}

data class HttpStatus(val code: Int)

fun status(code: Int): HttpStatus = HttpStatus(code)

sealed interface ErrorResponsePayload

data class ApiError(val code: String, val message: String) : ErrorResponsePayload

data class MultiErrorPayload(val errors: List<ApiError>) : ErrorResponsePayload
