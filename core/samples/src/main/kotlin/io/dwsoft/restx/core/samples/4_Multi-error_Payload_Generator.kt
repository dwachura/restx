package io.dwsoft.restx.core.samples

import io.dwsoft.restx.RestX
import io.dwsoft.restx.core.payload.Message
import io.dwsoft.restx.core.cause.message.generatedAs

fun main() {
    val generator = RestX.treat<List<Exception>> { asContainerOf<Exception> {
        extractedAs { it }
        eachRepresenting { operationError {
            withMessage { generatedAs { Message(context.localizedMessage) } }
        } }
        withStatus(500)
    } }

    val response = generator.responseOf(listOf(
        Exception("Generic error"), IllegalArgumentException("Bad argument"), NumberFormatException("Wrong number")
    ))
    println(response.payload)
    println(response.status)
}
