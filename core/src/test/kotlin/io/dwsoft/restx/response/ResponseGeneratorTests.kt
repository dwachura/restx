package io.dwsoft.restx.response

import io.dwsoft.restx.fault.FaultResult
import io.dwsoft.restx.dummy
import io.dwsoft.restx.mock
import io.dwsoft.restx.payload.ErrorPayloadGenerator
import io.dwsoft.restx.payload.ErrorResponsePayload
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.verify

class ResponseGeneratorTests : FunSpec({
    test("payload generator is called") {
        val fault = FaultResult()
        val generator = mock<ErrorPayloadGenerator<FaultResult, *>> {
            every { payloadOf(fault) } returns dummy()
        }

        ResponseGenerator(generator, dummy()).responseOf(fault)

        verify { generator.payloadOf(fault) }
    }

    test("status provider is called") {
        val generator = mock<ErrorPayloadGenerator<FaultResult, *>> {
            every { payloadOf(any()) } returns dummy()
        }
        val statusProvider = mock<ResponseStatusProvider> {
            every { get() } returns dummy()
        }

        ResponseGenerator(generator, statusProvider).responseOf(FaultResult())

        verify { statusProvider.get() }
    }

    test("fault is converted to response") {
        val payload = dummy<ErrorResponsePayload>()
        val generator = ResponseGenerator(
            mock<ErrorPayloadGenerator<FaultResult, *>> {
                every { payloadOf(any()) } returns payload
            }
        ) { HttpStatus(500) }

        val response = generator.responseOf(FaultResult())

        response shouldBe ErrorResponse(HttpStatus(500), payload)
    }
})