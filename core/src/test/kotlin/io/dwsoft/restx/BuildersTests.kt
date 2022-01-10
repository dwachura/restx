package io.dwsoft.restx

import io.dwsoft.restx.core.cause.CauseProcessor
import io.dwsoft.restx.core.cause.CauseResolver
import io.dwsoft.restx.core.cause.DataErrorSourceResolver
import io.dwsoft.restx.core.cause.causeId
import io.dwsoft.restx.core.cause.code.CodeResolver
import io.dwsoft.restx.core.cause.message.MessageResolver
import io.dwsoft.restx.core.dummy
import io.dwsoft.restx.core.mock
import io.dwsoft.restx.core.response.ResponseStatusProvider
import io.kotest.assertions.assertSoftly
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.every
import io.mockk.verify

class SimpleResponseGeneratorBuilderTests : FunSpec({
    test("configuration without payload generator factory throws exception") {
        shouldThrow<IllegalArgumentException> {
            SimpleResponseGeneratorBuilder.buildFrom(
                SimpleResponseGeneratorBuilder.Config<Any>().apply {
                    status { dummy() }
                }
            )
        }.message shouldContain "Payload generator factory block not set"
    }

    test("configuration without status provider factory throws exception") {
        shouldThrow<IllegalArgumentException> {
            SimpleResponseGeneratorBuilder.buildFrom(
                SimpleResponseGeneratorBuilder.Config<Any>().apply {
                    payload { dummy() }
                }
            )
        }.message shouldContain "Status provider factory block not set"
    }

    test("configured payload generator factory is called") {
        val factoryBlock = mock<AnyErrorPayloadGeneratorFactoryBlock> {
            every { this@mock(any()) } returns dummy()
        }
        val config = SimpleResponseGeneratorBuilder.Config<Any>().apply {
            payload(factoryBlock)
            status { dummy() }
        }

        SimpleResponseGeneratorBuilder.buildFrom(config)

        verify { factoryBlock(any()) }
    }

    test("configured status provider factory is called") {
        val factoryBlock = mock<ResponseStatusProviderFactoryBlock> {
            every { this@mock(any()) } returns dummy()
        }
        val config = SimpleResponseGeneratorBuilder.Config<Any>().apply {
            payload { dummy() }
            status(factoryBlock)
        }

        SimpleResponseGeneratorBuilder.buildFrom(config)

        verify { factoryBlock(ResponseStatusProvider.Factories) }
    }
})

private typealias AnyErrorPayloadGeneratorFactoryBlock = ErrorPayloadGeneratorFactoryBlock<Any>

class SingleErrorPayloadGeneratorBuilderTests : FunSpec({
    test("configuration without cause processor factory throws exception") {
        shouldThrow<IllegalArgumentException> {
            SingleErrorPayloadGeneratorBuilder.buildFrom(
                SingleErrorPayloadGeneratorBuilder.Config<Any>().apply {
                    identifiedBy { dummy() }
                }
            )
        }.message shouldContain "Cause processor factory block not set"
    }

    test("configured cause resolver factory is called") {
        val factoryBlock = mock<AnyCauseResolverFactoryBlock> {
            every { this@mock(any()) } returns dummy()
        }
        val config = SingleErrorPayloadGeneratorBuilder.Config<Any>().apply {
            identifiedBy(factoryBlock)
            processedAs { dummy() }
        }

        SingleErrorPayloadGeneratorBuilder.buildFrom(config)

        verify { factoryBlock(CauseResolver.Factories) }
    }

    test("by default generator is configured with cause resolver identifying fault by its type") {
        val defaultCauseResolver = SingleErrorPayloadGeneratorBuilder.Config<Any>()
            .causeResolverFactoryBlock(CauseResolver.Factories)

        assertSoftly {
            defaultCauseResolver.causeOf(RuntimeException()).id shouldBe RuntimeException::class.qualifiedName
            defaultCauseResolver.causeOf("abcd").id shouldBe String::class.qualifiedName
        }
    }

    test("configured cause processor factory is called") {
        val factoryBlock = mock<AnyCauseProcessorFactoryBlock> {
            every { this@mock(any()) } returns dummy()
        }
        val config = SingleErrorPayloadGeneratorBuilder.Config<Any>().apply {
            identifiedBy { dummy() }
            processedAs(factoryBlock)
        }

        SingleErrorPayloadGeneratorBuilder.buildFrom(config)

        verify { factoryBlock(CauseProcessors) }
    }
})

