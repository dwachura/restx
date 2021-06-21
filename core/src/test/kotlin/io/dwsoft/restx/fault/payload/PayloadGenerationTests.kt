package io.dwsoft.restx.fault.payload

import io.dwsoft.restx.fault.cause.CauseProcessors
import io.dwsoft.restx.fault.cause.CauseResolvers
import io.dwsoft.restx.fault.cause.CauseProcessor
import io.dwsoft.restx.fault.cause.MultipleCauseResolver
import io.dwsoft.restx.fault.cause.SingleCauseResolver
import io.dwsoft.restx.fault.cause.causeId
import io.dwsoft.restx.fault.cause.invoke
import io.dwsoft.restx.fault.dummy
import io.dwsoft.restx.fault.mock
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
        val resolver = mock<SingleCauseResolver<Any>> {
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
        shouldThrow<IllegalStateException> {
            SingleErrorPayloadGenerator.buildFrom(
                SingleErrorPayloadGeneratorConfig<Any>().apply {
                    processedBy { dummy() }
                }
            )
        }.message shouldContain "Cause resolver factory must be provided"
    }

    test("configuration without cause processor factory throws exception") {
        shouldThrow<IllegalStateException> {
            SingleErrorPayloadGenerator.buildFrom(
                SingleErrorPayloadGeneratorConfig<Any>().apply {
                    identifiedBy { dummy() }
                }
            )
        }.message shouldContain "Cause processor factory must be provided"
    }

    test("configured cause resolver factory is called") {
        val factory = mock<AnySingleCauseResolverFactory> {
            every { this@mock(any()) } returns dummy()
        }
        val config = SingleErrorPayloadGeneratorConfig<Any>().apply {
            identifiedBy(factory)
            processedBy { dummy() }
        }

        SingleErrorPayloadGenerator.buildFrom(config)

        verify { factory(CauseResolvers) }
    }

    test("configured cause processor factory is called") {
        val factory = mock<AnyCauseProcessorFactory> {
            every { this@mock(any()) } returns dummy()
        }
        val config = SingleErrorPayloadGeneratorConfig<Any>().apply {
            identifiedBy { dummy() }
            processedBy(factory)
        }

        SingleErrorPayloadGenerator.buildFrom(config)

        verify { factory(CauseProcessors) }
    }
})

class MultiErrorPayloadGeneratorTests : FunSpec({
    test("fault cause info resolver is called") {
        val resolver = mock<MultipleCauseResolver<Any>> {
            every { this@mock.causesOf(any()) } returns dummy()
        }
        val generator = MultiErrorPayloadGenerator(resolver, dummy())
        val fault = Any()

        generator.payloadOf(fault)

        verify { resolver(fault) }
    }

    test("fault cause info processor is called for each cause resolved") {
        val causes = listOf(Any().causeId("id"))
        val processor = mock<CauseProcessor<Any>>()
        val generator = MultiErrorPayloadGenerator(
            mock {
                every { this@mock.causesOf(any()) } returns causes
            },
            processor
        )

        generator.payloadOf(Any())

        causes.forEach {
            verify { processor.process(it) }
        }
    }

    test("exception is thrown when no causes are resolved") {
        shouldThrow<NoCausesResolved> {
            MultiErrorPayloadGenerator<Any>({ emptyList() }, dummy()).payloadOf(Any())
        }
    }

    test("single error payload is returned when single cause is resolved") {
        val expected = dummy<ApiError>()
        val generator = MultiErrorPayloadGenerator<Any>(
            { listOf(it.causeId("id")) }, { expected }
        )

        val result = generator.payloadOf(Any())

        result shouldBe expected
    }

    test("multi error payload is returned when multiple causes are resolved") {
        val expectedPayloadContent = listOf<ApiError>(dummy(), dummy())
        val generator = MultiErrorPayloadGenerator<Any>(
            { listOf(it.causeId("id"), it.causeId("id")) },
            mock { every { this@mock.process(any()) } returnsMany expectedPayloadContent }
        )

        val result = generator.payloadOf(Any())

        result shouldBe MultiErrorPayload(expectedPayloadContent)
    }
})

class MultipleErrorPayloadGeneratorBuilderTests : FunSpec({
    test("configuration without cause resolver factory throws exception") {
        shouldThrow<IllegalStateException> {
            MultiErrorPayloadGenerator.buildFrom(
                MultiErrorPayloadGeneratorConfig<Any>().apply {
                    processedBy { dummy() }
                }
            )
        }.message shouldContain "Cause resolver factory must be provided"
    }

    test("configuration without cause processor factory throws exception") {
        shouldThrow<IllegalStateException> {
            MultiErrorPayloadGenerator.buildFrom(
                MultiErrorPayloadGeneratorConfig<Any>().apply {
                    identifiedBy { dummy() }
                }
            )
        }.message shouldContain "Cause processor factory must be provided"
    }

    test("configured cause resolver factory is called") {
        val factory = mock<AnyMultipleCauseResolverFactory> {
            every { this@mock(any()) } returns dummy()
        }
        val config = MultiErrorPayloadGeneratorConfig<Any>().apply {
            identifiedBy(factory)
            processedBy { dummy() }
        }

        MultiErrorPayloadGenerator.buildFrom(config)

        verify { factory(CauseResolvers) }
    }

    test("configured cause processor factory is called") {
        val factory = mock<AnyCauseProcessorFactory> {
            every { this@mock(any()) } returns dummy()
        }
        val config = MultiErrorPayloadGeneratorConfig<Any>().apply {
            identifiedBy { dummy() }
            processedBy(factory)
        }

        MultiErrorPayloadGenerator.buildFrom(config)

        verify { factory(CauseProcessors) }
    }
})

private typealias AnySingleCauseResolverFactory = SingleCauseResolverFactory<Any>
private typealias AnyMultipleCauseResolverFactory = MultipleCauseResolverFactory<Any>
private typealias AnyCauseProcessorFactory = CauseProcessorFactory<Any>