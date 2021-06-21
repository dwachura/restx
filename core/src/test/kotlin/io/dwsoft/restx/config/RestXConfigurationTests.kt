package io.dwsoft.restx.config

import io.dwsoft.restx.RestX
import io.dwsoft.restx.fault.cause.message.providedBy
import io.dwsoft.restx.fault.cause.standard
import io.dwsoft.restx.fault.payload.ApiError
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.fail

class RestXConfigurationTests : FunSpec({
    test("single error handler is created") {
        val response = RestX.respondTo<Exception> {
            payloadOf { error {
                identifiedBy { type() }
                processedBy { standard { message { providedBy { context.message!! } } } }
            } }
            status { of(500) }
        }.responseOf(RuntimeException("message"))

        response.status.code shouldBe 500
        response.payload shouldBe ApiError(RuntimeException::class.qualifiedName!!, "message")
    }

    test("multiple errors handler is created") {
        fail { "Not yet implemented!" }
    }

//
//
//    test("test") {
//        val response = RestX.respondTo<Exception> {
////            payloadOf { basic { exception() } }
//            payloadOf { error {
//                identifiedBy { type() }
//                processedBy { standard {
//                    code { fixed("code") }
//                    message { fixed("message") }
//                } }
//            } }
//            payloadOf { error { identifiedBy { fixedId("test") } } }
//            payloadOf { errors { identifiedBy { multipleCauses(suffixedBy = { sequenceOf("1", "2", "3") })  } } }
//            status { ResponseStatusProvider { httpStatus(500) } }
//        }.responseOf(RuntimeException("message"))
//
//        response.status.code shouldBe 500
//        response.payload shouldBe ApiError("RuntimeException", "message")
//    }
})