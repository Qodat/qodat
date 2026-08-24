// Isolated decode harness. Not on the desktop classpath.
// `./gradlew test` stays unit tests: this module has no src/test and :test is disabled.
// Do not add JMH / kotlinx-benchmark here — qodat-cache already pins an older plugin.

tasks.named("test") {
    enabled = false
    setDependsOn(emptyList<Any>())
}

dependencies {
    implementation(rootProject)
    implementation(libs.disio)
    implementation(libs.runelite.cache) {
        exclude(group = "com.google.guava")
    }
}

tasks.register<JavaExec>("benchDecode") {
    group = "benchmark"
    description = "Synthetic decode speed vs RuneLite (not part of test)"
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("stan.qodat.bench.DecodeBenchmarkKt")
}
