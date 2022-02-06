rootProject.name = "restx"

gradle.rootProject {
    group = "io.dwsoft.$name"
    version = "0.1.0-SNAPSHOT"
}

include(
    "core",
    "core:samples",
    "spring-5"
)
