package io.dwsoft.restx.fault.payload

import io.dwsoft.restx.InitBlock
import io.dwsoft.restx.fault.cause.CauseProcessor
import io.dwsoft.restx.fault.cause.CauseProcessors
import io.dwsoft.restx.fault.cause.CauseResolver
import io.dwsoft.restx.fault.cause.CauseResolvers
import io.dwsoft.restx.fault.cause.causeId
import io.dwsoft.restx.fault.cause.invoke
import io.dwsoft.restx.dummy
import io.dwsoft.restx.mock
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.every
import io.mockk.verify
import io.dwsoft.restx.fault.payload.MultiErrorPayloadGenerator.Builder.Config as MultiErrorPayloadGeneratorConfig
import io.dwsoft.restx.fault.payload.SingleErrorPayloadGenerator.Builder.Config as SingleErrorPayloadGeneratorConfig

class SingleErrorPayloadGeneratorTests : FunSpec({
    test("fault cause info resolver is called") {
        val resolver = mock<CauseResolver<Any>> {
            every { this@mock.causeOf(any()) } returns dummy()
        }
        val generator = SingleErrorPayloadGenerator(resolver, dummy())
        val fault = Any()

        generator.payloadOf(fault)

        verify { resolver(fault) }
    }

    test("fault cause info processor is called") {
        val cause = Any().causeId("id")
        val processor = mock<CauseProcessor<Any>>()
        val generator = SingleErrorPayloadGenerator(
            mock {
                every { this@mock.causeOf(any()) } returns cause
            },
            processor
        )

        generator.payloadOf(Any())

        verify { processor.process(cause) }
    }
})

class SingleErrorPayloadGeneratorBuilderTests : FunSpec({
    test("configuration without cause resolver factory throws exception") {
        shouldThrow<IllegalArgumentException> {
            SingleErrorPayloadGenerator.buildFrom(
                SingleErrorPayloadGeneratorConfig<Any>().apply {
                    processedAs { dummy() }
                }
            )
        }.message shouldContain "Cause resolver factory block not set"
    }

    test("configuration without cause processor factory throws exception") {
        shouldThrow<IllegalArgumentException> {
            SingleErrorPayloadGenerator.buildFrom(
                SingleErrorPayloadGeneratorConfig<Any>().apply {
                    identifiedBy { dummy() }
                }
            )
        }.message shouldContain "Cause processor factory block not set"
    }

    test("configured cause resolver factory is called") {
        val factoryBlock = mock<AnyCauseResolverFactoryBlock> {
            every { this@mock(any()) } returns dummy()
        }
        val config = SingleErrorPayloadGeneratorConfig<Any>().apply {
            identifiedBy(factoryBlock)
            processedAs { dummy() }
        }

        SingleErrorPayloadGenerator.buildFrom(config)

        verify { factoryBlock(CauseResolvers) }
    }

    test("configured cause processor factory is called") {
        val factoryBlock = mock<AnyCauseProcessorFactoryBlock> {
            every { this@mock(any()) } returns dummy()
        }
        val config = SingleErrorPayloadGeneratorConfig<Any>().apply {
            identifiedBy { dummy() }
            processedAs(factoryBlock)
        }

        SingleErrorPayloadGenerator.buildFrom(config)

        verify { factoryBlock(CauseProcessors) }
    }
})

class MultiErrorPayloadGeneratorTests : FunSpec({
    test("sub-errors extractor function is called") {
        val extractor = mock<SubErrorExtractor<Any, Any>>()
        val generator = MultiErrorPayloadGenerator(extractor, dummy())
        val fault = Any()

        runCatching { generator.payloadOf(fault) }

        verify { extractor(fault) }
    }

    test("sub-error payload generator is called for each sub-error extracted") {
        val subError1 = Any()
        val subError2 = Any()
        val subErrorPayloadGenerator = mock<SingleErrorPayloadGenerator<Any>> {
            every { this@mock.payloadOf(any()) } returns dummy()
        }
        val generator = MultiErrorPayloadGenerator(
            SubErrorExtractor { listOf(subError1, subError2) },
            subErrorPayloadGenerator
        )
        val fault = Any()

        generator.payloadOf(fault)

        verify { subErrorPayloadGenerator.payloadOf(subError1) }
        verify { subErrorPayloadGenerator.payloadOf(subError2) }
    }

    test("exception is thrown when no sub-errors are extracted") {
        val generator = MultiErrorPayloadGenerator<Any, Any>({ emptyList() }, dummy())

        shouldThrow<NoSubErrorsExtracted> { generator.payloadOf(Any()) }
    }

    test("multi error payload containing errors generated for sub-errors is returned") {
        val expectedPayloadContent = listOf<SingleErrorPayload>(dummy(), dummy())
        val subErrorPayloadGenerator = mock<SingleErrorPayloadGenerator<Any>> {
            every { this@mock.payloadOf(any()) } returnsMany expectedPayloadContent
        }
        val generator = MultiErrorPayloadGenerator(
            SubErrorExtractor { listOf(dummy(), dummy()) },
            subErrorPayloadGenerator
        )

        val result = generator.payloadOf(Any())

        result shouldBe MultiErrorPayload(expectedPayloadContent)
    }
})

class MultipleErrorPayloadGeneratorBuilderTests : FunSpec({
    test("configuration without sub-error extractor throws exception") {
        shouldThrow<IllegalArgumentException> {
            MultiErrorPayloadGenerator.buildFrom(
                MultiErrorPayloadGeneratorConfig<Any, Any>().apply {
                    handledBy(dummy())
                }
            )
        }.message shouldContain "Sub-error extractor must be provided"
    }

    test("configuration without sub-error payload generator throws exception") {
        shouldThrow<IllegalArgumentException> {
            MultiErrorPayloadGenerator.buildFrom(
                MultiErrorPayloadGeneratorConfig<Any, Any>().apply {
                    extractedAs { dummy() }
                }
            )
        }.message shouldContain "Sub-error payload generator must be provided"
    }

    test("sub-error payload generator init block is called") {
        val initBlock = mock<InitBlock<SingleErrorPayloadGenerator.Builder.Config<Any>>>()

        runCatching { MultiErrorPayloadGeneratorConfig<Any, Any>().whichAre(initBlock) }

        verify { initBlock(any()) }
    }
})

private typealias AnyCauseResolverFactoryBlock = CauseResolverFactoryBlock<Any>
private typealias AnyCauseProcessorFactoryBlock = CauseProcessorFactoryBlock<Any>
