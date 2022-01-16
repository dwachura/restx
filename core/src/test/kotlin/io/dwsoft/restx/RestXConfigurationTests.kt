package io.dwsoft.restx

import io.dwsoft.restx.core.dummy
import io.dwsoft.restx.core.payload.MultiErrorPayload
import io.dwsoft.restx.core.payload.OperationError
import io.dwsoft.restx.core.payload.RequestDataError
import io.dwsoft.restx.core.payload.Source
import io.dwsoft.restx.core.response.HttpStatus
import io.dwsoft.restx.core.response.ResponseGenerator
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainInOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeTypeOf
import io.mockk.verify

class RestXConfigurationTests : FunSpec({
    test("generator of single error payloads with code the same as object type is created") {
        val generator = RestX.respondTo<Any> { asOperationError {
            withMessage { dummy() }
            withStatus { dummy() }
        } }

        generator.responseOf(Any())
            .payload.shouldBeTypeOf<OperationError>()
            .apply { code shouldBe Any::class.qualifiedName }
    }

    test("generator of single error payloads with fixed code is created") {
        val expectedCode = "code"
        val generator = RestX.respondTo<Any> { asOperationError {
            withCode(expectedCode)
            withMessage { dummy() }
            withStatus { dummy() }
        } }

        generator.responseOf(Any())
            .payload.shouldBeTypeOf<OperationError>()
            .apply { code shouldBe expectedCode }
    }

    test("generator of single error payloads with custom generated code is created") {
        val expectedCode = "code"
        val generator = RestX.respondTo<Any> { asOperationError {
            withCode { generatedAs { expectedCode } }
            withMessage { dummy() }
            withStatus { dummy() }
        } }

        generator.responseOf(Any())
            .payload.shouldBeTypeOf<OperationError>()
            .apply { code shouldBe expectedCode }
    }

    test("generator of single error payloads with map-based code is created") {
        val causeId = "id"
        val expectedCode = "code"
        val generator = RestX.respondTo<Any> { asOperationError {
            identifiedBy(causeId)
            withCode { mapBased(causeId to expectedCode) }
            withMessage { dummy() }
            withStatus { dummy() }
        } }

        generator.responseOf(Any())
            .payload.shouldBeTypeOf<OperationError>()
            .apply { code shouldBe expectedCode }
    }

    test("generator of single error payloads with fixed message is created") {
        val expectedMessage = "message"
        val generator = RestX.respondTo<Any> { asOperationError {
            withMessage(expectedMessage)
            withStatus { dummy() }
        } }

        generator.responseOf(Any())
            .payload.shouldBeTypeOf<OperationError>()
            .apply { message shouldBe expectedMessage }
    }

    test("generator of single error payloads with custom generated message is created") {
        val faultResult = RuntimeException("message")
        val generator = RestX.respondTo<Exception> { asOperationError {
            withMessage { generatedAs { context.message!! } }
            withStatus { dummy() }
        } }

        val response = generator.responseOf(faultResult)

        response.payload.shouldBeTypeOf<OperationError>()
            .apply { message shouldBe faultResult.message }
    }

    test("generator of single error payloads with map-based message is created") {
        val causeId = "id"
        val expectedMessage = "message"
        val generator = RestX.respondTo<Any> { asOperationError {
            identifiedBy(causeId)
            withMessage { mapBased(causeId to expectedMessage) }
            withStatus { dummy() }
        } }

        val response = generator.responseOf(Any())

        response.payload.shouldBeTypeOf<OperationError>()
            .apply { message shouldBe expectedMessage }
    }

    test("single error payload with defined status is created") {
        val status = 500
        val generator = RestX.respondTo<Any> { asOperationError {
            withMessage { dummy() }
            withStatus(status)
        } }

        val response = generator.responseOf(Any())

        response.status shouldBe HttpStatus(status)
    }

    test("generator of single error payloads for invalid request data errors is created") {
        class InvalidInput(val type: Source.Type, val location: String, val message: String)
        val expectedSource = Source.queryParam("queryParam1")
        val expectedMessage = "Invalid value in query param"
        val generator = RestX.respondTo<InvalidInput> { asRequestDataError {
            withMessage { generatedAs { context.message } }
            pointingInvalidValue { resolvedBy { cause -> cause.context.let { it.type.toSource(it.location) } } }
            withStatus { dummy() }
        } }

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
        val generator = RestX.respondTo<MultiExceptionFaultResult> { asContainerOf<Exception> {
            extractedAs { it.errors.asList() }
            eachRepresenting { operationError {
                withMessage { generatedAs { context.message!! } }
            } }
            withStatus(500)
        } }

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
        val compositeGenerator = RestX.compose { registeredByFaultType {
            register { generatorForExceptionFault }
            register { generatorForRuntimeExceptionFault }
            register { generatorForStringFault }
        } }

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
