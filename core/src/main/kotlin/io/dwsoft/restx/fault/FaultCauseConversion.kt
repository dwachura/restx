package io.dwsoft.restx.fault

import io.dwsoft.restx.Fault
import io.dwsoft.restx.RestXException
import io.dwsoft.restx.initLog

class FaultCause(val id: String, val fault: Fault)

internal fun <T : Fault> T.cause(id: String) = FaultCause(id, this)

/**
 * Interface for converters of fault causes to error objects
 */
fun interface FaultCauseConverter {
    /**
     * Converting method
     *
     * @throws RestXException in case of failure during conversion process
     */
    fun convert(cause: FaultCause): ApiError
}

/**
 * Factory method that returns default implementation of [FaultCauseConverter].
 * Returned instance throws [FaultCauseConversionFailed] in case of code or message
 * provider fails
 */
fun FaultCauseConverter(
    codeProvider: FaultCodeProvider,
    messageProvider: FaultMessageProvider
) = object : FaultCauseConverter {
    private val log = FaultCauseConverter::class.initLog()

    override fun convert(cause: FaultCause): ApiError {
        log.info { "Converting cause $cause" }
        val (code, message) = runCatching {
            Pair(
                codeProvider.codeFor(cause),
                messageProvider.messageFor(cause)
            )
        }.fold(
            onSuccess = { it },
            onFailure = { throw FaultCauseConversionFailed(it) }
        )
        return ApiError(code, message)
    }
}

class FaultCauseConversionFailed(throwable: Throwable) : RestXException(throwable)

/**
 * Interface for fault code providers
 */
fun interface FaultCodeProvider {
    /**
     * Method returning code for given fault cause
     *
     * @throws FaultCodeProvidingFailure in case code for given fault cannot be provided
     */
    fun codeFor(fault: FaultCause): String
}

class FaultCodeProvidingFailure(message: String) : RestXException(message)

/**
 * Provider returning fixed code for any fault cause
 */
class FixedFaultCodeProvider(private val code: String) : FaultCodeProvider {
    override fun codeFor(fault: FaultCause): String = code
}

/**
 * Provider returning code based on fault id from predefined map
 */
class MapBasedFaultCodeProvider(private val mapping: Map<String, String>) : FaultCodeProvider {
    init {
        require(mapping.isNotEmpty()) { "Fault code mappings not provided" }
    }

    override fun codeFor(fault: FaultCause): String {
        return mapping[fault.id]
            ?: throw FaultCodeProvidingFailure("None code mapping found for id '${fault.id}'")
    }
}

fun interface FaultMessageProvider {
    fun messageFor(fault: FaultCause): String
}

/**
 * Provider returning fixed message for any fault cause
 */
class FixedFaultMessageProvider(private val message: String) : FaultMessageProvider {
    override fun messageFor(fault: FaultCause): String = message
}

/**
 * Provider returning message based on fault id from predefined map
 */
class MapBasedFaultMessageProvider(private val mapping: Map<String, String>) : FaultMessageProvider {
    init {
        require(mapping.isNotEmpty()) { "Fault message mappings not provided" }
    }

    override fun messageFor(fault: FaultCause): String {
        return mapping[fault.id]
            ?: throw FaultCodeProvidingFailure("None message mapping found for id '${fault.id}'")
    }
}
