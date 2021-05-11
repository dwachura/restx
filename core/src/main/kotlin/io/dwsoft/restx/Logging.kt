package io.dwsoft.restx

import mu.KLogger
import mu.KotlinLogging
import kotlin.reflect.KClass

internal inline fun <reified T> T.initLog(): KLogger = T::class.qualifiedName.toLogger()

private fun String?.toLogger(): KLogger {
    require(this != null) { "logger id must not be null" }
    return KotlinLogging.logger(this)
}

internal inline fun <reified T : KClass<*>> T.initLog(): KLogger = this.qualifiedName.toLogger()
