
rootProject.name = "qodat"

include("qodat-api")
//include("qodat-launcher")
//include("qodat-launcher:buildSrc")
includeBuild("qodat-cache") {
    dependencySubstitution {
        substitute(module("com.displee:rs-cache-library")).using(project(":"))
    }
}