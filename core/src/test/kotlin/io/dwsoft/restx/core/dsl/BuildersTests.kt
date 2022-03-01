package io.dwsoft.restx.core.dsl

import io.dwsoft.restx.core.dummy
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FreeSpec
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

class BasicResponseGeneratorBuilderTests : FreeSpec({
    "exception is thrown when status provider is not configured" - {
        listOf(
            "OperationErrorResponseGenerator" to {
                OperationErrorResponseGeneratorSpecDelegate<Any>()
                    .apply { withMessage { dummy() } }
                    .let { BasicResponseGeneratorBuilder.buildFrom(it) }
            },
            "RequestDataErrorPayloadGenerator" to {
                RequestDataErrorResponseGeneratorSpecDelegate<Any>()
                    .apply {
                        withMessage { dummy() }
                        causedByInvalidInput { dummy() }
                    }
                    .let { BasicResponseGeneratorBuilder.buildFrom(it) }
            },
            "MultiErrorResponseGenerator" to {
                MultiErrorResponseGeneratorSpecDelegate<Any, Any>().apply {
                    extractedAs(dummy())
                    eachRepresenting { dummy() }
                }.let { BasicResponseGeneratorBuilder.buildFrom(it) }
            }

        ).forEach { (title, testBlock) ->
            title {
                shouldThrow<IllegalArgumentException> { testBlock() }
                    .message shouldContain "Status provider not configured"
            }
        }
    }
})

class OperationErrorPayloadGeneratorBuilderTests : FunSpec({
    test("created generator by default generates payload with code same as cause key") {
        val config = OperationErrorPayloadGeneratorSpecDelegate<Any>().apply {
            identifiedByKey("key")
            withMessage { dummy() }
        }
        val generator = OperationErrorPayloadGeneratorBuilder.buildFrom(config)

        val payload = generator.payloadOf(Any())

        payload.code shouldBe "key"
    }

    test("created generator identifies faults by type by default") {
        val config = OperationErrorPayloadGeneratorSpecDelegate<Any>().apply {
            withCode { sameAsCauseKey() }
            withMessage { dummy() }
        }
        val generator = OperationErrorPayloadGeneratorBuilder.buildFrom(config)

        val payload = generator.payloadOf(Any())

        payload.code shouldBe Any::class.qualifiedName
    }

    test("exception is thrown when message resolver is not configured") {
        val config = OperationErrorPayloadGeneratorSpecDelegate<Any>()

        shouldThrow<IllegalArgumentException> { OperationErrorPayloadGeneratorBuilder.buildFrom(config) }
            .message shouldContain "Message resolver not configured"
    }
})

class RequestDataErrorPayloadGeneratorBuilderTests : FunSpec({
    test("created generator by default generates payload with code same as cause key") {
        val config = RequestDataErrorPayloadGeneratorSpecDelegate<Any>().apply {
            identifiedByKey("key")
            withMessage { dummy() }
            causedByInvalidInput { dummy() }
        }
        val generator = RequestDataErrorPayloadGeneratorBuilder.buildFrom(config)

        val payload = generator.payloadOf(Any())

        payload.code shouldBe "key"
    }

    test("created generator identifies faults by type by default") {
        val config = RequestDataErrorPayloadGeneratorSpecDelegate<Any>().apply {
            withCode { sameAsCauseKey() }
            withMessage { dummy() }
            causedByInvalidInput { dummy() }
        }
        val generator = RequestDataErrorPayloadGeneratorBuilder.buildFrom(config)

        val payload = generator.payloadOf(Any())

        payload.code shouldBe Any::class.qualifiedName
    }

    test("exception is thrown when message resolver is not configured") {
        val config = RequestDataErrorPayloadGeneratorSpecDelegate<Any>().apply {
            causedByInvalidInput { dummy() }
        }

        shouldThrow<IllegalArgumentException> { RequestDataErrorPayloadGeneratorBuilder.buildFrom(config) }
            .message shouldContain "Message resolver not configured"
    }

    test("exception is thrown when request error source resolver is not configured") {
        val config = RequestDataErrorPayloadGeneratorSpecDelegate<Any>().apply {
            withMessage { dummy() }
        }

        shouldThrow<IllegalArgumentException> { RequestDataErrorPayloadGeneratorBuilder.buildFrom(config) }
            .message shouldContain "Request error source resolver not configured"
    }
})

class MultiErrorPayloadGeneratorBuilderTests : FunSpec({
    test("exception is thrown when sub-error extractor is not configured") {
        val config = MultiErrorPayloadGeneratorSpecDelegate<Any, Any>().apply {
            eachRepresenting { dummy() }
        }

        shouldThrow<IllegalArgumentException> { MultiErrorPayloadGeneratorBuilder.buildFrom(config) }
            .message shouldContain "Sub-error extractor not configured"
    }

    test("exception is thrown when sub-error payload generator is not configured") {
        val config = MultiErrorPayloadGeneratorSpecDelegate<Any, Any>().apply {
            extractedAs { dummy() }
        }

        shouldThrow<IllegalArgumentException> { MultiErrorPayloadGeneratorBuilder.buildFrom(config) }
            .message shouldContain "Sub-error payload generator not configured"
    }
})

class CompositeResponseGeneratorBuilderTests : FunSpec({
    test("exception is thrown when response generator registry is not configured") {
        val config = CompositeResponseGeneratorSpecDelegate()

        shouldThrow<IllegalArgumentException> { CompositeResponseGeneratorBuilder.buildFrom(config) }
            .message shouldContain "Response generator registry not configured"
    }
})

class TypeBasedResponseGeneratorRegistryBuilderTests : FunSpec({
    test("exception is thrown when response generator registry has no mappings defined") {
        val config = TypeBasedResponseGeneratorRegistrySpecDelegate()

        shouldThrow<IllegalArgumentException> { TypeBasedResponseGeneratorRegistryBuilder.buildFrom(config) }
            .message shouldContain "Response generator registry cannot be empty"
    }
})
