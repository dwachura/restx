package io.dwsoft.restx.core.response.payload

import io.dwsoft.restx.core.dummy
import io.dwsoft.restx.core.response.payload.Message.Translator.LocaleNotSupported
import io.dwsoft.restx.core.response.payload.Message.Translator.LocalizationException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import java.util.Locale

class MessageTests : FunSpec({
    test("message without translator configured returns message's value by default") {
        val expectedText = "Value"

        val localized = Message(expectedText).localized(dummy())

        localized shouldBe expectedText
    }

    test("localizing returns value translated by configured translator") {
        val expectedText = "translated"
        val message = Message("value").translatedBy { _, _ -> expectedText }

        val localized = message.localized(dummy())

        localized shouldBe expectedText
    }

    test(
        """
            |message configured with default translation strategy returns message's value for unsupported value,
            | when no default display text was set
        """.trimMargin()
    ) {
        val expectedText = "default"
        val message = Message(expectedText).withDefaultTranslator { _, locale -> throw LocaleNotSupported(locale) }

        val localized = message.localized(dummy())

        localized shouldBe expectedText
    }

    test("message configured with default translation strategy returns default text for unsupported value") {
        val expectedText = "default"
        val message = Message("").withDefaultTranslator(expectedText) { _, locale -> throw LocaleNotSupported(locale) }

        val localized = message.localized(dummy())

        localized shouldBe expectedText
    }

    test("message configured with default translation strategy rethrows exception thrown by decorated translator") {
        val expectedException = LocalizationException("default", Locale.ENGLISH, "Error")
        val message = Message("").withDefaultTranslator { _, _ -> throw expectedException }

        shouldThrow<LocalizationException> { message.localized(dummy()) } shouldBeSameInstanceAs expectedException
    }
})
