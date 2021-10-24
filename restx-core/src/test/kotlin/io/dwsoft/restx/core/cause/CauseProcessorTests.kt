package io.dwsoft.restx.core.cause

import io.dwsoft.restx.InitBlock
import io.dwsoft.restx.core.cause.code.CauseCodeProvider
import io.dwsoft.restx.core.cause.code.CauseCodeProviders
import io.dwsoft.restx.core.cause.message.CauseMessageProvider
import io.dwsoft.restx.core.cause.message.CauseMessageProviders
import io.dwsoft.restx.core.dummy
import io.dwsoft.restx.core.mock
import io.dwsoft.restx.core.payload.OperationError
import io.dwsoft.restx.core.payload.RequestDataError
import io.dwsoft.restx.core.payload.Source
import io.kotest.assertions.assertSoftly
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.every
import io.mockk.verify
import io.dwsoft.restx.core.cause.OperationErrorProcessor.Builder.Config as OperationErrorProcessorBuilderConfig
import io.dwsoft.restx.core.cause.RequestDataErrorProcessor.Builder.Config as RequestDataErrorProcessorBuilderConfig

abstract class StandardCauseProcessorTestsBase(
    createProcessor: (CauseCodeProvider<Any>, CauseMessageProvider<Any>) -> CauseProcessor<Any>,
    additionalTestsInitBlock: InitBlock<FunSpec> = {}
) : FunSpec({
    test("code provider is called") {
        val fault = Any().causeId("")
        val codeProvider = mock<CauseCodeProvider<Any>>()

        createProcessor(codeProvider, dummy()).process(fault)

        verify { codeProvider.codeFor(fault) }
    }

    test("message provider is called") {
        val fault = Any().causeId("")
        val messageProvider = mock<CauseMessageProvider<Any>>()

        createProcessor(dummy(), messageProvider).process(fault)

        verify { messageProvider.messageFor(fault) }
    }

    test("exception is thrown in case of code provider failure") {
        val sut = createProcessor(
            mock { every { codeFor(any()) } throws RuntimeException() },
            dummy()
        )

        shouldThrow<CauseProcessingFailure> { sut.process(Any().causeId("")) }
    }

    test("exception is thrown in case of message provider failure") {
        val sut = createProcessor(
            { "code" },
            mock { every { messageFor(any()) } throws RuntimeException() }
        )

        shouldThrow<CauseProcessingFailure> { sut.process(Any().causeId("")) }
    }

    this.apply(additionalTestsInitBlock)
})

class OperationErrorProcessorTests : StandardCauseProcessorTestsBase(
    { codeProvider, messageProvider -> OperationErrorProcessor(codeProvider, messageProvider) },
    {
        test("payload with defined data is returned") {
            val fault = Any().causeId("")
            val code = "code"
            val message = "message"

            val payload = OperationErrorProcessor<Any>({ code }, { message }).process(fault)

            payload shouldBe OperationError(code, message)
        }
    }
)

class RequestDataErrorProcessorTests : StandardCauseProcessorTestsBase(
    { codeProvider, messageProvider -> RequestDataErrorProcessor(codeProvider, messageProvider, dummy()) },
    {
        test("data error source provider is called") {
            val fault = Any().causeId("")
            val dataErrorSourceProvider = mock<DataErrorSourceResolver<Any>>()

            RequestDataErrorProcessor(dummy(), dummy(), dataErrorSourceProvider)
                .process(fault)

            verify { dataErrorSourceProvider.sourceOf(fault) }
        }

        test("payload with defined data is returned") {
            val fault = Any().causeId("")
            val code = "code"
            val message = "message"
            val source = Source.queryParam("query")

            val payload = RequestDataErrorProcessor<Any>({ code }, { message }, { source }).process(fault)

            payload shouldBe RequestDataError(code, message, source)
        }

        test("exception is thrown in case of failure during data error source provisioning") {
            val failingDataErrorSourceProvider =
                mock<DataErrorSourceResolver<Any>> { every { sourceOf(any()) } throws RuntimeException() }

            shouldThrow<CauseProcessingFailure> {
                RequestDataErrorProcessor(dummy(), dummy(), failingDataErrorSourceProvider)
                    .process(Any().causeId(""))
            }
        }
    }
)

