import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
//    application
    java
    kotlin("jvm") version "1.4.0"
//    id("org.openjfx.javafxplugin") version "0.0.9"
}
group = "me.31619"
version = "1.0-SNAPSHOT"

repositories {
    maven(url = "https://jitpack.io")
    mavenCentral()
    jcenter()
}

dependencies {
    implementation(group = "com.github.runelite.runelite", name = "cache", version = "runelite-parent-1.5.2.1")
    testImplementation(kotlin("test-junit"))
}

//application {
//    mainClassName = "stan.qodat.Qodat"
//}

//javafx {
//    version = "11"
//    modules(
//        "javafx.base",
//        "javafx.controls",
//        "javafx.fxml",
//        "javafx.graphics",
//        "javafx.media",
//        "javafx.swing")
//}
sourceSets {
    named("main") {
        java.srcDir("src/main/kotlin")
    }
}
tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}