package io.dwsoft.restx.fault

import io.dwsoft.restx.Fault
import io.dwsoft.restx.dummy
import io.dwsoft.restx.mock
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.verify

class FaultHandlerTests : FunSpec() {
    init {
        test("error resolver is called") {
            val fault = Fault()
            val resolver = mock<ApiErrorsResolver> {
                every { errorsOf(fault) } returns listOf(dummy())
            }

            FaultHandler(resolver, dummy()).handle(fault)

            verify { resolver.errorsOf(fault) }
        }

        test("exception is thrown when no errors are resolved") {
            val resolver = ApiErrorsResolver.returning(emptyList())

            shouldThrow<NoErrorsResolved> {
                FaultHandler(resolver, dummy()).handle(Fault())
            }
        }

        test("status provider is called") {
            val errorsResolver = ApiErrorsResolver.returning(dummy<ApiError>())
            val statusProvider = mock<ResponseStatusProvider> {
                every { get() } returns dummy()
            }

            FaultHandler(errorsResolver, statusProvider).handle(Fault())

            verify { statusProvider.get() }
        }

        test("single error is converted to response") {
            val apiError = dummy<ApiError>()
            val handler = FaultHandler(ApiErrorsResolver.returning(apiError)) { HttpStatus(500) }

            val response = handler.handle(Fault())

            response shouldBe ErrorResponse(HttpStatus(500), apiError)
        }

        test("multiple errors are converted to response") {
            val apiErrors = listOf<ApiError>(dummy(), dummy())
            val handler = FaultHandler(ApiErrorsResolver.returning(apiErrors)) { HttpStatus(500) }

            val response = handler.handle(Fault())

            response shouldBe ErrorResponse(status(500), apiErrors)
        }
    }
}