package io.dwsoft.restx.fault.response

import io.dwsoft.restx.fault.dummy
import io.dwsoft.restx.fault.mock
import io.dwsoft.restx.fault.payload.ErrorPayloadGenerator
import io.dwsoft.restx.fault.payload.ErrorResponsePayload
import io.dwsoft.restx.fault.response.SimpleResponseGenerator.Builder.Config
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
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

class SimpleResponseGeneratorBuilderTests : FunSpec({
    test("configuration without payload generator factory throws exception") {
        shouldThrow<IllegalArgumentException> {
            SimpleResponseGenerator.buildFrom(
                Config<Any>().apply {
                    status { dummy() }
                }
            )
        }.message shouldContain "Payload generator factory block not set"
    }

    test("configuration without status provider factory throws exception") {
        shouldThrow<IllegalArgumentException> {
            SimpleResponseGenerator.buildFrom(
                Config<Any>().apply {
                    payload { dummy() }
                }
            )
        }.message shouldContain "Status provider factory block not set"
    }

    test("configured payload generator factory is called") {
        val factoryBlock = mock<AnyErrorPayloadGeneratorFactoryBlock> {
            every { this@mock(any()) } returns dummy()
        }
        val config = Config<Any>().apply {
            payload(factoryBlock)
            status { dummy() }
        }

        SimpleResponseGenerator.buildFrom(config)

        verify { factoryBlock(any()) }
    }

    test("configured status provider factory is called") {
        val factoryBlock = mock<ResponseStatusProviderFactoryBlock> {
            every { this@mock(any()) } returns dummy()
        }
        val config = Config<Any>().apply {
            payload { dummy() }
            status(factoryBlock)
        }

        SimpleResponseGenerator.buildFrom(config)

        verify { factoryBlock(ResponseStatusProviders) }
    }
})

private typealias AnyErrorPayloadGeneratorFactoryBlock = ErrorPayloadGeneratorFactoryBlock<Any>
