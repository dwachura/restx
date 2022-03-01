package io.dwsoft.restx.core.cause.message

import io.dwsoft.restx.core.dummy
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class PlainTextMessageResolverTests : FunSpec({
    test("message containing defined text is returned") {
        val expectedMsgContent = "text"
        val resolver = PlainTextMessageResolver<Any> { expectedMsgContent }

        val message = resolver.messageFor(dummy())

        message.value shouldBe expectedMsgContent
    }
})
