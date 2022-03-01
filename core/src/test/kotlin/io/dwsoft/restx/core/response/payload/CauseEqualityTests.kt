package io.dwsoft.restx.core.response.payload

import io.kotest.assertions.assertSoftly
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class CauseEqualityTests : FunSpec({
    test("objects with the same key are equal, regardless of fault results") {
        val causeOf: FaultCreator = { it.causeKey("key") }

        assertSoftly {
            (causeOf(Any()) == causeOf(Any())) shouldBe true
            (causeOf(Any()) == causeOf(RuntimeException())) shouldBe true
        }
    }

    test("objects with different key are not equal, even though are created for the same fault") {
        val fault = Any()
        val cause1 = fault.causeKey("key-1")
        val cause2 = fault.causeKey("key-2")

        (cause1 == cause2) shouldBe false
    }
})

private typealias FaultCreator = (Any) -> Cause<Any>
