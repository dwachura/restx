package io.dwsoft.restx.core.cause

/**
 * Class that holds info that allows to identify specific reason of failure.
 *
 * @param T type of an [object][context] holding info related to this cause of failure
 */
class Cause<out T : Any>(val id: String, val context: T) {
    /**
     * Equality check - [Cause] instances are considered the same, if they are identified by the same [id].
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Cause<*>

        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

    override fun toString(): String {
        return "CauseId(id='$id')"
    }
}

/**
 * Utility method to create [Cause] with given [id] for fault represented by this object.
 */
internal fun <T : Any> T.causeId(id: String) = Cause(id, this)
