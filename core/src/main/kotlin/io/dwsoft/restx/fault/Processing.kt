package io.dwsoft.restx.fault

import io.dwsoft.restx.RestXException
import io.dwsoft.restx.initLog
import io.dwsoft.restx.payload.ApiError

/**
 * Interface for processors of [FaultCauseId] that convert them into [ApiError] objects.
 *
 * @param T type of fault object that cause info is created for
 */
fun interface FaultCauseProcessor<T : FaultResult> {
    /**
     * Method responsible for converting [FaultCauseId] into corresponding [ApiError].
     *
     * @throws RestXException in case of processing failure
     */
    fun process(causeId: FaultCauseId<T>): ApiError
}

/**
 * Factory method that returns default implementation of [FaultCauseProcessor].
 * Returned instance throws [FaultCauseProcessingFailed] in case of code or message
 * provider fails.
 *
 * @param T type of fault object which causes can be processed by created object
 * @param codeProvider provider of error codes
 * @param messageProvider provider of error messages
 */
fun <T : FaultResult> FaultCauseProcessor(
    codeProvider: FaultCodeProvider<T>,
    messageProvider: FaultMessageProvider<T>
) = object : FaultCauseProcessor<T> {
    private val log = FaultCauseProcessor::class.initLog()

    override fun process(causeId: FaultCauseId<T>): ApiError {
        log.info { "Processing cause $causeId" }
        val (code, message) = runCatching {
            Pair(
                codeProvider.codeFor(causeId),
                messageProvider.messageFor(causeId)
            )
        }.fold(
            onSuccess = { it },
            onFailure = { throw FaultCauseProcessingFailed(it) }
        )
        return ApiError(code, message)
    }
}

class FaultCauseProcessingFailed(throwable: Throwable) : RestXException(throwable)

/**
 * Interface for fault code providers.
 *
 * @param T type of fault object which causes are supported by created object
 */
fun interface FaultCodeProvider<T : FaultResult> {
    /**
     * Method returning code for given [FaultCauseId].
     *
     * @throws FaultCodeProvisioningFailure in case code for given fault cannot be provided
     */
    fun codeFor(causeId: FaultCauseId<T>): String
}

class FaultCodeProvisioningFailure(message: String) : RestXException(message)

/**
 * Implementation of [FaultCodeProvider] returning fixed code for any fault cause.
 */
class FixedFaultCodeProvider(private val code: String) : FaultCodeProvider<Any> {
    override fun codeFor(causeId: FaultCauseId<Any>): String = code
}

/**
 * Implementation of [FaultCodeProvider] returning code based on fault id from predefined map.
 *
 * @param mapping <fault cause id>:<fault code> map
 */
class MapBasedFaultCodeProvider(private val mapping: Map<String, String>) : FaultCodeProvider<Any> {
    init {
        require(mapping.isNotEmpty()) { "Fault code mappings not provided" }
    }

    override fun codeFor(causeId: FaultCauseId<Any>): String {
        return mapping[causeId.id]
            ?: throw FaultCodeProvisioningFailure("None code mapping found for id '${causeId.id}'")
    }
}

/**
 * Interface of fault message providers.
 *
 * @param T type of fault object which causes are supported by created object
 */
fun interface FaultMessageProvider<T : FaultResult> {
    /**
     * Method returning message for given [FaultCauseId].
     *
     * @throws FaultMessageProvisioningFailure in case message for given fault cannot be provided
     */
    fun messageFor(causeId: FaultCauseId<T>): String
}

class FaultMessageProvisioningFailure(message: String) : RestXException(message)

/**
 * Implementation of [FaultMessageProvider] returning fixed message for any fault cause.
 */
class FixedFaultMessageProvider(private val message: String) : FaultMessageProvider<Any> {
    override fun messageFor(causeId: FaultCauseId<Any>): String = message
}

/**
 * Implementation of [FaultMessageProvider] returning message based on fault id from predefined map.
 *
 * @param mapping <fault cause id>:<fault message> map
 */
class MapBasedFaultMessageProvider(private val mapping: Map<String, String>) : FaultMessageProvider<Any> {
    init {
        require(mapping.isNotEmpty()) { "Fault message mappings not provided" }
    }

    override fun messageFor(causeId: FaultCauseId<Any>): String {
        return mapping[causeId.id]
            ?: throw FaultMessageProvisioningFailure("None message mapping found for id '${causeId.id}'")
    }
}
