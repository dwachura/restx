plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
    mavenCentral()
}

val kotlinVersion : String by project
dependencies {
    implementation(kotlin("gradle-plugin", kotlinVersion))
}
