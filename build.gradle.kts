import org.gradle.internal.os.OperatingSystem

plugins {
    id("org.openjfx.javafxplugin") version "0.0.11"
    id("org.beryx.runtime") version "1.12.7"
    kotlin("jvm") version "1.6.0"
    kotlin("plugin.serialization") version "1.6.0"
    application
}

group = "stan.qodat"
version = "0.0.9"

repositories {
    maven(url = "https://repo.runelite.net")
    maven(url = "https://jitpack.io")
    mavenCentral()
    jcenter()
}

javafx {
    version = "17.0.2"
    modules(
        "javafx.controls",
        "javafx.fxml",
        "javafx.media",
        "javafx.swing"
    )
}

val javaFXOptions = the<org.openjfx.gradle.JavaFXOptions>()

dependencies {
    implementation(group = "net.runelite", name = "cache", version = "1.8.9") {
        exclude(group = "com.google.common")
    }
    implementation(project("qodat-api"))
    implementation("org.jsoup:jsoup:1.14.3")
    implementation("us.ihmc:ihmc-javafx-toolkit:0.20.0")
    implementation("org.jcodec:jcodec:0.2.5")
    implementation("org.jcodec:jcodec-javase:0.2.5")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.3.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-javafx:1.6.0")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.3.1")
    implementation("org.orbisgis:poly2tri-core:0.1.2")
    implementation("com.displee:rs-cache-library:6.8.1")
    implementation("org.joml:joml-primitives:1.10.0")
    implementation("org.joml:joml:1.10.2")
    testImplementation(kotlin("test-junit"))
}

sourceSets {
    named("main") {
        java.srcDir("src/main/kotlin")
    }
}

application {
    mainClass.set("stan.qodat.Launcher")
    applicationName = "Qodat"
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(17))
}
runtime {

    imageZip.set(project.file("${buildDir}/distributions/app-${javafx.platform.classifier}.zip"))

    options.set(listOf("--strip-debug", "--compress", "2", "--no-header-files", "--no-man-pages"))

    modules.set(listOf("java.desktop", "jdk.unsupported", "java.scripting", "java.logging", "java.xml", "java.naming", "java.sql"))

    targetPlatform("win", "/Users/stanvanderbend/Documents/jdk-17.0.2")

    jpackage {
        appVersion = "1.0.0"

        val os = OperatingSystem.current()
        when {
            os.isWindows -> {
                installerType = "msi"
                installerOptions = listOf(
                    "--win-per-user-install",
                    "--win-dir-chooser",
                    "--win-menu",
                    "--win-shortcut"
                )
                imageOptions = listOf("--icon", "src/main/resources/stan/qodat/images/icon.png")
            }
            os.isMacOsX -> {
                imageOptions = listOf("--icon", "src/main/resources/stan/qodat/images/icon.icns")
                installerOptions = listOf(
                    "--mac-package-name", "Qodat",
                )
            }
        }
    }
}

tasks {
    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions.jvmTarget = "17"
    }
}