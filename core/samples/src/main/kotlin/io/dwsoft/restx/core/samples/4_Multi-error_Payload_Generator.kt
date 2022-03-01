package io.dwsoft.restx.core.samples

import io.dwsoft.restx.RestX
import io.dwsoft.restx.core.dsl.generatedAs
import io.dwsoft.restx.core.dsl.withStatus
import io.dwsoft.restx.core.response.payload.Message

fun main() {
    val generator = RestX.config {
        treat<List<Exception>> {
            asContainerOf<Exception> {
                extractedAs { it }
                eachRepresenting {
                    operationError { withMessage { generatedAs { Message(context.localizedMessage) } } }
                }
                withStatus(500)
            }
        }
    }

    val response = generator.responseOf(
        listOf(
            Exception("Generic error"),
            IllegalArgumentException("Bad argument"),
            NumberFormatException("Wrong number")
        )
    )
    println(response.payload)
    println(response.status)
}
