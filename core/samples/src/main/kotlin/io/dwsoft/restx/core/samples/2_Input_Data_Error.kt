package io.dwsoft.restx.core.samples

import io.dwsoft.restx.RestX
import io.dwsoft.restx.core.dsl.generatedAs
import io.dwsoft.restx.core.dsl.identifiedBy
import io.dwsoft.restx.core.dsl.withStatus
import io.dwsoft.restx.core.response.payload.Message
import io.dwsoft.restx.core.response.payload.RequestDataError.Source

fun main() {
    class InvalidParamException(val type: Source.Type, val location: String, message: String) :
        RuntimeException(message)

    val generator = RestX.config {
        treat<InvalidParamException> {
            asRequestDataError {
                identifiedBy("INVALID_PARAM")
                withMessage { generatedAs { Message(context.localizedMessage) } }
                pointingInvalidValue {
                    resolvedBy { cause -> cause.context.let { it.type.toSource(it.location) } }
                }
                withStatus(400)
            }
        }
    }

    val response = generator.responseOf(InvalidParamException(Source.Type.QUERY, "queryParam1", "Invalid value"))
    println(response.payload)
    println(response.status)
}
