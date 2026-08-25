import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.openjfx.gradle.JavaFXModule
import org.openjfx.gradle.JavaFXOptions
import org.openjfx.gradle.JavaFXPlatform
import java.io.File

plugins {
    alias(libs.plugins.javafx)
    alias(libs.plugins.conveyor)
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    application
}

repositories {
    mavenCentral()
}

version = "0.4.3"

val javaVersion = libs.versions.java.get()

allprojects {
    group = "stan.qodat"
    apply(plugin = "org.jetbrains.kotlin.jvm")
    repositories {
        maven(url = "https://repo.runelite.net")
        mavenCentral()
    }
    java {
        toolchain.languageVersion.set(JavaLanguageVersion.of(javaVersion))
    }
    sourceSets {
        named("main") {
            java.srcDir("src/main/kotlin")
        }
    }
    tasks {
        withType<KotlinCompile>().configureEach {
            compilerOptions.jvmTarget.set(JvmTarget.fromTarget(javaVersion))
        }

        jar {
            duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        }

        withType<Test>().configureEach {
            // Separate JVMs; JavaFX tests that start JFXPanel stay isolated.
            maxParallelForks = (Runtime.getRuntime().availableProcessors() / 2).coerceAtLeast(1)
        }
    }
}

application {
    mainClass.set("stan.qodat.Launcher")
    applicationName = "Qodat"
}

tasks {
    processResources {
        val appVersion = version.toString()
        inputs.property("appVersion", appVersion)
        filesMatching("stan/qodat/version.properties") {
            filter { line ->
                if (line.startsWith("version=")) "version=$appVersion" else line
            }
        }
    }
    jar {
        manifest {
            attributes(
                "Implementation-Title" to "Qodat",
                "Implementation-Version" to version
            )
        }
    }
}

javafx {
    version = libs.versions.javafx.get()
    modules(
        "javafx.controls",
        "javafx.fxml",
        "javafx.media",
        "javafx.swing"
    )
}

val javaFXOptions = the<org.openjfx.gradle.JavaFXOptions>()

// OpenJFX 0.1.0 only patches `:run`. IntelliJ's Application config launches
// `:stan.qodat.Launcher.main()`, which needs the same --module-path treatment.
tasks.withType<JavaExec>().configureEach {
    // Skip `:run` so we do not stack a second, empty --module-path on the plugin's doFirst.
    if (name == "run" || mainModule.isPresent) {
        return@configureEach
    }
    val execTask = this
    doFirst {
        putJavaFXJarsOnModulePathForClasspathApplication(execTask, javaFXOptions)
    }
}

dependencies {
    implementation(project("qodat-api"))
    implementation(libs.gson)
    implementation(libs.guava)
    implementation(libs.commons.compress)
    implementation(libs.jsoup)
    implementation(libs.ihmc.javafx.toolkit)
    implementation(libs.jcodec)
    implementation(libs.jcodec.javase)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.javafx)
    implementation(libs.kotlinx.datetime)
    implementation(libs.poly2tri.core)
    implementation(libs.logback.classic)
    implementation(libs.rs.cache.library)
    implementation(libs.disio)
    implementation(libs.joml.primitives)
    implementation(libs.joml)
    implementation(libs.tornadofx) {
        exclude(group = "org.openjfx")
    }
    implementation(libs.conveyor.control)
    testImplementation(kotlin("test-junit"))
    testImplementation(libs.runelite.cache) {
        exclude(group = "com.google.guava")
    }
}

/**
 * Mirrors [org.openjfx.gradle.JavaFXPlugin] so IntelliJ-generated `*.main()` JavaExec
 * tasks get JavaFX on `--module-path` the same way `:run` does.
 */
fun putJavaFXJarsOnModulePathForClasspathApplication(execTask: JavaExec, javaFXOptions: JavaFXOptions) {
    val platform = javaFXOptions.platform
    val classpath = execTask.classpath
    val fxJars = classpath.filter { isJavaFXJar(it, platform) }
    if (fxJars.isEmpty) {
        return
    }
    execTask.classpath = classpath.filter { !isJavaFXJar(it, platform) }
    @Suppress("UNCHECKED_CAST")
    val modules = (javaFXOptions.modules as List<String>).joinToString(",")
    execTask.jvmArgumentProviders.add {
        listOf("--module-path", fxJars.asPath, "--add-modules", modules)
    }
}

fun isJavaFXJar(jar: File, platform: JavaFXPlatform): Boolean =
    jar.isFile && JavaFXModule.values().any { module ->
        module.compareJarFileName(platform, jar.name) || module.moduleJarFileName == jar.name
    }

tasks.register<JavaExec>("downloadLatestOsrsCache") {
    group = "verification"
    description = "Download the latest archive.runestats.com OSRS cache (not part of test)"
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("stan.qodat.cache.OsrsCacheArchive")
    val dest = providers.gradleProperty("qodat.cache.dest")
        .orElse(providers.systemProperty("qodat.cache.dest"))
        .orElse(layout.buildDirectory.dir("osrs-cache").map { it.asFile.absolutePath })
    args(dest.get())
}

tasks.register<Test>("cacheSmoke") {
    group = "verification"
    description = "Decode a sample of each type from a real OSRS cache"
    testClassesDirs = sourceSets.test.get().output.classesDirs
    classpath = sourceSets.test.get().runtimeClasspath
    filter {
        includeTestsMatching("stan.qodat.cache.LatestOsrsCacheSmokeTest")
    }
    // Resolve here: Test.systemProperty(String, Object) stringifies the value.
    // Passing the Provider itself made the JVM see
    // "or(or(or(provider(?), ...), fixed()))" instead of the cache path.
    val cacheDir = providers.gradleProperty("qodat.cache.dir")
        .orElse(providers.systemProperty("qodat.cache.dir"))
        .orElse(providers.environmentVariable("QODAT_CACHE_DIR"))
        .orElse("")
    systemProperty("qodat.cache.dir", cacheDir.get())
    systemProperty("qodat.cache.required", "true")
}
