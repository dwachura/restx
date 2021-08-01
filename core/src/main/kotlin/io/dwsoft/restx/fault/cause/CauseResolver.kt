package io.dwsoft.restx.fault.cause

import io.dwsoft.restx.RestXException
import kotlin.reflect.KClass

/**
 * Interface of cause resolvers - services used to retrieve information about reasons of failure
 * from passed fault result.
 *
 * @param T type of fault objects that resolvers of this class supports
 *
 * @throws RestXException in case of errors during resolving
 */
fun interface CauseResolver<T : Any> {
    fun causeOf(fault: T): Cause<T>
}
operator fun <T : Any> CauseResolver<T>.invoke(fault: T) = causeOf(fault)

/**
 * Factories of cause resolvers
 */
object CauseResolvers {
    /**
     * Factory method for [CauseResolver]s that provide causes identified by values provided by given
     * supplier (possible retrieved from fault result instance).
     */
    fun <T : Any> function(supplier: (T) -> String): CauseResolver<T> =
        CauseResolver { faultResult -> Cause(supplier(faultResult), faultResult) }

    /**
     * Factory method for [CauseResolver]s that provide causes identified by given constant value.
     */
    fun <T : Any> fixedId(id: String): CauseResolver<T> = function { id }

    /**
     * Factory method for [CauseResolver]s that provide causes identified by given fault result type
     * (as type's qualified name of the passed fault result).
     * In case runtime type cannot be resolved (e.g. anonymous object passed or local class instance)
     * type is determined from passed class.
     *
     * @param baseType default type that should be used when fault result object type cannot be retrieved
     *  in runtime
     * @param T type of fault result objects supported by returned resolver
     */
    fun <T : Any> type(baseType: KClass<T>): CauseResolver<T> {
        val defaultClassName = baseType.qualifiedName!!
        return CauseResolver { fault -> Cause(fault::class.qualifiedName ?: defaultClassName, fault) }
    }

    inline fun <reified T : Any> type(): CauseResolver<T> = type(T::class)
}
