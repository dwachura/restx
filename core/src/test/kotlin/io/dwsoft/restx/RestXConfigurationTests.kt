package io.dwsoft.restx

import io.dwsoft.restx.core.dsl.generatedAs
import io.dwsoft.restx.core.dsl.identifiedBy
import io.dwsoft.restx.core.dsl.mapBased
import io.dwsoft.restx.core.dsl.register
import io.dwsoft.restx.core.dsl.withCode
import io.dwsoft.restx.core.dsl.withMessage
import io.dwsoft.restx.core.dsl.withStatus
import io.dwsoft.restx.core.dummy
import io.dwsoft.restx.core.payload.MultiErrorPayload
import io.dwsoft.restx.core.payload.OperationError
import io.dwsoft.restx.core.payload.RequestDataError
import io.dwsoft.restx.core.payload.RequestDataError.Source
import io.dwsoft.restx.core.payload.asMessage
import io.dwsoft.restx.core.response.HttpStatus
import io.dwsoft.restx.core.response.ResponseGenerator
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeTypeOf
import io.mockk.verify

// TODO: default configurations
class RestXConfigurationTests : FunSpec({
    test("generator of single error payloads with code the same as object type is created") {
        val generator = RestX.config {
            treat<Any> { asOperationError {
                withMessage { dummy() }
                withStatus { dummy() }
            } }
        }

        generator.responseOf(Any())
            .payload.shouldBeTypeOf<OperationError>()
            .apply { code shouldBe Any::class.qualifiedName }
    }

    test("generator of single error payloads with fixed code is created") {
        val expectedCode = "code"
        val generator = RestX.config {
            treat<Any> { asOperationError {
                withCode(expectedCode)
                withMessage { dummy() }
                withStatus { dummy() }
            } }
        }

        generator.responseOf(Any())
            .payload.shouldBeTypeOf<OperationError>()
            .apply { code shouldBe expectedCode }
    }

    test("generator of single error payloads with custom generated code is created") {
        val expectedCode = "code"
        val generator = RestX.config {
            treat<Any> { asOperationError {
                withCode { generatedAs { expectedCode } }
                withMessage { dummy() }
                withStatus { dummy() }
            } }
        }

        generator.responseOf(Any())
            .payload.shouldBeTypeOf<OperationError>()
            .apply { code shouldBe expectedCode }
    }

    test("generator of single error payloads with map-based code is created") {
        val causeKey = "key"
        val expectedCode = "code"
        val generator = RestX.config {
            treat<Any> { asOperationError {
                identifiedBy(causeKey)
                withCode { mapBased(causeKey to expectedCode) }
                withMessage { dummy() }
                withStatus { dummy() }
            } }
        }

        generator.responseOf(Any())
            .payload.shouldBeTypeOf<OperationError>()
            .apply { code shouldBe expectedCode }
    }

    test("generator of single error payloads with fixed message is created") {
        val expectedMessage = "message"
        val generator = RestX.config {
            treat<Any> { asOperationError {
                withMessage(expectedMessage)
                withStatus { dummy() }
            } }
        }

        generator.responseOf(Any())
            .payload.shouldBeTypeOf<OperationError>()
            .apply { message.value shouldBe expectedMessage }
    }

    test("generator of single error payloads with custom generated message is created") {
        val faultResult = RuntimeException("message")
        val generator = RestX.config {
            treat<Exception> { asOperationError {
                withMessage { generatedAs { context.message!!.asMessage() } }
                withStatus { dummy() }
            } }
        }

        val response = generator.responseOf(faultResult)

        response.payload.shouldBeTypeOf<OperationError>()
            .apply { message.value shouldBe faultResult.message }
    }

    test("single error payload with defined status is created") {
        val status = 500
        val generator = RestX.config {
            treat<Any> { asOperationError {
                withMessage { dummy() }
                withStatus(status)
            } }
        }

        val response = generator.responseOf(Any())

        response.status shouldBe HttpStatus(status)
    }

    test("generator of single error payloads for invalid request data errors is created") {
        class InvalidInput(val type: Source.Type, val location: String)
        val expectedSource = Source.query("queryParam1")
        val generator = RestX.config {
            treat<InvalidInput> { asRequestDataError {
                withMessage { generatedAs { dummy() } }
                pointingInvalidValue { resolvedBy { cause -> cause.context.let { it.type.toSource(it.location) } } }
                withStatus { dummy() }
            } }
        }

        val response = generator.responseOf(
            InvalidInput(expectedSource.type, expectedSource.location)
        )

        response.payload.shouldBeTypeOf<RequestDataError>().apply {
            source shouldBe expectedSource
        }
    }

    test("generator of multi-error payload is created") {
        class MultiExceptionFaultResult(vararg val errors: Exception)
        val exceptions = listOf(
            Exception("Generic error"),
            IllegalArgumentException("Bad argument"),
            NumberFormatException("Wrong number")
        )
        val fault = MultiExceptionFaultResult(*exceptions.toTypedArray())
        val generator = RestX.config {
            treat<MultiExceptionFaultResult> { asContainerOf<Exception> {
                extractedAs { it.errors.asList() }
                eachRepresenting { operationError {
                    withMessage { generatedAs { context.message!!.asMessage() } }
                } }
                withStatus(500)
            } }
        }

        val response = generator.responseOf(fault)

        response.status shouldBe HttpStatus(500)
        response.payload.shouldBeTypeOf<MultiErrorPayload>()
            .apply { errors.forEach { it::class shouldBe OperationError::class } }
            .apply { errors.forEachIndexed { i, error -> error.code shouldBe exceptions[i]::class.qualifiedName } }
            .apply { errors.forEachIndexed { i, error -> error.message.value shouldBe exceptions[i].message } }
    }

    test("composite generator is created") {
        val exceptionFault = Exception()
        val generatorForExceptionFault = dummy<ResponseGenerator<Exception>>()
        val runtimeExceptionFault = RuntimeException()
        val generatorForRuntimeExceptionFault = dummy<ResponseGenerator<RuntimeException>>()
        val stringFault = "fault"
        val generatorForStringFault = dummy<ResponseGenerator<String>>()
        val compositeGenerator = RestX.config {
            compose { registeredByFaultType {
                register { generatorForExceptionFault }
                register { generatorForRuntimeExceptionFault }
                register { generatorForStringFault }
            } }
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
