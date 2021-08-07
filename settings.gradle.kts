rootProject.name = "restx"

include("core")

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }

    plugins {
        kotlin("jvm") version "1.5.0" apply false
    }
}