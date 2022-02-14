plugins {
    id("restx-subproject-conventions")
}

dependencies {
    implementation(project(":core"))
    runtimeOnly("org.slf4j:slf4j-nop:1.7.36")
}
