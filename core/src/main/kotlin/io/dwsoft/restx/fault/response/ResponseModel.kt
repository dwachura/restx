package io.dwsoft.restx.fault.response

import io.dwsoft.restx.fault.payload.ErrorResponsePayload

/**
 * Model of HTTP response containing info about error(s) that happened into API process.
 */
class ErrorResponse(val status: HttpStatus, val payload: ErrorResponsePayload) {
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

data class HttpStatus(val code: Int)
