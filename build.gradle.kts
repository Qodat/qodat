plugins {
    id("org.openjfx.javafxplugin") version "0.0.11"
    kotlin("jvm") version "1.7.10"
    kotlin("plugin.serialization") version "1.6.0"
    application
}

repositories {
    jcenter()
}

allprojects {
    group = "stan.qodat"
    version = "0.0.6"
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
    }
}

application {
    mainClass.set("stan.qodat.Launcher")
    applicationName = "Qodat"
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
    implementation(project("qodat-api"))
    implementation("org.jsoup:jsoup:1.15.2")
    implementation("us.ihmc:ihmc-javafx-toolkit:17-0.21.2")
    implementation("org.jcodec:jcodec:0.2.5")
    implementation("org.jcodec:jcodec-javase:0.2.5")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.4.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-javafx:1.6.4")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.4.0")
    implementation("org.orbisgis:poly2tri-core:0.1.2")
    implementation("com.displee:rs-cache-library:6.8.1")
    implementation("org.joml:joml-primitives:1.10.0")
    implementation("org.joml:joml:1.10.2")
    implementation("no.tornado:tornadofx:1.7.20")
    testImplementation(kotlin("test-junit"))
}
