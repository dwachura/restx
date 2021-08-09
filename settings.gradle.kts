rootProject.name = "restx"

include(
    "core", "core:annotations"
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