package io.dwsoft.restx.core.payload

import io.dwsoft.restx.InitBlock
import io.dwsoft.restx.core.cause.CauseResolver
import io.dwsoft.restx.core.cause.CauseResolvingException
import io.dwsoft.restx.core.cause.DataErrorSourceResolvingException
import io.dwsoft.restx.core.cause.causeKey
import io.dwsoft.restx.core.cause.code.CodeResolver
import io.dwsoft.restx.core.cause.code.CodeResolvingException
import io.dwsoft.restx.core.cause.message.MessageResolver
import io.dwsoft.restx.core.dummy
import io.dwsoft.restx.core.mock
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.throwable.shouldHaveCause
import io.mockk.every
import io.mockk.verify

private typealias SingleErrorGeneratorFactory =
    (CauseResolver<Any>, CodeResolver<Any>, MessageResolver<Any>) -> SingleErrorPayloadGenerator<Any>

abstract class SingleErrorPayloadGeneratorTestsBase<R : SingleErrorPayload>(
    createGenerator: SingleErrorGeneratorFactory,
    additionalTestsInitBlock: InitBlock<FunSpec> = {}
) : FunSpec({
    test("exception is thrown in case of cause resolver failure") {
        val exCause = CauseResolvingException("")
        val sut = createGenerator(
            mock { every { causeOf(any()) } throws exCause },
            dummy(),
            dummy()
        )

        shouldThrow<PayloadGenerationException> { sut.payloadOf(Any().causeKey("")) }
            .shouldHaveCause { it shouldBe exCause }
    }

    test("exception is thrown in case of code resolver failure") {
        val exCause = CodeResolvingException("")
        val sut = createGenerator(
            dummy(),
            mock { every { codeFor(any()) } throws exCause },
            dummy()
        )

        shouldThrow<PayloadGenerationException> { sut.payloadOf(Any().causeKey("")) }
            .shouldHaveCause { it shouldBe exCause }
    }

    test("exception is thrown in case of message resolver failure") {
        val exCause = CodeResolvingException("")
        val sut = createGenerator(
            dummy(),
            dummy(),
            mock { every { messageFor(any()) } throws exCause }
        )

        shouldThrow<PayloadGenerationException> { sut.payloadOf(Any().causeKey("")) }
            .shouldHaveCause { it shouldBe exCause }
    }

    this.apply(additionalTestsInitBlock)
})

class OperationErrorPayloadGeneratorTests : SingleErrorPayloadGeneratorTestsBase<OperationError>(
    { causeResolver, codeResolver, messageResolver ->
        OperationErrorPayloadGenerator(causeResolver, codeResolver, messageResolver)
    },
    {
        test("payload with defined data is returned") {
            val cause = Any().causeKey("")
            val code = "code"
            val message = "message".asMessage()
            val generator = OperationErrorPayloadGenerator<Any>({ cause }, { code }, { message })

            val payload = generator.payloadOf(Any())

            payload shouldBe OperationError(code, message)
        }
    }
)

class RequestDataErrorPayloadGeneratorTests : SingleErrorPayloadGeneratorTestsBase<RequestDataError>(
    { causeResolver, codeResolver, messageResolver ->
        RequestDataErrorPayloadGenerator(causeResolver, codeResolver, messageResolver, dummy())
    },
    {
        test("payload with defined data is returned") {
            val cause = Any().causeKey("")
            val code = "code"
            val message = "message".asMessage()
            val source = RequestDataError.Source.query("query")
            val generator = RequestDataErrorPayloadGenerator<Any>({ cause }, { code }, { message }, { source })

            val payload = generator.payloadOf(Any())

            payload shouldBe RequestDataError(code, message, source)
        }

        test("exception is thrown in case data error source cannot be resolved") {
            val exCause = DataErrorSourceResolvingException()
            val generator = RequestDataErrorPayloadGenerator<Any>(
                dummy(),
                dummy(),
                dummy(),
                mock { every { sourceOf(any()) } throws exCause }
            )

            shouldThrow<PayloadGenerationException> { generator.payloadOf(Any()) }
                .shouldHaveCause { it shouldBe exCause }
        }
    }
)

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

        shouldThrow<PayloadGenerationException> { generator.payloadOf(Any()) }
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
