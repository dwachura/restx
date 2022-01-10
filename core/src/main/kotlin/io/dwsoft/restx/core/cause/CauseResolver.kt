package io.dwsoft.restx.core.cause

import io.dwsoft.restx.RestXException
import io.dwsoft.restx.core.Logging.initLog
import kotlin.reflect.KClass
import kotlin.reflect.full.superclasses

/**
 * Interface of cause resolvers - services used to retrieve information about reasons of failure from passed
 * fault result.
 *
 * @param T type of fault objects that resolvers of this class supports
 *
 * @throws CauseResolvingFailure in case of errors during resolving
 */
fun interface CauseResolver<T : Any> {
    fun causeOf(fault: T): Cause<T>

    /**
     * Factories of [CauseResolver]s.
     */
    companion object Factories {
        /**
         * Factory method for [CauseResolver]s that provide causes identified by values provided by given supplier
         * (possible retrieved from fault result instance).
         */
        fun <T : Any> function(supplier: (T) -> String): CauseResolver<T> =
            CauseResolver { faultResult -> Cause(supplier(faultResult), faultResult) }

        /**
         * Factory method for [CauseResolver]s that provide causes identified by given constant value.
         */
        fun <T : Any> fixedId(id: String): CauseResolver<T> = function { id }

        /**
         * Factory method for [CauseResolver]s that provide causes identified by fault's [runtime type name]
         * [KClass.qualifiedName].
         *
         * In case of type cannot be resolved (e.g. anonymous or local class instance passed) fault's type hierarchy
         * is looked-up.
         */
        fun <T : Any> type(): CauseResolver<T> =
            object : CauseResolver<T> {
                private val log = CauseResolver::class.initLog()

                override fun causeOf(fault: T): Cause<T> {
                    val typeName = listOf(fault::class).findTypeName()
                        .also { log.debug { "Type name found: $it" } }
                    return Cause(typeName, fault)
                }

                private fun List<KClass<*>>.findTypeName(): String =
                    also { log.debug { "Searching type name for classes: $it" } }
                        .firstNotNullOfOrNull { it.qualifiedName }
                        ?: also { log.debug { "Resolvable type name not found - parent types will be searched" } }
                            .flatMap { it.superclasses }.findTypeName()
            }
    }
}
operator fun <T : Any> CauseResolver<T>.invoke(fault: T) = causeOf(fault)

class CauseResolvingFailure(message: String) : RestXException(message)
