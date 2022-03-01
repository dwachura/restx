package io.dwsoft.restx.core.response.payload

/**
 * Base interface used to flag classes that represent valid error response payload.
 */
sealed interface ErrorResponsePayload

/**
 * Base representation of a single error response payload.
 */
sealed interface SingleErrorPayload : ErrorResponsePayload {
    val code: String
    val message: Message
}

/**
 * Representation of an error response payload happened during request processing.
 */
data class OperationError(override val code: String, override val message: Message) : SingleErrorPayload

/**
 * Representation of an error response payload caused by invalid request data.
 */
data class RequestDataError(
    override val code: String,
    override val message: Message,
    val source: Source
) : SingleErrorPayload {
    /**
     * Representation of a 'source' of invalid request data.
     * It's used to provide additional information for API client regarding [type][type] and [place][location] of
     * bad data, e.g. name of query param or path to body property.
     */
    class Source private constructor(val type: Type, val location: String) {
        init {
            require(location.isNotBlank()) { "Invalid data source location must be set" }
        }

        override fun toString(): String {
            return "${javaClass.simpleName}(type=$type, location=$location)"
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Source

            if (type != other.type) return false
            if (location != other.location) return false

            return true
        }

        override fun hashCode(): Int {
            var result = type.hashCode()
            result = 31 * result + location.hashCode()
            return result
        }

        companion object Factories {
            fun query(param: String) = Source(Type.QUERY, param)
            fun header(name: String) = Source(Type.HEADER, name)
            fun body(path: String) = Source(Type.BODY, path)
        }

        /**
         * Type of invalid request data source.
         */
        enum class Type {
            /**
             * Invalid value of a query parameter.
             */
            QUERY,

            /**
             * Invalid value of a header.
             */
            HEADER,

            /**
             * Invalid value of a body property.
             */
            BODY;

            fun toSource(location: String) = Source(this, location)
        }
    }
}

/**
 * Representation of a response payload generated in case of multiple errors happen during
 * execution of application's logic.
 */
data class MultiErrorPayload(val errors: List<SingleErrorPayload>) : ErrorResponsePayload

fun List<SingleErrorPayload>.toPayload() = when (size) {
    1 -> first()
    else -> MultiErrorPayload(this)
}
