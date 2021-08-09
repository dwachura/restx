plugins {
    kotlin("jvm")
    kotlin("kapt")
    `java-library`
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
}

dependencies {
    implementation("com.google.auto.service:auto-service:1.0")
    kapt("com.google.auto.service:auto-service:1.0")

    implementation("com.squareup:kotlinpoet:1.9.0")

    kaptTest(project(":core:annotations"))

    testImplementation("io.kotest:kotest-runner-junit5:4.6.0")
    testImplementation("io.kotest:kotest-assertions-core:4.6.0")
    testImplementation("io.mockk:mockk:1.11.0")
}

tasks.test {
    useJUnitPlatform()
}
