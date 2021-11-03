package io.dwsoft.restx.core.cause.message

import io.dwsoft.restx.core.cause.causeId
import io.kotest.assertions.assertSoftly
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.containInOrder
import io.kotest.matchers.string.shouldContain

class FixedMessageResolverTests : FunSpec({
    test("defined message is always returned") {
        val expectedMessage = "Expected fault message"
        val sut = FixedMessageResolver(expectedMessage)

        assertSoftly {
            sut.messageFor(Any().causeId("id1")) shouldBe expectedMessage
            sut.messageFor(Exception().causeId("id2")) shouldBe expectedMessage
            sut.messageFor("fault".causeId("id1")) shouldBe expectedMessage
        }
    }
})

class MapBasedMessageResolverTests : FunSpec({
    test("message resolver cannot be created without mappings") {
        shouldThrow<IllegalArgumentException> {
            MapBasedMessageResolver(emptyMap())
        }.message shouldContain "Fault message mappings not provided"
    }

    test("message is returned for defined cause id") {
        val faultId = "fault-id"
        val expectedMessage = "Expected message"

        val message = MapBasedMessageResolver(mapOf(faultId to expectedMessage))
            .messageFor(Any().causeId(faultId))

        message shouldBe expectedMessage
    }

    test("exception is thrown when message is not defined for given fault id") {
        val unmappedId = "unmapped-id"

        shouldThrow<MessageResolvingFailure> {
            MapBasedMessageResolver(mapOf("fault-id" to "Message"))
                .messageFor(Any().causeId(unmappedId))
        }.message should containInOrder("None message mapping found for id", unmappedId)
    }
})
