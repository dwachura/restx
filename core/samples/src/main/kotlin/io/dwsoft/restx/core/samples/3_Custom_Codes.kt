package io.dwsoft.restx.core.samples

import io.dwsoft.restx.RestX
import java.io.IOException
import io.dwsoft.restx.core.cause.code.mapBased

fun main() {
    val generator = RestX.respondTo<Exception> { asOperationError {
        withCode { mapBased( // codes will be taken from defined map - key == fault id (type name, as defined above)
            Exception::class.qualifiedName!! to "GENERIC_FAILURE",
            RuntimeException::class.qualifiedName!! to "RUNTIME_FAILURE",
            IOException::class.qualifiedName!! to "IO_FAILURE"
        ) }
        withMessage("Error message")
        withStatus(500) // HTTP status of a response
    } }

    var response = generator.responseOf(RuntimeException("Runtime failure"))
    println(response.payload)

    response = generator.responseOf(IOException("I/O failure"))
    println(response.payload)
}
