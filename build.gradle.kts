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

version = "0.4.1"

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
    }
}

application {
    mainClass.set("stan.qodat.Launcher")
    applicationName = "Qodat"
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
    implementation(libs.joml.primitives)
    implementation(libs.joml)
    implementation(libs.tornadofx) {
        exclude(group = "org.openjfx")
    }
    implementation(libs.xmlutil.serialization)
    testImplementation(kotlin("test-junit"))
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
