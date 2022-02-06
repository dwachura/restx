plugins {
    id("restx-subproject-conventions")
    kotlin("plugin.spring") version "1.6.10"
}

dependencies {
    implementation("io.dwsoft.restx:restx-core:${project.version}")

    compileOnly(platform("org.springframework:spring-framework-bom:5.3.15"))
    compileOnly("org.springframework:spring-web")
    compileOnly("javax.servlet:javax.servlet-api:4.0.1")

    testImplementation("io.kotest.extensions:kotest-extensions-spring:1.1.0")
    testImplementation(platform("org.springframework.boot:spring-boot-dependencies:2.6.2"))
    testImplementation("org.springframework.boot:spring-boot-starter-web")
    testImplementation("org.springframework.boot:spring-boot-starter-test")

//    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
//    implementation("org.jetbrains.kotlin:kotlin-reflect")
}

tasks.withType<Test> {
    useJUnitPlatform()
}
