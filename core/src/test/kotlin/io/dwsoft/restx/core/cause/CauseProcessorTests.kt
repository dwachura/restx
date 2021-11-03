package io.dwsoft.restx.core.cause

import io.dwsoft.restx.InitBlock
import io.dwsoft.restx.core.cause.code.CodeResolver
import io.dwsoft.restx.core.cause.code.CodeResolvers
import io.dwsoft.restx.core.cause.message.MessageResolver
import io.dwsoft.restx.core.cause.message.MessageResolvers
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
    createProcessor: (CodeResolver<Any>, MessageResolver<Any>) -> CauseProcessor<Any>,
    additionalTestsInitBlock: InitBlock<FunSpec> = {}
) : FunSpec({
    test("code resolver is called") {
        val fault = Any().causeId("")
        val codeResolver = mock<CodeResolver<Any>>()

        createProcessor(codeResolver, dummy()).process(fault)

        verify { codeResolver.codeFor(fault) }
    }

    test("message resolver is called") {
        val fault = Any().causeId("")
        val messageResolver = mock<MessageResolver<Any>>()

        createProcessor(dummy(), messageResolver).process(fault)

        verify { messageResolver.messageFor(fault) }
    }

    test("exception is thrown in case of code resolver failure") {
        val sut = createProcessor(
            mock { every { codeFor(any()) } throws RuntimeException() },
            dummy()
        )

        shouldThrow<CauseProcessingFailure> { sut.process(Any().causeId("")) }
    }

    test("exception is thrown in case of message resolver failure") {
        val sut = createProcessor(
            { "code" },
            mock { every { messageFor(any()) } throws RuntimeException() }
        )

        shouldThrow<CauseProcessingFailure> { sut.process(Any().causeId("")) }
    }

    this.apply(additionalTestsInitBlock)
})

class OperationErrorProcessorTests : StandardCauseProcessorTestsBase(
    { codeResolver, messageResolver -> OperationErrorProcessor(codeResolver, messageResolver) },
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
    { codeResolver, messageResolver -> RequestDataErrorProcessor(codeResolver, messageResolver, dummy()) },
    {
        test("data error source resolver is called") {
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

        test("exception is thrown in case data error source cannot be resolved") {
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
    createConfig: (CodeResolverFactoryBlock<Any>?, MessageResolverFactoryBlock<Any>?) -> StandardConfig<Any>,
    buildProcessor: (StandardConfig<Any>) -> CauseProcessor<Any>,
    additionalTestsInitBlock: InitBlock<FunSpec> = {}
) : FunSpec({
    test("configuration without message resolver factory throws exception") {
        val config = createConfig(dummy(), null)

        shouldThrow<IllegalArgumentException> { buildProcessor(config) }
            .message shouldContain "Message resolver factory block not set"
    }

    test("configured code resolver factory is called") {
        val factoryBlock = mock<AnyCodeResolverFactoryBlock> {
            every { this@mock(any()) } returns dummy()
        }
        val config = createConfig(factoryBlock) { dummy() }

        buildProcessor(config)

        verify { factoryBlock(CodeResolvers) }
    }

    test("configured message resolver factory is called") {
        val factoryBlock = mock<AnyMessageResolverFactoryBlock> {
            every { this@mock(any()) } returns dummy()
        }
        val config = createConfig({ dummy() }, factoryBlock)

        buildProcessor(config)

        verify { factoryBlock(MessageResolvers) }
    }

    test("by default standard processor is configured with code resolver based on fault cause id") {
        val defaultCodeResolver = createConfig(null) { dummy() }.codeResolverFactoryBlock(CodeResolvers)
        val assertResolvedCodeIsEqualTo: (String) -> Unit = { defaultCodeResolver.codeFor(causeId(it)) shouldBe it }

        assertSoftly {
            assertResolvedCodeIsEqualTo("expected-id")
            assertResolvedCodeIsEqualTo("expected-id-2")
        }
    }

    this.apply(additionalTestsInitBlock)
})

private typealias AnyCodeResolverFactoryBlock = CodeResolverFactoryBlock<Any>
private typealias AnyMessageResolverFactoryBlock = MessageResolverFactoryBlock<Any>

class OperationErrorProcessorBuilderTests : StandardCauseProcessorBuilderTestsBase(
    { codeResolverFactory, messageResolverFactory ->
        OperationErrorProcessorBuilderConfig<Any>().apply {
            codeResolverFactory?.let { code(it) }
            messageResolverFactory?.let { message(it) }
        }
    },
    { OperationErrorProcessor.buildFrom(it as OperationErrorProcessorBuilderConfig<Any>) }
)

class RequestDataErrorProcessorBuilderTests : StandardCauseProcessorBuilderTestsBase(
    { codeResolverFactory, messageResolverFactory ->
        RequestDataErrorProcessorBuilderConfig<Any>().apply {
            codeResolverFactory?.let { code(it) }
            messageResolverFactory?.let { message(it) }
            invalidValue { dummy() }
        }
    },
    { RequestDataErrorProcessor.buildFrom(it as RequestDataErrorProcessorBuilderConfig<Any>) },
    {
        test("configuration without data error source resolver factory throws exception") {
            shouldThrow<IllegalArgumentException> {
                RequestDataErrorProcessor.buildFrom(
                    RequestDataErrorProcessor.Builder.Config<Any>().apply {
                        code { dummy() }
                        message { dummy() }
                    }
                )
            }.message shouldContain "Data error source resolver factory block not set"
        }

        test("configured data error source resolver factory is called") {
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
