package io.dwsoft.restx.fault

import io.dwsoft.restx.Fault
import io.dwsoft.restx.dummy
import io.dwsoft.restx.mock
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.mockk.every
import io.mockk.verify

class ApiErrorsResolverTests : FunSpec() {
    init {
        test("fault cause resolver is called") {
            val resolver = mock<FaultCauseResolver> {
                every { causesOf(any()) } returns listOf(dummy())
            }
            val fault = Fault()

            ApiErrorsResolver(resolver, dummy()).errorsOf(fault)

            verify { resolver.causesOf(fault) }
        }

        test("exception is thrown when no causes are found") {
            val resolver = FaultCauseResolver { emptyList() }

            shouldThrow<NoCausesResolved> {
                ApiErrorsResolver(resolver, dummy()).errorsOf(Fault())
            }
        }

        test("converter is called for each cause") {
            val causes = listOf<FaultCause>(dummy(), dummy(), dummy())
            val resolver = FaultCauseResolver { causes }
            val converter = mock<FaultCauseConverter>()

            ApiErrorsResolver(resolver, converter).errorsOf(Fault())

            causes.forEach { verify { converter.convert(it) } }
        }

        test("collection of errors is returned") {
            val resolver = FaultCauseResolver { listOf(dummy(), dummy()) }
            val expectedErrors = listOf<ApiError>(dummy(), dummy())
            val converter = mock<FaultCauseConverter> {
                every { convert(any()) } returnsMany expectedErrors
            }

            val result = ApiErrorsResolver(resolver, converter).errorsOf(Fault())

            result shouldContainExactly expectedErrors
        }
    }
}