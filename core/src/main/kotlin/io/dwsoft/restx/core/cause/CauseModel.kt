package io.dwsoft.restx.core.cause

/**
 * Class that holds info that allows to identify specific reason of failure.
 *
 * @param T type of an [object][context] holding info related to this cause of failure
 */
class Cause<out T : Any>(val key: String, val context: T) {
    /**
     * Equality check - [Cause] instances are considered the same, if they are identified by the same [key].
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Cause<*>

        if (key != other.key) return false

        return true
    }

    override fun hashCode(): Int {
        return key.hashCode()
    }

    override fun toString(): String {
        return "Cause(key='$key')"
    }
}

/**
 * Utility method to create [Cause] with given [key] for fault represented by this object.
 */
internal fun <T : Any> T.causeKey(key: String) = Cause(key, this)
