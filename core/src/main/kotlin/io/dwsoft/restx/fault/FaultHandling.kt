package io.dwsoft.restx.fault

import io.dwsoft.restx.Fault
import io.dwsoft.restx.RestXException
import io.dwsoft.restx.initLog

/**
 * RestX library's entry point to handle fault results
 */
class FaultHandler(
    private val resolver: ApiErrorsResolver,
    private val statusProvider: ResponseStatusProvider
) {
    private val log = initLog()

    /**
     * Main method that performs fault result handling
     *
     * @throws NoErrorsResolved when resolver returns empty collection of errors
     */
    fun handle(fault: Fault): ErrorResponse {
        log.info { "Handling fault: $fault" }
        val errors = resolver.errorsOf(fault)
        return (errors.takeIf { it.isNotEmpty() } ?: throw NoErrorsResolved())
            .toErrorResponse(statusProvider.get())
    }
}

class NoErrorsResolved : RestXException()

private fun Collection<ApiError>.toErrorResponse(status: HttpStatus) =
    ErrorResponse(status, this.toList())

/**
 * Interface of API errors resolvers - components used to convert
 * fault results into one or more API errors objects
 */
fun interface ApiErrorsResolver {
    /**
     * Method that performs fault result conversion into collection of API errors
     *
     * @throws RestXException  in case of failure during errors resolution process
     */
    fun errorsOf(fault: Fault): Collection<ApiError>

    // TODO: work on naming of functions
    companion object {
        /**
         * Factory method that returns implementation of resolver returning collection
         * of pre-defined errors for any [Fault] result
         */
        fun returning(errors: Collection<ApiError>) = ApiErrorsResolver { errors }

        fun returning(vararg errors: ApiError) = returning(listOf(*errors))

        fun returning(lazyError: (Fault) -> Collection<ApiError>) = ApiErrorsResolver { lazyError(it) }

        fun returningSingle(lazyError: (Fault) -> ApiError) = ApiErrorsResolver { listOf(lazyError(it)) }
    }
}

/**
 * Factory method that returns default implementation of [ApiErrorsResolver].
 * Returned instance throws [NoCausesResolved] in case passed [FaultCauseResolver]
 * returns empty collection of causes
 */
fun ApiErrorsResolver(
    resolver: FaultCauseResolver,
    converter: FaultCauseConverter
) = object : ApiErrorsResolver {
    private val log = ApiErrorsResolver::class.initLog()

    override fun errorsOf(fault: Fault): Collection<ApiError> {
        log.info { "Resolving errors of fault: $fault" }
        val causes = resolver.causesOf(fault)
        return (causes.takeIf { it.isNotEmpty() } ?: throw NoCausesResolved())
            .map { converter.convert(it) }
    }
}

class NoCausesResolved : RestXException()

fun interface FaultCauseResolver {
    fun causesOf(fault: Fault): List<FaultCause>
}

fun interface ResponseStatusProvider {
    fun get(): HttpStatus
}
