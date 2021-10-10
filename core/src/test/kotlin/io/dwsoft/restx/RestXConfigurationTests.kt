package io.dwsoft.restx

import io.dwsoft.restx.fault.cause.code
import io.dwsoft.restx.fault.cause.message
import io.dwsoft.restx.fault.payload.MultiErrorPayload
import io.dwsoft.restx.fault.payload.OperationError
import io.dwsoft.restx.fault.payload.RequestDataError
import io.dwsoft.restx.fault.payload.Source
import io.dwsoft.restx.fault.response.HttpStatus
import io.dwsoft.restx.fault.response.ResponseGenerator
import io.dwsoft.restx.fault.response.status
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainInOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeTypeOf
import io.mockk.verify

class RestXConfigurationTests : FunSpec({
    test("generator of single error payloads with code the same as object type is created") {
        val generator = RestX.respondTo<Any> {
            payload {
                error {
                    identifiedBy { type() }
                    processedAs { operationError {
                        code { sameAsCauseId() }
                        message { dummy() }
                    } }
                }
            }
            status { dummy() }
        }

        generator.responseOf(Any())
            .payload.shouldBeTypeOf<OperationError>()
            .apply { code shouldBe Any::class.qualifiedName }
    }

    test("generator of single error payloads with fixed code is created") {
        val expectedCode = "code"
        val generator = RestX.respondTo<Any> {
            payload {
                error {
                    identifiedBy { type() }
                    processedAs { operationError {
                        code(expectedCode)
                        message { dummy() }
                    } }
                }
            }
            status { dummy() }
        }

        generator.responseOf(Any())
            .payload.shouldBeTypeOf<OperationError>()
            .apply { code shouldBe expectedCode }
    }

    test("generator of single error payloads with custom generated code is created") {
        val expectedCode = "code"
        val generator = RestX.respondTo<Any> {
            payload {
                error {
                    withId("dummy")
                    processedAs { operationError {
                        code { generatedAs { expectedCode } }
                        message { dummy() }
                    } }
                }
            }
            status { dummy() }
        }

        generator.responseOf(Any())
            .payload.shouldBeTypeOf<OperationError>()
            .apply { code shouldBe expectedCode }
    }

    test("generator of single error payloads with map-based code is created") {
        val causeId = "id"
        val expectedCode = "code"
        val generator = RestX.respondTo<Any> {
            payload {
                error {
                    withId(causeId)
                    processedAs { operationError {
                        code { mapBased(causeId to expectedCode) }
                        message { dummy() }
                    } }
                }
            }
            status { dummy() }
        }

        generator.responseOf(Any())
            .payload.shouldBeTypeOf<OperationError>()
            .apply { code shouldBe expectedCode }
    }

    test("generator of single error payloads with fixed message is created") {
        val expectedMessage = "message"
        val generator = RestX.respondTo<Any> {
            payload {
                error {
                    identifiedBy { type() }
                    processedAs { operationError {
                        message(expectedMessage)
                    } }
                }
            }
            status { dummy() }
        }

        generator.responseOf(Any())
            .payload.shouldBeTypeOf<OperationError>()
            .apply { message shouldBe expectedMessage }
    }

    test("generator of single error payloads with custom generated message is created") {
        val faultResult = RuntimeException("message")
        val generator = RestX.respondTo<Exception> {
            payload {
                error {
                    identifiedBy { type() }
                    processedAs { operationError {
                        message { generatedAs { context.message!! } }
                    } }
                }
            }
            status { dummy() }
        }

        val response = generator.responseOf(faultResult)

        response.payload.shouldBeTypeOf<OperationError>()
            .apply { message shouldBe faultResult.message }
    }

    test("generator of single error payloads with map-based message is created") {
        val causeId = "id"
        val expectedMessage = "message"
        val generator = RestX.respondTo<Any> {
            payload {
                error {
                    withId(causeId)
                    processedAs { operationError {
                        message { mapBased(causeId to expectedMessage) }
                    } }
                }
            }
            status { dummy() }
        }

        val response = generator.responseOf(Any())

        response.payload.shouldBeTypeOf<OperationError>()
            .apply { message shouldBe expectedMessage }
    }

    test("single error payload with defined status is created") {
        val status = 500
        val generator = RestX.respondTo<Any> {
            payload {
                error {
                    identifiedBy { type() }
                    processedAs { operationError {
                        message { dummy() }
                    } }
                }
            }
            status(status)
        }

        val response = generator.responseOf(Any())

        response.status shouldBe HttpStatus(status)
    }

    test("generator of single error payloads for invalid request data errors is created") {
        class InvalidInput(val type: Source.Type, val location: String, val message: String)
        val expectedSource = Source.queryParam("queryParam1")
        val expectedMessage = "Invalid value in query param"
        val generator = RestX.respondTo<InvalidInput> {
            payload {
                error {
                    identifiedBy { fixedId(InvalidInput::class.simpleName!!) }
                    processedAs {
                        requestDataError {
                            message { generatedAs { context.message } }
                            invalidValue { resolvedBy { cause ->
                                cause.context.let { it.type.toSource(it.location) }
                            } }
                        }
                    }
                }
            }
            status(400)
        }

        val response = generator.responseOf(
            InvalidInput(expectedSource.type, expectedSource.location, expectedMessage)
        )

        response.payload.shouldBeTypeOf<RequestDataError>().apply {
            source shouldBe expectedSource
        }
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
                    processedAs {
                        operationError {
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
                OperationError(subError1::class.qualifiedName!!, subError1.message!!),
                OperationError(subError2::class.qualifiedName!!, subError2.message!!),
                OperationError(subError3::class.qualifiedName!!, subError3.message!!)
            ) }
    }

    test("composite generator is created") {
        val exceptionFault = Exception()
        val generatorForExceptionFault = dummy<ResponseGenerator<Exception>>()
        val runtimeExceptionFault = RuntimeException()
        val generatorForRuntimeExceptionFault = dummy<ResponseGenerator<RuntimeException>>()
        val stringFault = "fault"
        val generatorForStringFault = dummy<ResponseGenerator<String>>()
        val compositeGenerator = RestX.compose {
            registeredByFaultType {
                register { generatorForExceptionFault }
                register { generatorForRuntimeExceptionFault }
                register { generatorForStringFault }
            }
        }

        compositeGenerator.responseOf(exceptionFault)
        compositeGenerator.responseOf(runtimeExceptionFault)
        compositeGenerator.responseOf(stringFault)

        verify {
            generatorForExceptionFault.responseOf(exceptionFault)
            generatorForRuntimeExceptionFault.responseOf(runtimeExceptionFault)
            generatorForStringFault.responseOf(stringFault)
        }
    }
})
