plugins {
    kotlin("jvm")
    `maven-publish`
}

group = "${parent!!.group}"
version = rootProject.version

val artifactName = "${rootProject.name}-${project.name}"

publishing {
    publications {
        create<MavenPublication>("maven") {
            artifactId = artifactName
            from(components["java"])
        }
    }
}

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    logging()
    kotest()
    mockk()
}

tasks.jar {
    archiveBaseName.set(artifactName)
}

tasks.test {
    useJUnitPlatform()
}
