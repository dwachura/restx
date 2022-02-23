package io.dwsoft.restx.core.cause

import io.dwsoft.restx.RestXException
import io.dwsoft.restx.core.payload.RequestDataError.Source

/**
 * Interface of [data error sources][Source] resolvers.
 */
fun interface DataErrorSourceResolver<T : Any> {
    /**
     * Method resolving [Source] for the given error cause.
     *
     * @throws DataErrorSourceResolvingException in case of errors during resolving
     */
    fun sourceOf(cause: Cause<T>): Source
}
operator fun <T : Any> DataErrorSourceResolver<T>.invoke(cause: Cause<T>) = this.sourceOf(cause)

class DataErrorSourceResolvingException : RestXException()
