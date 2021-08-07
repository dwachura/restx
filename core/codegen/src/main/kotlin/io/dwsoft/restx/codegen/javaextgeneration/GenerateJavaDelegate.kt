package io.dwsoft.restx.codegen.javaextgeneration

import kotlin.reflect.KClass

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
annotation class GenerateJavaDelegate(
    val paramMappings: Array<ParamMapping>,
    val delegateCallProviderType: KClass<out DelegateCallProvider> = DelegateCallProvider::class
) {
    annotation class ParamMapping(val source: ParamDef, val target: ParamDef) {
        annotation class ParamDef(val name: String, val type: TypeDef)
        annotation class TypeDef(val rawType: KClass<*>, val typeParameters: Array<TypeDef> = [])
    }
}

/**
 * Defines how delegate should be called from generated function.
 * During code generation code of object of this type will be created, and it will be called into generated function body.
 *
 * See [io.dwsoft.restx.codegen.javaextgeneration.KotlinSpecificFunctionCallProvider]
 * In this example generated code would look like:
 *
 * object KotlinSpecificFunctionCallProviderObject : KotlinSpecificFunctionCallProvider()
 *
 * fun FakeKotlinClass.kotlinSpecificFunction(javaFactory: JavaFriendlyFactory<String, Int>) {
 *     KotlinSpecificFunctionCallProviderObject.get()(this, javaFactory)
 * }
 */
interface DelegateCallProvider {
    fun get(): Any
}