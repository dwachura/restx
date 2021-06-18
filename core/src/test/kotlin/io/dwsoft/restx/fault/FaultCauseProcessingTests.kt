package io.dwsoft.restx.fault

import io.dwsoft.restx.dummy
import io.dwsoft.restx.mock
import io.dwsoft.restx.payload.ApiError
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.containInOrder
import io.kotest.matchers.string.shouldContain
import io.mockk.every
import io.mockk.verify

class DefaultFaultCauseProcessorTests : FunSpec({
    test("code provider should be called") {
        val fault = FaultResult().causeId("")
        val codeProvider = mock<FaultCodeProvider<Any>>()

        FaultCauseProcessor(codeProvider, dummy()).process(fault)

        verify { codeProvider.codeFor(fault) }
    }

    test("message provider should be called") {
        val fault = FaultResult().causeId("")
        val messageProvider = mock<FaultMessageProvider<Any>>()

        FaultCauseProcessor(dummy(), messageProvider).process(fault)

        verify { messageProvider.messageFor(fault) }
    }

    test("error with defined data is returned") {
        val fault = FaultResult().causeId("")
        val expectedError = ApiError("code", "message")

        val error = FaultCauseProcessor<Any>(
            { expectedError.code }, { expectedError.message }
        ).process(fault)

        error shouldBe expectedError
    }

    test("exception is thrown in case of code provider failure") {
        val sut = FaultCauseProcessor<Any>(
            mock { every { codeFor(any()) } throws RuntimeException() },
            dummy()
        )

        shouldThrow<FaultCauseProcessingFailed> { sut.process(FaultResult().causeId("")) }
    }

    test("exception is thrown in case of message provider failure") {
        val sut = FaultCauseProcessor<Any>({ "code" },
            mock { every { messageFor(any()) } throws RuntimeException() })

        shouldThrow<FaultCauseProcessingFailed> { sut.process(FaultResult().causeId("")) }
    }
})

class FixedFaultCodeProviderTests : FunSpec({
    test("defined code is always returned") {
        val expectedCode = "CONST_CODE"
        val sut = FixedFaultCodeProvider(expectedCode)

        sut.codeFor(FaultResult().causeId("id1")) shouldBe expectedCode
        sut.codeFor(Exception().causeId("id2")) shouldBe expectedCode
        sut.codeFor("fault".causeId("id1")) shouldBe expectedCode
    }
})

class FixedFaultMessageProviderTests : FunSpec({
    test("defined message is always returned") {
        val expectedMessage = "Expected fault message"
        val sut = FixedFaultMessageProvider(expectedMessage)

        sut.messageFor(FaultResult().causeId("id1")) shouldBe expectedMessage
        sut.messageFor(Exception().causeId("id2")) shouldBe expectedMessage
        sut.messageFor("fault".causeId("id1")) shouldBe expectedMessage
    }
})

class MapBasedFaultCodeProviderTests : FunSpec({
    test("code provider cannot be created without mappings") {
        shouldThrow<IllegalArgumentException> {
            MapBasedFaultCodeProvider(emptyMap())
        }.message shouldContain "Fault code mappings not provided"
    }

    test("code is returned for defined cause id") {
        val faultId = "fault-id"
        val expectedCode = "expected-code"

        val code = MapBasedFaultCodeProvider(mapOf(faultId to expectedCode))
            .codeFor(FaultResult().causeId(faultId))

        code shouldBe expectedCode
    }

    test("exception is thrown when code is not defined for given fault id") {
        val unmappedId = "unmapped-id"

        shouldThrow<FaultCodeProvisioningFailure> {
            MapBasedFaultCodeProvider(mapOf("fault-id" to "code"))
                .codeFor(FaultResult().causeId(unmappedId))
        }.message should containInOrder("None code mapping found for id", unmappedId)
    }
})

class MapBasedFaultMessageProviderTests : FunSpec({
    test("message provider cannot be created without mappings") {
        shouldThrow<IllegalArgumentException> {
            MapBasedFaultMessageProvider(emptyMap())
        }.message shouldContain "Fault message mappings not provided"
    }

    test("message is returned for defined cause id") {
        val faultId = "fault-id"
        val expectedMessage = "Expected message"

        val message = MapBasedFaultMessageProvider(mapOf(faultId to expectedMessage))
            .messageFor(FaultResult().causeId(faultId))

        message shouldBe expectedMessage
    }

    test("exception is thrown when message is not defined for given fault id") {
        val unmappedId = "unmapped-id"

        shouldThrow<FaultMessageProvisioningFailure> {
            MapBasedFaultMessageProvider(mapOf("fault-id" to "Message"))
                .messageFor(FaultResult().causeId(unmappedId))
        }.message should containInOrder("None message mapping found for id", unmappedId)
    }
})

