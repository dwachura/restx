package io.dwsoft.restx.core.payload

import io.dwsoft.restx.core.cause.CauseProcessor
import io.dwsoft.restx.core.cause.CauseResolver
import io.dwsoft.restx.core.cause.causeId
import io.dwsoft.restx.core.cause.invoke
import io.dwsoft.restx.core.dummy
import io.dwsoft.restx.core.mock
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.verify

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
