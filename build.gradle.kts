import org.openjfx.gradle.JavaFXModule
import org.openjfx.gradle.JavaFXOptions
import org.openjfx.gradle.JavaFXPlatform
import java.io.File

plugins {
    id("org.openjfx.javafxplugin") version "0.1.0"
    id("dev.hydraulic.conveyor") version "1.5"
    kotlin("jvm") version "1.9.10"
    kotlin("plugin.serialization") version "1.9.10"
    application
}

repositories {
    jcenter()
}

version = "0.3.3"

allprojects {
    group = "stan.qodat"
    apply(plugin = "org.jetbrains.kotlin.jvm")
    repositories {
        maven(url = "https://repo.runelite.net")
        mavenCentral()
    }
    java {
        toolchain.languageVersion.set(JavaLanguageVersion.of(17))
    }
    sourceSets {
        named("main") {
            java.srcDir("src/main/kotlin")
        }
    }
    tasks {
        withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
            kotlinOptions.jvmTarget = "17"
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
    version = "17.0.16"
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
    implementation("com.google.code.gson:gson:2.8.5")
    implementation("com.google.guava:guava:23.2-jre")
    implementation("org.apache.commons:commons-compress:1.10")
    implementation("org.jsoup:jsoup:1.17.2")
    implementation("us.ihmc:ihmc-javafx-toolkit:17-0.21.2")
    implementation("org.jcodec:jcodec:0.2.5")
    implementation("org.jcodec:jcodec-javase:0.2.5")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-javafx:1.8.0")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.5.0")
    implementation("org.orbisgis:poly2tri-core:0.1.2")
    implementation(group = "ch.qos.logback", name = "logback-classic", version = "1.2.9")
    implementation("com.displee:rs-cache-library:7.1.3")
    implementation("org.joml:joml-primitives:1.10.0")
    implementation("org.joml:joml:1.10.5")
    implementation("no.tornado:tornadofx:1.7.20")
    implementation("io.github.pdvrieze.xmlutil:serialization-jvm:0.86.3")
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
