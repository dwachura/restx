package io.dwsoft.restx.codegen.javaextgeneration

import io.dwsoft.restx.codegen.javaextgeneration.GenerateJavaDelegate.ParamMapping
import io.dwsoft.restx.codegen.javaextgeneration.GenerateJavaDelegate.ParamMapping.ParamDef
import io.dwsoft.restx.codegen.javaextgeneration.GenerateJavaDelegate.ParamMapping.TypeDef
import io.kotest.core.spec.style.FunSpec
import kotlin.Function1

class KotlinSpecificFunctionCallProvider : DelegateCallProvider {
    override fun get(): Any = { fake: FakeKotlinClass, javaFactory: JavaFriendlyFactory<String, Int> ->
        fake.kotlinSpecificFunction { javaFactory.invoke(it) }
    }
}

class FakeKotlinClass {
    @GenerateJavaDelegate(
        paramMappings = [
            ParamMapping(
                source = ParamDef(
                    name = "kotlinFactory",
                    type = TypeDef(
                        rawType = Function1::class,
                        typeParameters = [TypeDef(rawType = String::class), TypeDef(rawType = Int::class)]
                    )
                ),
                target = ParamDef(
                    name = "javaFactory",
                    type = TypeDef(
                        rawType = JavaFriendlyFactory::class,
                        typeParameters = [TypeDef(rawType = String::class), TypeDef(rawType = Int::class)]
                    )
                )
            )
        ],
        delegateCallProviderType = KotlinSpecificFunctionCallProvider::class
    )
    fun kotlinSpecificFunction(kotlinFactory: (String) -> Int) {}

    companion object {
//        @GenerateJavaExtension
        fun companionFun() {}
    }
}

//fun FakeKotlinClass.kotlinSpecificFunction(javaFactory: JavaFriendlyFactory<String, Int>) {
//    kotlinSpecificFunction { javaFactory.invoke(it) }
//}

fun interface JavaFriendlyFactory<T, R> {
    operator fun invoke(factories: T): R
}

class GenerateJavaExtensionProcessorTests : FunSpec({
})
