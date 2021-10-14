package io.dwsoft.restx.core.payload

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FreeSpec
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.string.shouldContain

class RequestDataErrorSourceTests : FreeSpec({
    "creation of a source without location throws exception" - {
        listOf(
            "" to "empty string",
            "   " to "blank string",
            "\t \n \r" to "\\t \\n \\r"
        ).forEach { (invalidValue, description) ->
            "invalid location: '$invalidValue' ($description)" - {
                shouldThrow<IllegalArgumentException> { Source.queryParam(invalidValue) }
                    .message shouldContain "Invalid data source location must be set"
            }
        }
    }
})
