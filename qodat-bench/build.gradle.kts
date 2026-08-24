// Isolated decode harness. Not on the desktop classpath.
// `./gradlew test` stays unit tests: this module has no src/test and is skipped
// unless a :qodat-bench:* or benchDecode task is requested.
// Do not add JMH / kotlinx-benchmark here — qodat-cache already pins an older plugin.

tasks.named("test") {
    enabled = false
    setDependsOn(emptyList<Any>())
}

gradle.taskGraph.whenReady {
    val benchRequested = gradle.startParameter.taskNames.any {
        it.contains("qodat-bench", ignoreCase = true) ||
            it.contains("benchDecode", ignoreCase = true) ||
            it.contains("benchListWrap", ignoreCase = true) ||
            it.contains("benchSpriteList", ignoreCase = true)
    }
    if (!benchRequested) {
        tasks.matching { it.name != "clean" }.configureEach {
            enabled = false
        }
    }
}

dependencies {
    implementation(rootProject)
    implementation("com.displee:rs-cache-library")
    implementation(libs.kotlinx.coroutines.core)
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

tasks.register<JavaExec>("benchSpriteList") {
    group = "benchmark"
    description = "Index-only sprite list vs full archive decompress (not part of test)"
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("stan.qodat.bench.SpriteListBenchmarkKt")
}

tasks.register<JavaExec>("benchListWrap") {
    group = "benchmark"
    description = "Chunked cache-list wrap vs parallel stream (not part of test)"
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("stan.qodat.bench.ListWrapBenchmarkKt")
}
