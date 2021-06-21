package io.dwsoft.restx.fault.cause

import io.dwsoft.restx.fault.dummy
import io.dwsoft.restx.fault.mock
import io.kotest.assertions.assertSoftly
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.verify

class CauseResolverFactoriesTests : FunSpec({
    test("single cause with fixed id is returned") {
        val expectedId = "fixed-id"
        val resolver = CauseResolvers.fixedId<Any>(expectedId)

        assertSoftly {
            resolver(Any()).id shouldBe expectedId
            resolver(RuntimeException()).id shouldBe expectedId
            resolver("ABC").id shouldBe expectedId
        }
    }

    test("cause with type based id is created") {
        val resolver = CauseResolvers.type<Any>()

        assertSoftly {
            resolver(Any()).id shouldBe qualifiedNameOf<Any>()
            resolver(RuntimeException()).id shouldBe qualifiedNameOf<RuntimeException>()
            resolver("ABC").id shouldBe qualifiedNameOf<String>()
        }
    }

    test("resolver's type parameter is used when type cannot retrieved from fault object") {
        val resolver = CauseResolvers.type<TestFaultClass>()

        val causeId = resolver(object : TestFaultClass() {})

        causeId.id shouldBe TestFaultClass.classQualifiedName()
    }

    test("cause with id supplied by function is created") {
        val faultId = "fault-id"
        val resolver = CauseResolvers.function<TestFaultClass> { it.faultId }

        resolver.causeOf(TestFaultClass(faultId)).id shouldBe faultId
    }

    test("multi-cause resolver calls single-cause one used to its creation") {
        val fault = TestFaultClass()
        val singleCauseResolver = mock<SingleCauseResolver<TestFaultClass>> {
            every { this@mock.causeOf(fault) } returns dummy()
        }
        val multiCauseResolver = CauseResolvers.multipleCauses(
            prefixedBy = singleCauseResolver,
            suffixedBy = { dummy() }
        )

        multiCauseResolver.causesOf(fault)

        verify { singleCauseResolver.causeOf(fault) }
    }

    test("""
        causes returned by multi-cause resolver are combined of single-cause 
        resolver cause's id and provided suffixes
    """.trimIndent()) {
        val prefix = "prefix"
        val expectedSuffixes = sequenceOf("1", "2", "3")
        val resolver = CauseResolvers.multipleCauses(
            prefixedBy = CauseResolvers.fixedId(prefix),
            suffixedBy = { expectedSuffixes }
        )
        val expectedIds = expectedSuffixes.map { "$prefix.$it" }.toList()

        val causesIds = resolver.causesOf(TestFaultClass()).map { it.id }

        causesIds shouldContainExactly expectedIds
    }

    test("separator of multi-cause resolver can be changed") {
        val separator = "---"
        val expectedSuffix = "suffix"
        val resolver = CauseResolvers.multipleCauses<TestFaultClass>(
            withSeparator = separator
        ) { sequenceOf(expectedSuffix) }

        val causeId = resolver.causesOf(TestFaultClass()).map { it.id }.first()

        causeId shouldBe "${TestFaultClass.classQualifiedName()}$separator$expectedSuffix"
    }
})

private inline fun <reified T> qualifiedNameOf() = T::class.qualifiedName!!

private open class TestFaultClass(val faultId: String = "") {
    companion object {
        fun classQualifiedName() = TestFaultClass::class.qualifiedName!!
    }
}