private typealias AnyCauseResolverFactoryBlock = CauseResolverFactoryBlock<Any>
private typealias AnyCauseProcessorFactoryBlock = CauseProcessorFactoryBlock<Any>

class MultipleErrorPayloadGeneratorBuilderTests : FunSpec({
    test("configuration without sub-error extractor throws exception") {
        shouldThrow<IllegalArgumentException> {
            MultiErrorPayloadGeneratorBuilder.buildFrom(
                MultiErrorPayloadGeneratorBuilder.Config<Any, Any>().apply {
                    handledBy(dummy())
                }
            )
        }.message shouldContain "Sub-error extractor must be provided"
    }

    test("configuration without sub-error payload generator throws exception") {
        shouldThrow<IllegalArgumentException> {
            MultiErrorPayloadGeneratorBuilder.buildFrom(
                MultiErrorPayloadGeneratorBuilder.Config<Any, Any>().apply {
                    extractedAs { dummy() }
                }
            )
        }.message shouldContain "Sub-error payload generator must be provided"
    }

    test("sub-error payload generator init block is called") {
        val initBlock = mock<InitBlock<SingleErrorPayloadGeneratorBuilder.Config<Any>>>()

        runCatching { MultiErrorPayloadGeneratorBuilder.Config<Any, Any>().whichAre(initBlock) }

        verify { initBlock(any()) }
    }
})

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

        verify { factoryBlock(CodeResolver.Factories) }
    }

    test("configured message resolver factory is called") {
        val factoryBlock = mock<AnyMessageResolverFactoryBlock> {
            every { this@mock(any()) } returns dummy()
        }
        val config = createConfig({ dummy() }, factoryBlock)

        buildProcessor(config)

        verify { factoryBlock(MessageResolver.Factories) }
    }

    test("by default standard processor is configured with code resolver based on fault cause id") {
        val defaultCodeResolver = createConfig(null) { dummy() }.codeResolverFactoryBlock(CodeResolver.Factories)
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
        OperationErrorProcessorBuilder.Config<Any>().apply {
            codeResolverFactory?.let { code(it) }
            messageResolverFactory?.let { message(it) }
        }
    },
    { OperationErrorProcessorBuilder.buildFrom(it as OperationErrorProcessorBuilder.Config<Any>) }
)

class RequestDataErrorProcessorBuilderTests : StandardCauseProcessorBuilderTestsBase(
    { codeResolverFactory, messageResolverFactory ->
        RequestDataErrorProcessorBuilder.Config<Any>().apply {
            codeResolverFactory?.let { code(it) }
            messageResolverFactory?.let { message(it) }
            invalidValue { dummy() }
        }
    },
    { RequestDataErrorProcessorBuilder.buildFrom(it as RequestDataErrorProcessorBuilder.Config<Any>) },
    {
        test("configuration without data error source resolver factory throws exception") {
            shouldThrow<IllegalArgumentException> {
                RequestDataErrorProcessorBuilder.buildFrom(
                    RequestDataErrorProcessorBuilder.Config<Any>().apply {
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
            val config = RequestDataErrorProcessorBuilder.Config<Any>().apply {
                code { dummy() }
                message { dummy() }
                invalidValue(factoryBlock)
            }

            RequestDataErrorProcessorBuilder.buildFrom(config)

            verify { factoryBlock(DataErrorSourceResolver.Factories) }
        }
    }
)

private typealias AnyDataErrorSourceProviderFactoryBlock = DataErrorSourceResolverFactoryBlock<Any>
