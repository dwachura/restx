package io.dwsoft.restx.fault.response

import io.dwsoft.restx.fault.dummy
import io.dwsoft.restx.fault.mock
import io.dwsoft.restx.fault.payload.ErrorPayloadGenerator
import io.dwsoft.restx.fault.payload.ErrorResponsePayload
import io.dwsoft.restx.fault.response.ResponseGenerator.Builder.Config
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.every
import io.mockk.verify

class ResponseGeneratorTests : FunSpec({
    test("payload generator is called") {
        val fault = Any()
        val generator = mock<ErrorPayloadGenerator<Any, *>> {
            every { payloadOf(fault) } returns dummy()
        }

        ResponseGenerator(generator, dummy()).responseOf(fault)

        verify { generator.payloadOf(fault) }
    }

    test("status provider is called") {
        val generator = mock<ErrorPayloadGenerator<Any, *>> {
            every { payloadOf(any()) } returns dummy()
        }
        val statusProvider = mock<ResponseStatusProvider> {
            every { get() } returns dummy()
        }

        ResponseGenerator(generator, statusProvider).responseOf(Any())

        verify { statusProvider.get() }
    }

    test("fault is converted to response") {
        val payload = dummy<ErrorResponsePayload>()
        val generator = ResponseGenerator(
            mock<ErrorPayloadGenerator<Any, *>> {
                every { payloadOf(any()) } returns payload
            }
        ) { HttpStatus(500) }

        val response = generator.responseOf(Any())

        response shouldBe ErrorResponse(HttpStatus(500), payload)
    }
})

class ResponseGeneratorBuilderTests : FunSpec({
    test("configuration without payload generator factory throws exception") {
        shouldThrow<IllegalStateException> {
            ResponseGenerator.buildFrom(
                Config<Any>().apply {
                    status { dummy() }
                }
            )
        }.message shouldContain "Payload generator factory must be provided"
    }

    test("configuration without status provider factory throws exception") {
        shouldThrow<IllegalStateException> {
            ResponseGenerator.buildFrom(
                Config<Any>().apply {
                    payloadOf { dummy() }
                }
            )
        }.message shouldContain "Status provider factory must be provided"
    }

    test("configured payload generator factory is called") {
        val factory = mock<AnyErrorPayloadGeneratorFactory> {
            every { this@mock(any()) } returns dummy()
        }
        val config = Config<Any>().apply {
            payloadOf(factory)
            status { dummy() }
        }

        ResponseGenerator.buildFrom(config)

        verify { factory(ErrorPayloadGenerator.Builders) }
    }

    test("configured status provider factory is called") {
        val factory = mock<ResponseStatusProviderFactory> {
            every { this@mock(any()) } returns dummy()
        }
        val config = Config<Any>().apply {
            payloadOf { dummy() }
            status(factory)
        }

        ResponseGenerator.buildFrom(config)

        verify { factory(ResponseStatusProviders) }
    }
})

private typealias AnyErrorPayloadGeneratorFactory = ErrorPayloadGeneratorFactory<Any>