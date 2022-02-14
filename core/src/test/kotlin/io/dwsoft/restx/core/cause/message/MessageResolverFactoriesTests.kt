package io.dwsoft.restx.core.cause.message

import io.dwsoft.restx.core.cause.causeId
import io.dwsoft.restx.core.payload.Message
import io.kotest.assertions.assertSoftly
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class MessageResolverFactoriesTests : FunSpec({
    test("plain text message is returned") {
        val sut = MessageResolver.Factories.fromTextGeneratedAs<Exception> { context.localizedMessage }

        sut.messageFor(Exception("Message 1").causeId("id1")).value shouldBe "Message 1"
        sut.messageFor(Exception("Message 2").causeId("id2")).value shouldBe "Message 2"
    }

    test("fixed message is always returned") {
        val expectedMessage = "Expected fault message"
        val sut = MessageResolver.Factories.fromText<Any>(expectedMessage)

        assertSoftly {
            sut.messageFor(Any().causeId("id1")).value shouldBe expectedMessage
            sut.messageFor(Exception().causeId("id2")).value shouldBe expectedMessage
            sut.messageFor("fault".causeId("id1")).value shouldBe expectedMessage
        }
    }

    test("custom defined message is returned") {
        val expectedMessage = "cause"
        val sut = MessageResolver.Factories.generatedAs<Any> { Message(id) }

        sut.messageFor(Any().causeId(expectedMessage)).value shouldBe expectedMessage
    }
})