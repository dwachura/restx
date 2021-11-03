import org.gradle.kotlin.dsl.DependencyHandlerScope

internal fun DependencyHandlerScope.logging() = also {
    add("implementation", "io.github.microutils:kotlin-logging-jvm:2.1.21")
    add("testRuntimeOnly", "ch.qos.logback:logback-classic:1.2.10")
}

internal fun DependencyHandlerScope.kotest() = also {
    add("testImplementation", "io.kotest:kotest-runner-junit5:5.0.3")
    add("testImplementation", "io.kotest:kotest-assertions-core:5.0.3")
}

internal fun DependencyHandlerScope.mockk() = also {
    add("testImplementation", "io.mockk:mockk:1.12.2")
}
