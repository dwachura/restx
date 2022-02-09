import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    `maven-publish` apply false
}

group = "${parent!!.group}"
version = rootProject.version

repositories {
    mavenLocal()
    mavenCentral()
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {
    logging()
}

findProperty("restx.testing.disabled") ?: run {
    dependencies {
        kotest()
        mockk()
    }

    tasks.test {
        useJUnitPlatform()
    }
}

findProperty("restx.maven.publishing.disabled") ?: run {
    apply { plugin("org.gradle.maven-publish") }

    val artifactName = "${rootProject.name}-${project.name}"

    publishing {
        publications {
            create<MavenPublication>("maven") {
                artifactId = artifactName
                from(components["java"])
            }
        }
    }

    tasks.jar {
        archiveBaseName.set(artifactName)
    }
}
