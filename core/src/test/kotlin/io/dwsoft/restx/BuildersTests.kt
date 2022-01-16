package io.dwsoft.restx

import io.dwsoft.restx.core.cause.CauseResolver
import io.dwsoft.restx.core.cause.DataErrorSourceResolver
import io.dwsoft.restx.core.cause.causeId
import io.dwsoft.restx.core.cause.code.CodeResolver
import io.dwsoft.restx.core.cause.message.MessageResolver
import io.dwsoft.restx.core.dummy
import io.dwsoft.restx.core.mock
import io.dwsoft.restx.core.response.ResponseStatusProvider
import io.kotest.assertions.assertSoftly
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.Tuple2
import io.kotest.core.Tuple3
import io.kotest.core.spec.style.FreeSpec
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.every
import io.mockk.verify

abstract class SingleErrorPayloadGeneratorBuilderTestsBase(
    createConfig: CreateSingleErrorPayloadGeneratorConfig,
    buildGenerator: (SingleErrorPayloadGeneratorDsl<Any>) -> Unit,
    additionalTests: InitBlock<FunSpec> = {}
) : FunSpec({
    test("configuration without message resolver factory throws exception") {
        shouldThrow<IllegalArgumentException> {
            buildGenerator(createConfig(null, null, null))
        }.message shouldContain "Message resolver factory block not set"
    }

    test("configured cause resolver factory is called") {
        val factoryBlock = mock<AnyCauseResolverFactoryBlock> {
            every { this@mock(any()) } returns dummy()
        }
        val config = createConfig(factoryBlock, null) { dummy() }

        buildGenerator(config)

        verify { factoryBlock(CauseResolver.Factories) }
    }

    test("by default generator is configured with cause resolver identifying fault by its type") {
        val defaultCauseResolver = createConfig(null, null, dummy())
            .causeResolverFactoryBlock(CauseResolver.Factories)

        assertSoftly {
            defaultCauseResolver.causeOf(RuntimeException()).id shouldBe RuntimeException::class.qualifiedName
            defaultCauseResolver.causeOf("abcd").id shouldBe String::class.qualifiedName
        }
    }

    test("by default generator is configured with code resolver generating code same as cause id") {
        val cause = Any().causeId("abc")
        val defaultCodeResolver = createConfig(null, null, dummy())
            .codeResolverFactoryBlock(CodeResolver.Factories)

        defaultCodeResolver.codeFor(cause) shouldBe cause.id
    }

    test("configured message resolver factory is called") {
        val factoryBlock = mock<AnyMessageResolverFactoryBlock> {
            every { this@mock(any()) } returns dummy()
        }
        val config = createConfig(null, null, factoryBlock)

        buildGenerator(config)

        verify { factoryBlock(MessageResolver.Factories) }
    }

    apply(additionalTests)
})

private typealias CreateSingleErrorPayloadGeneratorConfig =
            (AnyCauseResolverFactoryBlock?, AnyCodeResolverFactoryBlock?, AnyMessageResolverFactoryBlock?) ->
        SingleErrorPayloadGeneratorDsl<Any>
private typealias AnyCauseResolverFactoryBlock = CauseResolverFactoryBlock<Any>
private typealias AnyCodeResolverFactoryBlock = CodeResolverFactoryBlock<Any>
private typealias AnyMessageResolverFactoryBlock = MessageResolverFactoryBlock<Any>

class OperationErrorPayloadGeneratorBuilderTests : SingleErrorPayloadGeneratorBuilderTestsBase(
    { causeResolverFactory: AnyCauseResolverFactoryBlock?, codeResolverFactory: AnyCodeResolverFactoryBlock?,
      messageResolverFactory: AnyMessageResolverFactoryBlock? ->
        OperationErrorPayloadGeneratorBuilder.Dsl<Any>().apply {
            causeResolverFactory?.let { identifiedBy(it) }
            codeResolverFactory?.let { withCode(it) }
            messageResolverFactory?.let { withMessage(it) }
        }
    },
    { config -> (config as OperationErrorPayloadGeneratorDsl<Any>)
        .let { OperationErrorPayloadGeneratorBuilder.buildFrom(it) }
    }
)

class RequestDataErrorPayloadGeneratorBuilderTests : SingleErrorPayloadGeneratorBuilderTestsBase(
    { causeResolverFactory: AnyCauseResolverFactoryBlock?, codeResolverFactory: AnyCodeResolverFactoryBlock?,
      messageResolverFactory: AnyMessageResolverFactoryBlock? ->
        RequestDataErrorPayloadGeneratorBuilder.Dsl<Any>().apply {
            causeResolverFactory?.let { identifiedBy(it) }
            codeResolverFactory?.let { withCode(it) }
            messageResolverFactory?.let { withMessage(it) }
            pointingInvalidValue { dummy() }
        }
    },
    { config -> (config as RequestDataErrorPayloadGeneratorDsl<Any>)
        .let { RequestDataErrorPayloadGeneratorBuilder.buildFrom(it) }
    },
    {
        test("configuration without data error source resolver factory throws exception") {
            shouldThrow<IllegalArgumentException> {
                RequestDataErrorPayloadGeneratorBuilder.buildFrom(
                    RequestDataErrorPayloadGeneratorBuilder.Dsl<Any>().apply {
                        withCode { dummy() }
                        withMessage { dummy() }
                    }
                )
            }.message shouldContain "Data error source resolver factory block not set"
        }

        test("configured data error source resolver factory is called") {
            val factoryBlock = mock<AnyDataErrorSourceProviderFactoryBlock> {
                every { this@mock(any()) } returns dummy()
            }
            val config = RequestDataErrorPayloadGeneratorBuilder.Dsl<Any>().apply {
                withCode { dummy() }
                withMessage { dummy() }
                pointingInvalidValue(factoryBlock)
            }

            RequestDataErrorPayloadGeneratorBuilder.buildFrom(config)

            verify { factoryBlock(DataErrorSourceResolver.Factories) }
        }
    }
)

