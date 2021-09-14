package io.dwsoft.restx.fault.response

import io.dwsoft.restx.fault.dummy
import io.dwsoft.restx.fault.mock
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldContainInOrder
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.mockk.every
import io.mockk.verify

class CompositeResponseGeneratorTests : FunSpec({
    test("sub-generator registry is called") {
        val generatorRegistry = mock<ResponseGeneratorRegistry>()
        val fault = Any()

        CompositeResponseGenerator(generatorRegistry).responseOf(fault)

        verify { generatorRegistry.searchFor(fault) }
    }

    test("found sub-generator is called") {
        val generator = mock<ResponseGenerator<Any>>()
        val fault = Any()

        CompositeResponseGenerator(
            mock { every { searchFor(any()) } returns generator }
        ).responseOf(fault)

        verify { generator.responseOf(fault) }
    }

    test("exception is thrown when no sub-generators found") {
        val fault = Any()

        shouldThrow<NoSubGeneratorFound> {
            CompositeResponseGenerator(
                mock { every { searchFor(any()) } returns null }
            ).responseOf(fault)
        }.message.shouldContainInOrder("No sub-generator found for fault", fault.toString())
    }

    test("sub-generator result is returned") {
        val expectedResult = dummy<ErrorResponse>()
        val generator = mock<ResponseGenerator<Any>> {
            every { responseOf(any()) } returns expectedResult
        }

        val result = CompositeResponseGenerator(mock {
            every { searchFor(any()) } returns generator
        }).responseOf(Any())

        result shouldBeSameInstanceAs expectedResult
    }
})

class CompositeResponseGeneratorBuilderTests : FunSpec({
    test("configuration without registry factory block throws exception") {
        val invalidConfig = CompositeResponseGenerator.Builder.Config()

        with(invalidConfig) {
            shouldThrow<IllegalArgumentException> {
                CompositeResponseGenerator.buildFrom(this)
            }.message shouldContain "Sub-generator registry factory block not set"
        }
    }
})
