package io.dwsoft.restx.core.cause.code

import io.dwsoft.restx.core.cause.causeId
import io.kotest.assertions.assertSoftly
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.containInOrder
import io.kotest.matchers.string.shouldContain
import kotlin.random.Random
import kotlin.random.nextUInt

class FixedCauseCodeProviderTests : FunSpec({
    test("defined code is always returned") {
        val expectedCode = "CONST_CODE"
        val sut = FixedCauseCodeProvider(expectedCode)

        assertSoftly {
            sut.codeFor(Any().causeId("id1")) shouldBe expectedCode
            sut.codeFor(Exception().causeId("id2")) shouldBe expectedCode
            sut.codeFor("fault".causeId("id1")) shouldBe expectedCode
        }
    }
})

class MapBasedCauseCodeProviderTests : FunSpec({
    test("code provider cannot be created without mappings") {
        shouldThrow<IllegalArgumentException> {
            MapBasedCauseCodeProvider(emptyMap())
        }.message shouldContain "Fault code mappings not provided"
    }

    test("code is returned for defined cause id") {
        val faultId = "fault-id"
        val expectedCode = "expected-code"

        val code = MapBasedCauseCodeProvider(mapOf(faultId to expectedCode))
            .codeFor(Any().causeId(faultId))

        code shouldBe expectedCode
    }

    test("exception is thrown when code is not defined for given fault id") {
        val unmappedId = "unmapped-id"

        shouldThrow<CauseCodeProvisioningFailure> {
            MapBasedCauseCodeProvider(mapOf("fault-id" to "code"))
                .codeFor(Any().causeId(unmappedId))
        }.message should containInOrder("None code mapping found for id", unmappedId)
    }
})

class GeneratedCauseCodeProviderTests : FunSpec({
    test("code defined by passed function is returned") {
        val suffix = Random.nextUInt()
        val sut = CauseCodeProviders.generatedAs<Any> { "${context::class.simpleName!!}_$suffix" }

        assertSoftly {
            sut.codeFor(Any().causeId("id1")) shouldBe "${Any::class.simpleName}_$suffix"
            sut.codeFor(Exception().causeId("id2")) shouldBe "${Exception::class.simpleName}_$suffix"
            sut.codeFor("fault".causeId("id1")) shouldBe "${String::class.simpleName}_$suffix"
        }
    }

    test("code same as cause id is returned") {
        val expectedId = "id"
        val sut = CauseCodeProviders.sameAsCauseId<Any>()

        sut.codeFor(Any().causeId(expectedId)) shouldBe expectedId
    }
})
