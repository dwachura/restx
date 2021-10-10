package io.dwsoft.restx

import io.dwsoft.restx.Logging.initLog
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class PlatformCommonsTests : FunSpec({
    test("logger is initialized from object type") {
        val exception: Exception = RuntimeException()

        val logger = shouldNotThrowAny { exception.initLog() }
        logger.name shouldBe RuntimeException::class.qualifiedName
    }

    test("logger is initialized from explicit type") {
        val logger = shouldNotThrowAny { String::class.initLog() }
        logger.name shouldBe String::class.qualifiedName
    }

    test("logger is initialized from null object") {
        val nullException: Exception? = null

        val logger = shouldNotThrowAny { nullException.initLog() }
        logger.name shouldBe Exception::class.qualifiedName
    }

    test("logger is initialized from anonymous object") {
        val anonymous: Exception = object : RuntimeException() {}

        val logger = shouldNotThrowAny { anonymous.initLog() }
        logger.name shouldBe Exception::class.qualifiedName
    }
})
