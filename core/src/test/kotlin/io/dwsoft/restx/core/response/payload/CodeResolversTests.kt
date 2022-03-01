package io.dwsoft.restx.core.response.payload

import io.kotest.assertions.assertSoftly
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.containInOrder
import io.kotest.matchers.string.shouldContain

class FixedCodeResolverTests : FunSpec({
    test("defined code is always returned") {
        val expectedCode = "CONST_CODE"
        val sut = FixedCodeResolver(expectedCode)

        assertSoftly {
            sut.codeFor(Any().causeKey("key1")) shouldBe expectedCode
            sut.codeFor(Exception().causeKey("key2")) shouldBe expectedCode
            sut.codeFor("fault".causeKey("key1")) shouldBe expectedCode
        }
    }
})

class MapBasedCodeResolverTests : FunSpec({
    test("code resolver cannot be created without mappings") {
        shouldThrow<IllegalArgumentException> {
            MapBasedCodeResolver(emptyMap())
        }.message shouldContain "Fault code mappings not provided"
    }

    test("code is returned for defined cause key") {
        val faultKey = "fault-key"
        val expectedCode = "expected-code"

        val code = MapBasedCodeResolver(mapOf(faultKey to expectedCode))
            .codeFor(Any().causeKey(faultKey))

        code shouldBe expectedCode
    }

    test("exception is thrown when code is not defined for given fault key") {
        val unmappedKey = "unmapped-key"

        shouldThrow<CodeResolvingException> {
            MapBasedCodeResolver(mapOf("fault-key" to "code"))
                .codeFor(Any().causeKey(unmappedKey))
        }.message should containInOrder("None code mapping found for key", unmappedKey)
    }
})
