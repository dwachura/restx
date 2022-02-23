package io.dwsoft.restx.core.cause

import io.dwsoft.restx.RestXException

/**
 * Interface of cause resolvers - services used to retrieve information about reasons of failure from passed
 * fault result.
 *
 * @param T type of fault objects that resolvers of this class supports
 *
 * @throws CauseResolvingException in case of errors during resolving
 */
fun interface CauseResolver<T : Any> {
    fun causeOf(fault: T): Cause<T>
}
operator fun <T : Any> CauseResolver<T>.invoke(fault: T) = causeOf(fault)

class CauseResolvingException(message: String) : RestXException(message)
