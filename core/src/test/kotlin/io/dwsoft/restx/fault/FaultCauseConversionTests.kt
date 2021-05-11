package io.dwsoft.restx.fault

import io.dwsoft.restx.Fault
import io.dwsoft.restx.dummy
import io.dwsoft.restx.mock
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.containInOrder
import io.kotest.matchers.string.shouldContain
import io.mockk.every
import io.mockk.verify

class FaultCauseConverterTests : FunSpec() {
    init {
        test("code provider should be called") {
            val fault = Fault().cause("")
            val codeProvider = mock<FaultCodeProvider>()

            FaultCauseConverter(codeProvider, dummy()).convert(fault)

            verify { codeProvider.codeFor(fault) }
        }

        test("message provider should be called") {
            val fault = Fault().cause("")
            val messageProvider = mock<FaultMessageProvider>()

            FaultCauseConverter(dummy(), messageProvider).convert(fault)

            verify { messageProvider.messageFor(fault) }
        }

        test("error with defined data is returned") {
            val fault = Fault().cause("")
            val expectedError = ApiError("code", "message")

            val error = FaultCauseConverter(
                { expectedError.code }, { expectedError.message }
            ).convert(fault)

            error shouldBe expectedError
        }

        test("exception is thrown in case of code provider failure") {
            val sut = FaultCauseConverter(
                mock { every { codeFor(any()) } throws RuntimeException() },
                dummy()
            )

            shouldThrow<FaultCauseConversionFailed> { sut.convert(Fault().cause("")) }
        }

        test("exception is thrown in case of message provider failure") {
            val sut = FaultCauseConverter({ "code" },
                mock { every { messageFor(any()) } throws RuntimeException() })

            shouldThrow<FaultCauseConversionFailed> { sut.convert(Fault().cause("")) }
        }
    }
}

class FixedFaultCodeProviderTests : FunSpec() {
    init {
        test("defined code is always returned") {
            val expectedCode = "CONST_CODE"
            val sut = FixedFaultCodeProvider(expectedCode)

            sut.codeFor(Fault().cause("id1")) shouldBe expectedCode
            sut.codeFor(Exception().cause("id2")) shouldBe expectedCode
            sut.codeFor("fault".cause("id1")) shouldBe expectedCode
        }
    }
}

class FixedFaultMessageProviderTests : FunSpec() {
    init {
        test("defined message is always returned") {
            val expectedMessage = "Expected fault message"
            val sut = FixedFaultMessageProvider(expectedMessage)

            sut.messageFor(Fault().cause("id1")) shouldBe expectedMessage
            sut.messageFor(Exception().cause("id2")) shouldBe expectedMessage
            sut.messageFor("fault".cause("id1")) shouldBe expectedMessage
        }
    }
}

class MapBasedFaultCodeProviderTests : FunSpec() {
    init {
        test("code provider cannot be created without mappings") {
            shouldThrow<IllegalArgumentException> {
                MapBasedFaultCodeProvider(emptyMap())
            }.message shouldContain "Fault code mappings not provided"
        }

        test("code is returned for defined cause id") {
            val faultId = "fault-id"
            val expectedCode = "expected-code"

            val code = MapBasedFaultCodeProvider(mapOf(faultId to expectedCode))
                .codeFor(Fault().cause(faultId))

            code shouldBe expectedCode
        }

        test("exception is thrown when code is not defined for given fault id") {
            val unmappedId = "unmapped-id"

            shouldThrow<FaultCodeProvidingFailure> {
                MapBasedFaultCodeProvider(mapOf("fault-id" to "code"))
                    .codeFor(Fault().cause(unmappedId))
            }.message should containInOrder("None code mapping found for id", unmappedId)
        }
    }
}

class MapBasedFaultMessageProviderTests : FunSpec() {
    init {
        test("message provider cannot be created without mappings") {
            shouldThrow<IllegalArgumentException> {
                MapBasedFaultMessageProvider(emptyMap())
            }.message shouldContain "Fault message mappings not provided"
        }

        test("message is returned for defined cause id") {
            val faultId = "fault-id"
            val expectedMessage = "Expected message"

            val message = MapBasedFaultMessageProvider(mapOf(faultId to expectedMessage))
                .messageFor(Fault().cause(faultId))

            message shouldBe expectedMessage
        }

        test("exception is thrown when message is not defined for given fault id") {
            val unmappedId = "unmapped-id"

            shouldThrow<FaultCodeProvidingFailure> {
                MapBasedFaultMessageProvider(mapOf("fault-id" to "Message"))
                    .messageFor(Fault().cause(unmappedId))
            }.message should containInOrder("None message mapping found for id", unmappedId)
        }
    }
}

