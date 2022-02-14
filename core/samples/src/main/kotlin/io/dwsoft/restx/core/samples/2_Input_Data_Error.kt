package io.dwsoft.restx.core.samples

import io.dwsoft.restx.RestX
import io.dwsoft.restx.core.cause.message.generatedAs
import io.dwsoft.restx.core.payload.Message
import io.dwsoft.restx.core.payload.RequestDataError.Source

fun main() {
    class InvalidParamException(val type: Source.Type, val location: String, message: String)
        : RuntimeException(message)

    val generator = RestX.respondTo<InvalidParamException> { asRequestDataError {
        identifiedBy("INVALID_PARAM")
        withMessage { generatedAs { Message(context.localizedMessage) } }
        pointingInvalidValue { resolvedBy { cause ->
            cause.context.let { it.type.toSource(it.location) }
        } }
        withStatus(400)
    } }

    val response = generator.responseOf(InvalidParamException(Source.Type.QUERY, "queryParam1", "Invalid value"))
    println(response.payload)
    println(response.status)
}
