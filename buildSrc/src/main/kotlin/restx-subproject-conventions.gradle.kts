import org.gradle.kotlin.dsl.repositories

plugins {
    kotlin("jvm")
}

group = "${parent!!.group}.$name"
version = rootProject.version

repositories {
    gradlePluginPortal()
    mavenCentral()
}

dependencies {
    implementation("io.github.microutils:kotlin-logging-jvm:2.0.11")
    runtimeOnly("ch.qos.logback:logback-classic:1.2.6")
}
