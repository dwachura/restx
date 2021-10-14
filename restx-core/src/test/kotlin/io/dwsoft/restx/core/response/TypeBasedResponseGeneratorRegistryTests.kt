package io.dwsoft.restx.core.response

import io.dwsoft.restx.core.dummy
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.concurrent.Executors
import kotlin.reflect.KClass

class TypeBasedResponseGeneratorRegistryTests : FunSpec({
    test("empty registry returns null") {
        val emptyRegistry = TypeBasedResponseGeneratorRegistry(emptyMap())

        val result = emptyRegistry.searchFor(Any())

        result.shouldBeNull()
    }

    test("lookup of unmapped type is null") {
        val registry = TypeBasedResponseGeneratorRegistry(mapOf(
            RuntimeException::class to dummy()
        ))

        val result = registry.searchFor(Exception())

        result.shouldBeNull()
    }

    test("generator is returned for mapped type") {
        val expectedGenerator = dummy<ResponseGenerator<RuntimeException>>()
        val registry = TypeBasedResponseGeneratorRegistry(mapOf(
            RuntimeException::class to expectedGenerator
        ))

        val result = registry.searchFor(RuntimeException())

        result shouldBe expectedGenerator
    }

    test("generator is returned for mapped base type when no explicit mapping exists") {
        val expectedGenerator = dummy<ResponseGenerator<RuntimeException>>()
        val registry = TypeBasedResponseGeneratorRegistry(mapOf(
            RuntimeException::class to expectedGenerator
        ))

        val result = registry.searchFor(IllegalArgumentException())

        result shouldBe expectedGenerator
    }

    test("map is called once for multiple same calls") {
        val generatorsMap = spyk(mapOf<KClass<*>, ResponseGenerator<*>>(
            Exception::class to dummy<ResponseGenerator<Exception>>()
        ))
        val registry = TypeBasedResponseGeneratorRegistry(generatorsMap)

        withContext(Executors.newFixedThreadPool(2).asCoroutineDispatcher()) {
            listOf(
                async { registry.searchFor(IOException("ex1")) },
                async { registry.searchFor(IOException("ex2")) }
            ).joinAll()
        }
        registry.searchFor(IOException("ex3"))

        verify(exactly = 1) { generatorsMap[IOException::class] }
    }
})

class TypeBasedResponseGeneratorRegistryBuilderTests : FunSpec({
    test("configuration without any mapping throws exception") {
        val invalidConfig = TypeBasedResponseGeneratorRegistry.Builder.Config()

        with(invalidConfig) {
            shouldThrow<IllegalArgumentException> {
                TypeBasedResponseGeneratorRegistry.buildFrom(this)
            }.message shouldContain "Response generator registry cannot be empty"
        }
    }
})
