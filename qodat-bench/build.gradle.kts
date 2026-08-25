// Isolated decode harness. Not on the desktop classpath.
// Included only when a bench* task is requested, -Pqodat.includeBench=true,
// or an IntelliJ sync (see settings.gradle.kts). Do not add JMH /
// kotlinx-benchmark here — qodat-cache already pins an older plugin.

tasks.named("test") {
    enabled = false
    setDependsOn(emptyList<Any>())
}

dependencies {
    implementation(project(":"))
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

tasks.register<JavaExec>("benchSkeleton") {
    group = "benchmark"
    description = "Framemap / frame / Maya skeleton decode (not part of test)"
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("stan.qodat.bench.SkeletonBenchmarkKt")
}
