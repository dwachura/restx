package io.dwsoft.restx.core.response.payload

import io.dwsoft.restx.core.response.payload.RequestDataError.Source
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.string.shouldContain

class RequestDataErrorSourceTests : FreeSpec({
    "creation of a source without location throws exception" - {
        listOf(
            "" to "empty string",
            "   " to "blank string",
            "\t \n \r" to "\\t \\n \\r"
        ).forEach { (invalidValue, description) ->
            "invalid location: '$invalidValue' ($description)" - {
                shouldThrow<IllegalArgumentException> { Source.query(invalidValue) }
                    .message shouldContain "Invalid data source location must be set"
            }
        }
    }
})
