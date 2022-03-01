package io.dwsoft.restx.core.dsl

import io.dwsoft.restx.core.response.payload.Message
import io.dwsoft.restx.core.response.payload.causeKey
import io.dwsoft.restx.core.response.payload.invoke
import io.kotest.assertions.assertSoftly
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlin.random.Random
import kotlin.random.nextUInt

class CauseResolverFactoriesTests : FunSpec({
    test("single cause with fixed id is returned") {
        val expectedId = "fixed-id"
        val resolver = CauseResolvers<Any>().fixedKey(expectedId)

        assertSoftly {
            resolver(Any()).key shouldBe expectedId
            resolver(RuntimeException()).key shouldBe expectedId
            resolver("ABC").key shouldBe expectedId
        }
    }

    test("cause with type based id is created") {
        val resolver = CauseResolvers<Any>().type()

        assertSoftly {
            resolver(Any()).key shouldBe qualifiedNameOf<Any>()
            resolver(RuntimeException()).key shouldBe qualifiedNameOf<RuntimeException>()
            resolver("ABC").key shouldBe qualifiedNameOf<String>()
        }
    }

    test("first parent type found is used when type cannot retrieved from fault object") {
        open class LocalTypeWithRuntimeUnresolvableName : RuntimeException()
        val resolver = CauseResolvers<Exception>().type()

        val causeId = resolver.causeOf(object : LocalTypeWithRuntimeUnresolvableName() {}).key

        causeId shouldBe RuntimeException::class.qualifiedName
    }

    test("cause with id supplied by function is created") {
        val faultId = "fault-id"
        val resolver = CauseResolvers<TestFaultClass>().function { it.faultId }

        resolver.causeOf(TestFaultClass(faultId)).key shouldBe faultId
    }
})

private inline fun <reified T> qualifiedNameOf() = T::class.qualifiedName!!

private open class TestFaultClass(val faultId: String = "") {
    companion object {
        fun classQualifiedName() = TestFaultClass::class.qualifiedName!!
    }
}

class CodeResolverFactoriesTests : FunSpec({
    test("code same as cause id is returned") {
        val expectedId = "id"
        val sut = CodeResolvers<Any>().sameAsCauseKey()

        sut.codeFor(Any().causeKey(expectedId)) shouldBe expectedId
    }

    test("code defined by passed function is returned") {
        val suffix = Random.nextUInt()
        val sut = CodeResolvers<Any>().generatedAs { "${context::class.simpleName!!}_$suffix" }

        assertSoftly {
            sut.codeFor(Any().causeKey("id1")) shouldBe "${Any::class.simpleName}_$suffix"
            sut.codeFor(Exception().causeKey("id2")) shouldBe "${Exception::class.simpleName}_$suffix"
            sut.codeFor("fault".causeKey("id1")) shouldBe "${String::class.simpleName}_$suffix"
        }
    }
})

class MessageResolverFactoriesTests : FunSpec({
    test("plain text message is returned") {
        val sut = MessageResolvers<Exception>().fromTextGeneratedAs { context.localizedMessage }

        sut.messageFor(Exception("Message 1").causeKey("id1")).value shouldBe "Message 1"
        sut.messageFor(Exception("Message 2").causeKey("id2")).value shouldBe "Message 2"
    }

    test("fixed message is always returned") {
        val expectedMessage = "Expected fault message"
        val sut = MessageResolvers<Any>().fromText(expectedMessage)

        assertSoftly {
            sut.messageFor(Any().causeKey("id1")).value shouldBe expectedMessage
            sut.messageFor(Exception().causeKey("id2")).value shouldBe expectedMessage
            sut.messageFor("fault".causeKey("id1")).value shouldBe expectedMessage
        }
    }

    test("custom defined message is returned") {
        val expectedMessage = "cause"
        val sut = MessageResolvers<Any>().generatedAs { Message(expectedMessage) }

        sut.messageFor(Any().causeKey(expectedMessage)).value shouldBe expectedMessage
    }
})
