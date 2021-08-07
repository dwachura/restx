rootProject.name = "restx"

include(
    "core", "core:codegen"
)

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }

    plugins {
        kotlin("jvm") version "1.5.21" apply false
        kotlin("kapt") version "1.5.21" apply false
    }
}