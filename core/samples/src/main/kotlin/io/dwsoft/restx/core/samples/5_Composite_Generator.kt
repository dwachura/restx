package io.dwsoft.restx.core.samples

import io.dwsoft.restx.RestX
import io.dwsoft.restx.register

fun main() {
    val predefinedGenerator = RestX.treat<IllegalStateException> { asOperationError {
        withCode("ILLEGAL_STATE") // generate payloads with fixed code...
        withMessage("Illegal state exception") // ...and fixed message as well
        withStatus(500) // HTTP status of a response
    } }

    val compositeGenerator = RestX.compose { registeredByFaultType {
        register { predefinedGenerator } // register pre-defined generator
        register { // register in-line generator
            generatorFor<IllegalArgumentException> { asOperationError {
                withMessage("Illegal argument exception")
                withStatus(400)
            } }
        }
    } }

    var response = compositeGenerator.responseOf(IllegalStateException())
    println(response.payload)
    println(response.status)

    response = compositeGenerator.responseOf(IllegalArgumentException())
    println(response.payload)
    println(response.status)
}