abstract class StandardCauseProcessorBuilderTestsBase(
    createConfig: (CauseCodeProviderFactoryBlock<Any>?, CauseMessageProviderFactoryBlock<Any>?) -> StandardConfig<Any>,
    buildProcessor: (StandardConfig<Any>) -> CauseProcessor<Any>,
    additionalTestsInitBlock: InitBlock<FunSpec> = {}
) : FunSpec({
    test("configuration without message provider factory throws exception") {
        val config = createConfig(dummy(), null)

        shouldThrow<IllegalArgumentException> { buildProcessor(config) }
            .message shouldContain "Message provider factory block not set"
    }

    test("configured code provider factory is called") {
        val factoryBlock = mock<AnyCauseCodeProviderFactoryBlock> {
            every { this@mock(any()) } returns dummy()
        }
        val config = createConfig(factoryBlock) { dummy() }

        buildProcessor(config)

        verify { factoryBlock(CauseCodeProviders) }
    }

    test("configured message provider factory is called") {
        val factoryBlock = mock<AnyCauseMessageProviderFactoryBlock> {
            every { this@mock(any()) } returns dummy()
        }
        val config = createConfig({ dummy() }, factoryBlock)

        buildProcessor(config)

        verify { factoryBlock(CauseMessageProviders) }
    }

    test("by default standard processor is configured with code provider based on fault cause id") {
        val defaultCodeProvider = createConfig(null) { dummy() }.causeCodeProviderFactoryBlock(CauseCodeProviders)
        val assertCodeProvidedEqualTo: (String) -> Unit = { defaultCodeProvider.codeFor(causeId(it)) shouldBe it }

        assertSoftly {
            assertCodeProvidedEqualTo("expected-id")
            assertCodeProvidedEqualTo("expected-id-2")
        }
    }

    this.apply(additionalTestsInitBlock)
})

private typealias AnyCauseCodeProviderFactoryBlock = CauseCodeProviderFactoryBlock<Any>
private typealias AnyCauseMessageProviderFactoryBlock = CauseMessageProviderFactoryBlock<Any>

class OperationErrorProcessorBuilderTests : StandardCauseProcessorBuilderTestsBase(
    { codeProviderFactory, messageProviderFactory ->
        OperationErrorProcessorBuilderConfig<Any>().apply {
            codeProviderFactory?.let { code(it) }
            messageProviderFactory?.let { message(it) }
        }
    },
    { OperationErrorProcessor.buildFrom(it as OperationErrorProcessorBuilderConfig<Any>) }
)

class RequestDataErrorProcessorBuilderTests : StandardCauseProcessorBuilderTestsBase(
    { codeProviderFactory, messageProviderFactory ->
        RequestDataErrorProcessorBuilderConfig<Any>().apply {
            codeProviderFactory?.let { code(it) }
            messageProviderFactory?.let { message(it) }
            invalidValue { dummy() }
        }
    },
    { RequestDataErrorProcessor.buildFrom(it as RequestDataErrorProcessorBuilderConfig<Any>) },
    {
        test("configuration without data error source provider factory throws exception") {
            shouldThrow<IllegalArgumentException> {
                RequestDataErrorProcessor.buildFrom(
                    RequestDataErrorProcessor.Builder.Config<Any>().apply {
                        code { dummy() }
                        message { dummy() }
                    }
                )
            }.message shouldContain "Data error source provider factory block not set"
        }

        test("configured data error source provider factory is called") {
            val factoryBlock = mock<AnyDataErrorSourceProviderFactoryBlock> {
                every { this@mock(any()) } returns dummy()
            }
            val config = RequestDataErrorProcessor.Builder.Config<Any>().apply {
                code { dummy() }
                message { dummy() }
                invalidValue(factoryBlock)
            }

            RequestDataErrorProcessor.buildFrom(config)

            verify { factoryBlock(DataErrorSourceResolvers) }
        }
    }
)

private typealias AnyDataErrorSourceProviderFactoryBlock = DataErrorSourceResolverFactoryBlock<Any>
