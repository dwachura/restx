package io.dwsoft.restx.core.cause

import io.kotest.assertions.assertSoftly
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class FaultCauseIdEqualityTests : FunSpec({
    test("objects with the same id are equal, regardless of fault results") {
        val causeOf: FaultCreator = { it.causeId("id") }

        assertSoftly {
            (causeOf(Any()) == causeOf(Any())) shouldBe true
            (causeOf(Any()) == causeOf(RuntimeException())) shouldBe true
        }
    }

    test("objects with different id, created for the same fault are not equal") {
        val fault = Any()
        val cause1 = fault.causeId("id-1")
        val cause2 = fault.causeId("id-2")

        (cause1 == cause2) shouldBe false
    }
})

private typealias FaultCreator = (Any) -> Cause<Any>
