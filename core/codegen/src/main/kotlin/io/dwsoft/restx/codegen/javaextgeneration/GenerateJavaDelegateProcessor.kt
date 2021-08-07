package io.dwsoft.restx.codegen.javaextgeneration

import com.google.auto.service.AutoService
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.asTypeName
import io.dwsoft.restx.codegen.createExtensionFunction
import io.dwsoft.restx.codegen.createFile
import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.Processor
import javax.annotation.processing.RoundEnvironment
import javax.annotation.processing.SupportedSourceVersion
import javax.lang.model.SourceVersion
import javax.lang.model.element.ElementKind
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.PackageElement
import javax.lang.model.element.TypeElement

@AutoService(Processor::class)
@SupportedSourceVersion(SourceVersion.RELEASE_8)
class GenerateJavaDelegateProcessor : AbstractProcessor() {
    private val supportedAnnotation = GenerateJavaDelegate::class.java
    private val supportedAnnotationName = supportedAnnotation.name

    override fun getSupportedAnnotationTypes(): MutableSet<String> = mutableSetOf(supportedAnnotationName)

    override fun process(annotations: MutableSet<out TypeElement>, roundEnv: RoundEnvironment): Boolean {
        roundEnv.getElementsAnnotatedWith(supportedAnnotation)
            .filter { ElementKind.METHOD == it.kind }
            .map { it as ExecutableElement }
            .forEach { process(it) }
//            .map { FunctionInfo(
//                processingEnv.elementUtils.getPackageOf(it),
//                it.enclosingElement as TypeElement,
//                it
//            ) }
//            .toList()
//        println(toList)
        return false
    }

    private fun process(function: ExecutableElement) {
        val packageElement = processingEnv.elementUtils.getPackageOf(function)
        val functionClass = function.enclosingElement as TypeElement

        val packageName = packageElement.qualifiedName.toString()

        val delegateName = function.simpleName

        function.getAnnotation(GenerateJavaDelegate::class.java).paramMappings
            .map {  }


        val file = createFile(packageName, functionClass.qualifiedName.toString().removePrefix(packageName))
            .addFunction(
                createExtensionFunction("javaFactory", String::class)
                    .addParameter("factory", Int::class)
                    .returns(String::class)
                    .addCode(CodeBlock.builder()
                        .addStatement("$delegateName { factory(this) }")
                        .build())
                    .build()
            )

        println("-------- FILE ------------")
        println(file.build().writeTo(System.out))
    }
}

private class FunctionInfo(
    val packageElement: PackageElement,
    val classElement: TypeElement,
    functionElement: ExecutableElement
) {
    val name = functionElement.simpleName.toString()
    val parameters = functionElement.parameters.map { it.asType() }

    override fun toString(): String {
        return "${FunctionInfo::class.simpleName}[" +
                "package = ${packageElement.qualifiedName}, " +
                "class = ${classElement.qualifiedName}, " +
                "name = $name, " +
                "params = ${parameters.joinToString { it.asTypeName().toString() }}" +
                "]"
    }
}