package io.dwsoft.restx.fault.cause

import io.dwsoft.restx.fault.cause.StandardCauseProcessor.Builder.Config
import io.dwsoft.restx.fault.cause.code.CauseCodeProviders
import io.dwsoft.restx.fault.cause.code.CauseCodeProvider
import io.dwsoft.restx.fault.cause.message.CauseMessageProvider
import io.dwsoft.restx.fault.cause.message.CauseMessageProviders
import io.dwsoft.restx.fault.dummy
import io.dwsoft.restx.fault.mock
import io.dwsoft.restx.fault.payload.ApiError
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.every
import io.mockk.verify

class StandardCauseProcessorTests : FunSpec({
    test("code provider should be called") {
        val fault = Any().causeId("")
        val codeProvider = mock<CauseCodeProvider<Any>>()

        StandardCauseProcessor(codeProvider, dummy()).process(fault)

        verify { codeProvider.codeFor(fault) }
    }

    test("message provider should be called") {
        val fault = Any().causeId("")
        val messageProvider = mock<CauseMessageProvider<Any>>()

        StandardCauseProcessor(dummy(), messageProvider).process(fault)

        verify { messageProvider.messageFor(fault) }
    }

    test("error with defined data is returned") {
        val fault = Any().causeId("")
        val expectedError = ApiError("code", "message")

        val error = StandardCauseProcessor<Any>(
            { expectedError.code }, { expectedError.message }
        ).process(fault)

        error shouldBe expectedError
    }

    test("exception is thrown in case of code provider failure") {
        val sut = StandardCauseProcessor<Any>(
            mock { every { codeFor(any()) } throws RuntimeException() },
            dummy()
        )

        shouldThrow<CauseProcessingFailed> { sut.process(Any().causeId("")) }
    }

    test("exception is thrown in case of message provider failure") {
        val sut = StandardCauseProcessor<Any>({ "code" },
            mock { every { messageFor(any()) } throws RuntimeException() })

        shouldThrow<CauseProcessingFailed> { sut.process(Any().causeId("")) }
    }
})

class StandardCauseProcessorBuilderTests : FunSpec({
    test("configuration without message provider factory throws exception") {
        shouldThrow<IllegalStateException> {
            StandardCauseProcessor.buildFrom(
                Config<Any>().apply { code { dummy() } }
            )
        }.message shouldContain "Message provider factory block not set"
    }

    test("configured code provider factory is called") {
        val factoryBlock = mock<AnyCauseCodeProviderFactoryBlock> {
            every { this@mock(any()) } returns dummy()
        }
        val config = Config<Any>().apply {
            code(factoryBlock)
            message { dummy() }
        }

        StandardCauseProcessor.buildFrom(config)

        verify { factoryBlock(CauseCodeProviders) }
    }

    test("configured message provider factory is called") {
        val factoryBlock = mock<AnyCauseMessageProviderFactoryBlock> {
            every { this@mock(any()) } returns dummy()
        }
        val config = Config<Any>().apply {
            code { dummy() }
            message(factoryBlock)
        }

        StandardCauseProcessor.buildFrom(config)

        verify { factoryBlock(CauseMessageProviders) }
    }

    test("by default standard processor is configured with code provider based on fault id") {
        val expectedId = "expected-id"
        val processor = StandardCauseProcessor.buildFrom(
            Config<Any>().apply { message { dummy() } }
        )

        val result = processor.process(causeId(expectedId))

        result.code shouldBe expectedId
    }
})

private typealias AnyCauseCodeProviderFactoryBlock = CauseCodeProviderFactoryBlock<Any>
private typealias AnyCauseMessageProviderFactoryBlock = CauseMessageProviderFactoryBlock<Any>