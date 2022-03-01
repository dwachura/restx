plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
    mavenCentral()
}

val kotlinVersion : String by project
val kotlinterVersion : String by project
dependencies {
    implementation(kotlin("gradle-plugin", kotlinVersion))
    implementation("org.jmailen.gradle:kotlinter-gradle:$kotlinterVersion")
}
