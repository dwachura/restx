package io.dwsoft.restx.fault.cause

import io.dwsoft.restx.RestXException

/**
 * Interface of resolvers providing single cause per single fault result.
 *
 * @throws RestXException in case of errors during resolving
 */
fun interface SingleCauseResolver<T : Any> {
    fun causeOf(fault: T): Cause<T>
}
operator fun <T : Any> SingleCauseResolver<T>.invoke(fault: T) = causeOf(fault)

/**
 * Interface of resolvers providing multiple cause per single fault result.
 *
 * @throws RestXException in case of errors during resolving
 */
fun interface MultipleCauseResolver<T : Any> {
    fun causesOf(fault: T): Collection<Cause<T>>
}
operator fun <T : Any> MultipleCauseResolver<T>.invoke(fault: T) = causesOf(fault)

/**
 * Factories of cause resolvers
 */
object CauseResolvers {
    /**
     * Factory method for [SingleCauseResolver]s that provide causes identified by
     * values provided by given supplier (possible retrieved from fault result instance).
     */
    fun <T : Any> function(supplier: (T) -> String): SingleCauseResolver<T> =
        SingleCauseResolver { faultResult ->
            Cause(supplier(faultResult), faultResult)
        }

    /**
     * Factory method for [SingleCauseResolver]s that provide causes identified by
     * given constant value.
     */
    fun <T : Any> fixedId(id: String): SingleCauseResolver<T> = function { id }

    /**
     * Factory method for [SingleCauseResolver]s that provide causes identified by
     * given fault result type (as type's qualified name of the passed fault result).
     * In case runtime type cannot be resolved (e.g. anonymous object passed or local
     * class instance) type is determined from type parameter [T].
     *
     * @param T type of fault result objects supported by returned resolver (also default type)
     */
    inline fun <reified T : Any> type(): SingleCauseResolver<T> {
        val defaultClassName = T::class.qualifiedName!!
        return SingleCauseResolver { fault ->
            Cause(fault::class.qualifiedName ?: defaultClassName, fault)
        }
    }

    /**
     * Factory method that creates [MultipleCauseResolver] generating causes identified by
     * values which are composed of prefix returned by provided [SingleCauseResolver] and suffixes
     * retrieved from provided function (separated by provided separator [type]).
     *
     * @param prefixedBy [SingleCauseResolver] used to generate common prefix for causes generated
     *      by created resolver (default: [type])
     * @param withSeparator string used as separator of prefix and suffix (default: ".")
     * @param suffixedBy function used to provide suffixes for generated causes
     */
    inline fun <reified T : Any> multipleCauses(
        prefixedBy: SingleCauseResolver<T> = type(),
        withSeparator: String = ".",
        crossinline suffixedBy: (T) -> Sequence<String>
    ): MultipleCauseResolver<T> = MultipleCauseResolver { fault ->
        val tmpCause = prefixedBy.causeOf(fault)
        suffixedBy(fault)
            .map { Cause("${tmpCause.id}$withSeparator$it", tmpCause.context) }
            .toList()
    }
}
