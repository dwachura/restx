package io.dwsoft.restx.core.response

import io.dwsoft.restx.core.dummy
import io.dwsoft.restx.core.mock
import io.dwsoft.restx.core.payload.ErrorPayloadGenerator
import io.dwsoft.restx.core.payload.ErrorResponsePayload
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.verify

class SimpleResponseGeneratorTests : FunSpec({
    test("payload generator is called") {
        val fault = Any()
        val generator = mock<ErrorPayloadGenerator<Any, *>> {
            every { payloadOf(fault) } returns dummy()
        }

        SimpleResponseGenerator(generator, dummy()).responseOf(fault)

        verify { generator.payloadOf(fault) }
    }

    test("status provider is called") {
        val generator = mock<ErrorPayloadGenerator<Any, *>> {
            every { payloadOf(any()) } returns dummy()
        }
        val statusProvider = mock<ResponseStatusProvider> {
            every { get() } returns dummy()
        }

        SimpleResponseGenerator(generator, statusProvider).responseOf(Any())

        verify { statusProvider.get() }
    }

    test("fault is converted to response") {
        val payload = dummy<ErrorResponsePayload>()
        val generator = SimpleResponseGenerator(
            mock<ErrorPayloadGenerator<Any, *>> {
                every { payloadOf(any()) } returns payload
            }
        ) { HttpStatus(500) }

        val response = generator.responseOf(Any())

        response shouldBe ErrorResponse(HttpStatus(500), payload)
    }
})
