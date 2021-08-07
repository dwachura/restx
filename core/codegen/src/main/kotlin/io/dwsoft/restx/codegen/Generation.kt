package io.dwsoft.restx.codegen

import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import kotlin.reflect.KClass

fun createFile(packageName: String, fileName: String) = FileSpec.builder(packageName, fileName)

fun <T : Any> createExtensionFunction(name: String, receiverType: KClass<T>) =
    FunSpec.builder(name).receiver(receiverType)