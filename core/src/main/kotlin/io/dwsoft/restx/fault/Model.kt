package io.dwsoft.restx.fault

/**
 * [FaultResult] represents the result that differs from success and should be returned
 * as an error response.
 * It's just an alias for [Any] class because, in fact, any type can be treated as
 * a failure as the process itself doesn't depend on it.
 */
internal typealias FaultResult = Any

/**
 * Class that holds info that allows to identify specific reason of failure.
 *
 * @param T type of fault result object that the cause is related to
 */
class FaultCauseId<out T : FaultResult>(val id: String, val fault: T) {
    /**
     * Equality check - [FaultCauseId] instances are considered the same, if they
     * are identified by the same [id].
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as FaultCauseId<*>

        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }
}

/**
 * Utility method to create [FaultCauseId] with given [id] for fault represented
 * by this object.
 */
internal fun <T : FaultResult> T.causeId(id: String) = FaultCauseId(id, this)