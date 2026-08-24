
rootProject.name = "qodat"

include("qodat-api")
include("qodat-bench")
//include("qodat-launcher")
//include("qodat-launcher:buildSrc")
require(file("qodat-cache/settings.gradle.kts").isFile) {
    "qodat-cache submodule is not checked out. Run: git submodule update --init"
}
includeBuild("qodat-cache") {
    dependencySubstitution {
        substitute(module("com.displee:rs-cache-library")).using(project(":"))
    }
}