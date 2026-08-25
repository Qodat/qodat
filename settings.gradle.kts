rootProject.name = "qodat"

include("qodat-api")

// Bench depends on the desktop runtimeClasspath. Do not configure it on
// `./gradlew test` / CI. Include when a bench task is named, when the
// property is set, or during an IntelliJ sync so the module still appears.
val includeBench =
    System.getProperty("idea.sync.active") == "true" ||
        settings.startParameter.taskNames.any { name ->
            name.contains("qodat-bench", ignoreCase = true) ||
                name.contains("benchDecode", ignoreCase = true) ||
                name.contains("benchListWrap", ignoreCase = true) ||
                name.contains("benchSpriteList", ignoreCase = true) ||
                name.contains("benchSkeleton", ignoreCase = true)
        } ||
        providers.gradleProperty("qodat.includeBench").orElse("false").get() == "true"
if (includeBench) {
    include("qodat-bench")
}

require(file("qodat-cache/settings.gradle.kts").isFile) {
    "qodat-cache submodule is not checked out. Run: git submodule update --init"
}
includeBuild("qodat-cache") {
    dependencySubstitution {
        substitute(module("com.displee:rs-cache-library")).using(project(":"))
    }
}
