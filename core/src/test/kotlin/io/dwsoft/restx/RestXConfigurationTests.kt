package io.dwsoft.restx

import io.dwsoft.restx.fault.cause.code
import io.dwsoft.restx.fault.cause.message
import io.dwsoft.restx.fault.dummy
import io.dwsoft.restx.fault.payload.ApiError
import io.dwsoft.restx.fault.payload.MultiErrorPayload
import io.dwsoft.restx.fault.response.HttpStatus
import io.dwsoft.restx.fault.response.status
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainInOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeTypeOf

class RestXConfigurationTests : FunSpec({
    test("()") {
        RestX.Companion.respondTo<Exception> {
            payload {
                error {
                    identifiedBy { type() }
                    processedBy {
                        standard {
                            code { sameAsCauseId() }
                            message { generatedAs { context.message!! } }
                        }
                    }
                }
            }
            status { of(500) }
        }
    }

    test("generator of single error payloads with code the same as object type is created") {
        val generator = RestX.respondTo<Any> {
            payload {
                error {
                    identifiedBy { type() }
                    processedBy { standard {
                        code { sameAsCauseId() }
                        message { dummy() }
                    } }
                }
            }
            status { dummy() }
        }

        generator.responseOf(Any())
            .payload.shouldBeTypeOf<ApiError>()
            .apply { code shouldBe Any::class.qualifiedName }
    }

    test("generator of single error payloads with fixed code is created") {
        val expectedCode = "code"
        val generator = RestX.respondTo<Any> {
            payload {
                error {
                    identifiedBy { type() }
                    processedBy { standard {
                        code(expectedCode)
                        message { dummy() }
                    } }
                }
            }
            status { dummy() }
        }

        generator.responseOf(Any())
            .payload.shouldBeTypeOf<ApiError>()
            .apply { code shouldBe expectedCode }
    }

    test("generator of single error payloads with custom generated code is created") {
        val expectedCode = "code"
        val generator = RestX.respondTo<Any> {
            payload {
                error {
                    withId("dummy")
                    processedBy { standard {
                        code { generatedAs { expectedCode } }
                        message { dummy() }
                    } }
                }
            }
            status { dummy() }
        }

        generator.responseOf(Any())
            .payload.shouldBeTypeOf<ApiError>()
            .apply { code shouldBe expectedCode }
    }

    test("generator of single error payloads with map-based code is created") {
        val causeId = "id"
        val expectedCode = "code"
        val generator = RestX.respondTo<Any> {
            payload {
                error {
                    withId(causeId)
                    processedBy { standard {
                        code { mapBased(causeId to expectedCode) }
                        message { dummy() }
                    } }
                }
            }
            status { dummy() }
        }

        generator.responseOf(Any())
            .payload.shouldBeTypeOf<ApiError>()
            .apply { code shouldBe expectedCode }
    }

    test("generator of single error payloads with fixed message is created") {
        val expectedMessage = "message"
        val generator = RestX.respondTo<Any> {
            payload {
                error {
                    identifiedBy { type() }
                    processedBy { standard {
                        message(expectedMessage)
                    } }
                }
            }
            status { dummy() }
        }

        generator.responseOf(Any())
            .payload.shouldBeTypeOf<ApiError>()
            .apply { message shouldBe expectedMessage }
    }

    test("generator of single error payloads with custom generated message is created") {
        val faultResult = RuntimeException("message")
        val generator = RestX.respondTo<Exception> {
            payload {
                error {
                    identifiedBy { type() }
                    processedBy { standard {
                        message { generatedAs { context.message!! } }
                    } }
                }
            }
            status { dummy() }
        }

        val response = generator.responseOf(faultResult)

        response.payload.shouldBeTypeOf<ApiError>()
            .apply { message shouldBe faultResult.message }
    }

    test("generator of single error payloads with map-based message is created") {
        val causeId = "id"
        val expectedMessage = "message"
        val generator = RestX.respondTo<Any> {
            payload {
                error {
                    withId(causeId)
                    processedBy { standard {
                        message { mapBased(causeId to expectedMessage) }
                    } }
                }
            }
            status { dummy() }
        }

        val response = generator.responseOf(Any())

        response.payload.shouldBeTypeOf<ApiError>()
            .apply { message shouldBe expectedMessage }
    }

    test("single error payload with defined status is created") {
        val status = 500
        val generator = RestX.respondTo<Any> {
            payload {
                error {
                    identifiedBy { type() }
                    processedBy { standard {
                        message { dummy() }
                    } }
                }
            }
            status(status)
        }

        val response = generator.responseOf(Any())

        response.status shouldBe HttpStatus(status)
    }

    test("generator of multi-error payload is created") {
        class MultiExceptionFaultResult(vararg val errors: Exception)
        val subError1 = Exception("Generic error")
        val subError2 = IllegalArgumentException("Bad argument")
        val subError3 = NumberFormatException("Wrong number")
        val fault = MultiExceptionFaultResult(subError1, subError2, subError3)
        val generator = RestX.respondTo<MultiExceptionFaultResult> {
            payload { subErrors<Exception> {
                extractedAs { it.errors.asList() }
                whichAre {
                    identifiedBy { type() }
                    processedBy {
                        standard {
                            message { generatedAs { context.message!! } }
                        }
                    }
                }
            } }
            status(500)
        }

        val response = generator.responseOf(fault)


        response.status shouldBe HttpStatus(500)
        response.payload.shouldBeTypeOf<MultiErrorPayload>()
            .apply { errors shouldContainInOrder listOf(
                ApiError(subError1::class.qualifiedName!!, subError1.message!!),
                ApiError(subError2::class.qualifiedName!!, subError2.message!!),
                ApiError(subError3::class.qualifiedName!!, subError3.message!!)
            ) }
    }
})