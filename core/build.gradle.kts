plugins {
    kotlin("jvm")
}

dependencies {
    implementation("io.github.microutils:kotlin-logging-jvm:2.0.6")
    runtimeOnly("ch.qos.logback:logback-classic:1.2.3")

    testImplementation("io.kotest:kotest-runner-junit5:4.6.0")
    testImplementation("io.kotest:kotest-assertions-core:4.6.0")
    testImplementation("io.mockk:mockk:1.11.0")
}

tasks.test {
    useJUnitPlatform()
}
