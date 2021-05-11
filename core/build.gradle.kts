plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
}

//repositories {
//    maven { url = uri("https://jitpack.io") }
//}

dependencies {
    implementation("io.github.microutils:kotlin-logging-jvm:2.0.6")
    runtimeOnly("ch.qos.logback:logback-classic:1.2.3")

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.2.1")
//    implementation("com.github.everit-org:json-schema:1.12.2")
//    testImplementation(kotlin("test-junit5"))
//    testImplementation("org.junit.jupiter:junit-jupiter:5.7.1")
//    testImplementation("org.assertj:assertj-core:3.19.0")
    testImplementation("io.kotest:kotest-runner-junit5:4.6.0")
    testImplementation("io.kotest:kotest-assertions-core:4.6.0")
    testImplementation("io.mockk:mockk:1.11.0")
}

tasks.test {
    useJUnitPlatform()
}

