package io.dwsoft.restx.core.cause

import io.kotest.assertions.assertSoftly
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

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

    test("creation of resolver with unresolvable default type throws exception") {
        class LocalTypeWithRuntimeUnresolvableName

        shouldThrow<IllegalArgumentException> {
            CauseResolvers.type<LocalTypeWithRuntimeUnresolvableName>()
        }.apply {
            message shouldContain Regex("Default type \\[.*] doesn't have resolvable canonical name")
            message shouldContain LocalTypeWithRuntimeUnresolvableName::class.java.name
        }

    }

    test("cause with id supplied by function is created") {
        val faultId = "fault-id"
        val resolver = CauseResolvers.function<TestFaultClass> { it.faultId }

        resolver.causeOf(TestFaultClass(faultId)).id shouldBe faultId
    }
})

private inline fun <reified T> qualifiedNameOf() = T::class.qualifiedName!!

private open class TestFaultClass(val faultId: String = "") {
    companion object {
        fun classQualifiedName() = TestFaultClass::class.qualifiedName!!
    }
}