private typealias AnyDataErrorSourceProviderFactoryBlock = DataErrorSourceResolverFactoryBlock<Any>

class MultipleErrorPayloadGeneratorBuilderTests : FunSpec({
    test("configuration without sub-error extractor throws exception") {
        shouldThrow<IllegalArgumentException> {
            MultiErrorPayloadGeneratorBuilder.buildFrom(
                MultiErrorPayloadGeneratorBuilder.Dsl<Any, Any>().apply {
                    eachHandledBy(dummy())
                }
            )
        }.message shouldContain "Sub-error extractor must be provided"
    }

    test("configuration without sub-error payload generator throws exception") {
        shouldThrow<IllegalArgumentException> {
            MultiErrorPayloadGeneratorBuilder.buildFrom(
                MultiErrorPayloadGeneratorBuilder.Dsl<Any, Any>().apply {
                    extractedAs { dummy() }
                }
            )
        }.message shouldContain "Sub-error payload generator must be provided"
    }

    test("sub-error payload generator factory block is called") {
        val factoryBlock = mock<AnySingleErrorPayloadGeneratorFactoryBlock>()

        runCatching { MultiErrorPayloadGeneratorBuilder.Dsl<Any, Any>().eachRepresenting(factoryBlock) }

        verify { factoryBlock(any()) }
    }
})

private typealias AnySingleErrorPayloadGeneratorFactoryBlock =
        SingleErrorPayloadGeneratorFactoryBlock<Any>

class SimpleResponseGeneratorBuilderTests : FreeSpec({
    "configuration without status provider factory throws exception" - {
        listOf(
            Tuple2("OperationError response generator") {
                SimpleResponseGeneratorBuilder.buildFrom(
                    SimpleResponseGeneratorBuilder.OperationErrorResponseGenerator.dsl()
                )
            },
            Tuple2("RequestDataError response generator") {
                SimpleResponseGeneratorBuilder.buildFrom(
                    SimpleResponseGeneratorBuilder.RequestDataErrorResponseGenerator.dsl()
                )
            },
            Tuple2("multi-error response generator") {
                SimpleResponseGeneratorBuilder.buildFrom(
                    SimpleResponseGeneratorBuilder.MultiErrorResponseGenerator.dsl()
                )
            },
        ).forEach { (generatorType, callBuilder) ->
            "building of $generatorType" {
                shouldThrow<IllegalArgumentException> { callBuilder() }
                    .message shouldContain "Status provider factory block not set"
            }
        }
    }

    "configured status provider factory is called" - {
        listOf(
            Tuple3(
                "OperationError response generator",
                mock<ResponseStatusProviderFactoryBlock> { every { this@mock(any()) } returns dummy() }
            ){
                    statusProviderFactoryMock: ResponseStatusProviderFactoryBlock ->
                SimpleResponseGeneratorBuilder.buildFrom(
                    SimpleResponseGeneratorBuilder.OperationErrorResponseGenerator.dsl<Any>().apply {
                        withMessage { dummy() }
                        withStatus(statusProviderFactoryMock)
                    }
                )
            },
            Tuple3(
                "RequestDataError response generator",
                mock { every { this@mock(any()) } returns dummy() }
            ){
                    statusProviderFactoryMock: ResponseStatusProviderFactoryBlock ->
                SimpleResponseGeneratorBuilder.buildFrom(
                    SimpleResponseGeneratorBuilder.RequestDataErrorResponseGenerator.dsl<Any>().apply {
                        withMessage { dummy() }
                        pointingInvalidValue { dummy() }
                        withStatus(statusProviderFactoryMock)
                    }
                )
            },
            Tuple3(
                "multi-error response generator",
                mock { every { this@mock(any()) } returns dummy() }
            ){
                    statusProviderFactoryMock: ResponseStatusProviderFactoryBlock ->
                SimpleResponseGeneratorBuilder.buildFrom(
                    SimpleResponseGeneratorBuilder.MultiErrorResponseGenerator.dsl<Any, Any>().apply {
                        extractedAs(dummy())
                        eachHandledBy(dummy())
                        withStatus(statusProviderFactoryMock)
                    }
                )
            }
        ).forEach { (generatorType, statusProviderFactoryMock, callBuilder) ->
            "building of $generatorType" {
                callBuilder(statusProviderFactoryMock)

                verify { statusProviderFactoryMock(ResponseStatusProvider.Factories) }
            }
        }
    }
})

class TypeBasedResponseGeneratorRegistryBuilderTests : FunSpec({
    test("configuration without any mapping throws exception") {
        val invalidConfig = TypeBasedResponseGeneratorRegistryBuilder.Dsl()

        with(invalidConfig) {
            shouldThrow<IllegalArgumentException> {
                TypeBasedResponseGeneratorRegistryBuilder.buildFrom(this)
            }.message shouldContain "Response generator registry cannot be empty"
        }
    }
})

class CompositeResponseGeneratorBuilderTests : FunSpec({
    test("configuration without registry factory block throws exception") {
        val invalidConfig = CompositeResponseGeneratorBuilder.Dsl()

        with(invalidConfig) {
            shouldThrow<IllegalArgumentException> {
                CompositeResponseGeneratorBuilder.buildFrom(this)
            }.message shouldContain "Sub-generator registry factory block not set"
        }
    }
})
