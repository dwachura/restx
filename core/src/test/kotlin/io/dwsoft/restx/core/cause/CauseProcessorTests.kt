package io.dwsoft.restx.core.cause

import io.dwsoft.restx.InitBlock
import io.dwsoft.restx.core.cause.code.CodeResolver
import io.dwsoft.restx.core.cause.message.MessageResolver
import io.dwsoft.restx.core.dummy
import io.dwsoft.restx.core.mock
import io.dwsoft.restx.core.payload.OperationError
import io.dwsoft.restx.core.payload.RequestDataError
import io.dwsoft.restx.core.payload.RequestDataError.Source
import io.dwsoft.restx.core.payload.asMessage
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.verify

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

        shouldThrow<CauseProcessingException> { sut.process(Any().causeId("")) }
    }

    test("exception is thrown in case of message resolver failure") {
        val sut = createProcessor(
            { "code" },
            mock { every { messageFor(any()) } throws RuntimeException() }
        )

        shouldThrow<CauseProcessingException> { sut.process(Any().causeId("")) }
    }

    this.apply(additionalTestsInitBlock)
})

class OperationErrorProcessorTests : StandardCauseProcessorTestsBase(
    { codeResolver, messageResolver -> OperationErrorProcessor(codeResolver, messageResolver) },
    {
        test("payload with defined data is returned") {
            val fault = Any().causeId("")
            val code = "code"
            val message = "message".asMessage()

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
            val message = "message".asMessage()
            val source = Source.query("query")

            val payload = RequestDataErrorProcessor<Any>({ code }, { message }, { source }).process(fault)

            payload shouldBe RequestDataError(code, message, source)
        }

        test("exception is thrown in case data error source cannot be resolved") {
            val failingDataErrorSourceProvider =
                mock<DataErrorSourceResolver<Any>> { every { sourceOf(any()) } throws RuntimeException() }

            shouldThrow<CauseProcessingException> {
                RequestDataErrorProcessor(dummy(), dummy(), failingDataErrorSourceProvider)
                    .process(Any().causeId(""))
            }
        }
    }
)
