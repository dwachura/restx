package io.dwsoft.restx.core.response

import io.dwsoft.restx.core.dummy
import io.dwsoft.restx.core.mock
import io.dwsoft.restx.core.payload.ErrorPayloadGenerator
import io.dwsoft.restx.core.payload.ErrorResponsePayload
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContainInOrder
import io.kotest.matchers.types.shouldBeSameInstanceAs
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

class CompositeResponseGeneratorTests : FunSpec({
    test("sub-generator registry is called") {
        val generatorRegistry = mock<ResponseGeneratorRegistry>()
        val fault = Any()

        CompositeResponseGenerator(generatorRegistry).responseOf(fault)

        verify { generatorRegistry.searchFor(fault) }
    }

    test("found sub-generator is called") {
        val generator = mock<ResponseGenerator<Any>>()
        val fault = Any()

        CompositeResponseGenerator(
            mock { every { searchFor(any()) } returns generator }
        ).responseOf(fault)

        verify { generator.responseOf(fault) }
    }

    test("exception is thrown when no sub-generators found") {
        val fault = Any()

        shouldThrow<NoSubGeneratorFound> {
            CompositeResponseGenerator(
                mock { every { searchFor(any()) } returns null }
            ).responseOf(fault)
        }.message.shouldContainInOrder("No sub-generator found for fault", fault.toString())
    }

    test("sub-generator result is returned") {
        val expectedResult = dummy<ErrorResponse>()
        val generator = mock<ResponseGenerator<Any>> {
            every { responseOf(any()) } returns expectedResult
        }

        val result = CompositeResponseGenerator(mock {
            every { searchFor(any()) } returns generator
        }).responseOf(Any())

        result shouldBeSameInstanceAs expectedResult
    }
})
