package io.dwsoft.restx.core.samples

import io.dwsoft.restx.RestX
import io.dwsoft.restx.core.cause.message.generatedAs
import io.dwsoft.restx.core.payload.Message

fun main() {
    val generator = RestX.respondTo<Exception> { asOperationError {
        identifiedBy { type() } // identify faults by its type - could be omitted, as it's a default behavior
        withCode { sameAsCauseId() } // generate payloads with code same as fault's identifier - could be omitted, as it's a default behavior
        withMessage { generatedAs { Message(context.localizedMessage) } } // generate payloads with exception message
        withStatus(500) // HTTP status of a response
    } }

    val response = generator.responseOf(RuntimeException("Service failure"))
    println(response.payload)
    println(response.status)
}
