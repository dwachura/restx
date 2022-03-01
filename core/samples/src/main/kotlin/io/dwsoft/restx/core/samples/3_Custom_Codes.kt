package io.dwsoft.restx.core.samples

import io.dwsoft.restx.RestX
import io.dwsoft.restx.core.dsl.mapBased
import io.dwsoft.restx.core.dsl.withMessage
import io.dwsoft.restx.core.dsl.withStatus
import java.io.IOException

fun main() {
    val generator = RestX.config {
        treat<Exception> { asOperationError {
            withCode { mapBased( // codes will be taken from defined map - key == fault key (type name, as defined above)
                Exception::class.qualifiedName!! to "GENERIC_FAILURE",
                RuntimeException::class.qualifiedName!! to "RUNTIME_FAILURE",
                IOException::class.qualifiedName!! to "IO_FAILURE"
            ) }
            withMessage("Error message")
            withStatus(500) // HTTP status of a response
        } }
    }

    var response = generator.responseOf(RuntimeException("Runtime failure"))
    println(response.payload)

    response = generator.responseOf(IOException("I/O failure"))
    println(response.payload)
}
