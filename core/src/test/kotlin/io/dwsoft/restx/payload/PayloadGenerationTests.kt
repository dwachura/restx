package io.dwsoft.restx.payload

import io.dwsoft.restx.fault.FaultResult
import io.dwsoft.restx.dummy
import io.dwsoft.restx.mock
import io.dwsoft.restx.fault.FaultCauseProcessor
import io.dwsoft.restx.fault.MultipleCauseResolver
import io.dwsoft.restx.fault.SingleCauseResolver
import io.dwsoft.restx.fault.causeId
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.verify

class SingleErrorPayloadGeneratorTests : FunSpec({
    test("fault cause info resolver is called") {
        val resolver = mock<SingleCauseResolver<FaultResult>> {
            every { this@mock.invoke(any()) } returns dummy()
        }
        val generator = SingleErrorPayloadGenerator(resolver, dummy())
        val fault = FaultResult()

        generator.payloadOf(fault)

        verify { resolver(fault) }
    }

    test("fault cause info processor is called") {
        val cause = FaultResult().causeId("id")
        val processor = mock<FaultCauseProcessor<FaultResult>>()
        val generator = SingleErrorPayloadGenerator(
            mock {
                every { this@mock.invoke(any()) } returns cause
            },
            processor
        )

        generator.payloadOf(FaultResult())

        verify { processor.process(cause) }
    }
})

class MultiErrorPayloadGeneratorTests : FunSpec({
    test("fault cause info resolver is called") {
        val resolver = mock<MultipleCauseResolver<FaultResult>> {
            every { this@mock.invoke(any()) } returns dummy()
        }
        val generator = MultiErrorPayloadGenerator<FaultResult>(resolver, dummy())
        val fault = FaultResult()

        generator.payloadOf(fault)

        verify { resolver(fault) }
    }

    test("fault cause info processor is called for each cause resolved") {
        val causes = listOf(FaultResult().causeId("id"))
        val processor = mock<FaultCauseProcessor<FaultResult>>()
        val generator = MultiErrorPayloadGenerator<FaultResult>(
            mock {
                every { this@mock.invoke(any()) } returns causes
            },
            processor
        )

        generator.payloadOf(FaultResult())

        causes.forEach {
            verify { processor.process(it) }
        }
    }

    test("exception is thrown when no causes are resolved") {
        shouldThrow<NoCausesResolved> {
            MultiErrorPayloadGenerator<Any>({ emptyList() }, dummy()).payloadOf(FaultResult())
        }
    }

    test("single error payload is returned when single cause is resolved") {
        val expected = dummy<ApiError>()
        val generator = MultiErrorPayloadGenerator<FaultResult>(
            { listOf(it.causeId("id")) }, { expected }
        )

        val result = generator.payloadOf(FaultResult())

        result shouldBe expected
    }

    test("multi error payload is returned when multiple causes are resolved") {
        val expectedPayloadContent = listOf<ApiError>(dummy(), dummy())
        val generator = MultiErrorPayloadGenerator<FaultResult>(
            { listOf(it.causeId("id"), it.causeId("id")) },
            mock { every { this@mock.process(any()) } returnsMany expectedPayloadContent }
        )

        val result = generator.payloadOf(FaultResult())

        result shouldBe MultiErrorPayload(expectedPayloadContent)
    }
})