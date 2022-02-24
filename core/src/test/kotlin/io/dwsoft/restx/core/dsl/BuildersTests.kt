package io.dwsoft.restx.core.dsl

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

private fun addSingleErrorPayloadGeneratorBuildersTests(spec: FunSpec) = spec.apply {
    test("created generator identifies faults by type by default") {
    }

    test("created generator by default generates payload with code same as cause key") {
    }
}

class OperationErrorPayloadGeneratorBuilderTests : FunSpec({
    test("created generator identifies faults by type by default") {
        val config = OperationErrorPayloadGeneratorSpecDelegate<Any>().apply {
            withCode("code")
            withMessage { fromText("message") }
        }
        val generator = OperationErrorPayloadGeneratorBuilder.buildFrom(config)

        val payload = generator.payloadOf(Any())

        payload.code shouldBe Any::class.java.name
    }

    test("created generator by default generates payload with code same as cause key") {
    }
})
