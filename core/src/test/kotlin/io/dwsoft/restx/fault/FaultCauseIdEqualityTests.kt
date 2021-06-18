package io.dwsoft.restx.fault

import io.kotest.core.spec.style.FunSpec
import io.kotest.data.forAll
import io.kotest.data.headers
import io.kotest.data.row
import io.kotest.data.table
import io.kotest.matchers.shouldBe

class FaultCauseIdEqualityTests : FunSpec({
    test("objects with the same id are equal, regardless of fault results") {
        forAll(table(
            headers("First fault result", "Second fault result"),
            row(Any(), Any()),
            row(Any(), RuntimeException())
        )) { fault1, fault2 ->
            val cause1 = FaultCauseId("id", fault1)
            val cause2 = FaultCauseId("id", fault2)

            (cause1 == cause2) shouldBe true
        }
    }

    test("objects with different id, created for the same fault are not equal") {
        val fault = Any()
        val cause1 = fault.causeId("id-1")
        val cause2 = fault.causeId("id-2")

        (cause1 == cause2) shouldBe false
    }
})