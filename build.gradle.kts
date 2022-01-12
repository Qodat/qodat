import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
//    id("org.openjfx.javafxplugin") version "0.0.10"
    kotlin("jvm") version "1.6.0"
    kotlin("plugin.serialization") version "1.6.0"
    application
}

group = "stan.qodat"
version = "0.0.5"

repositories {
    maven(url = "https://repo.runelite.net")
    maven(url = "https://jitpack.io")
    mavenCentral()
    jcenter()
}

dependencies {
    implementation(group = "net.runelite", name = "cache", version = "1.8.0") {
        exclude(group = "com.google.common")
    }
//    implementation("no.tornado:tornadofx:1.7.19")
//    implementation("com.github.kotlin-graphics:gln:v0.5.2")
    implementation("us.ihmc:ihmc-javafx-toolkit:0.20.0")
    implementation("org.jcodec:jcodec:0.2.5")
    implementation("org.jcodec:jcodec-javase:0.2.5")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.3.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.0-RC2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-javafx:1.6.0-RC2")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.3.1")
    implementation("org.orbisgis:poly2tri-core:0.1.2")
    implementation("com.displee:rs-cache-library:6.8.1")
    implementation("org.joml:joml-primitives:1.10.0")
    implementation("org.joml:joml:1.10.2")
//    implementation("com.github.kotlin-graphics:gln:v0.5.2")
    testImplementation(kotlin("test-junit"))
}
//javafx {
//    version = "14"
//    modules("javafx.controls", "javafx.fxml")
//    configuration = "compileOnly"
//}
//javafx {
//    version = JavaVersion.VERSION_11.toString()
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
application {
//    mainModule.set("stan.qodat")
    mainClass.set("stan.qodat.Qodat")
}
//jlink{
//    launcher {
//        name = "Qodat $version"
//    }
//    mergedModule {
//        excludeUses("com.google.common.base.PatternCompiler")
//    }
//    imageZip.set(project.file("${project.buildDir}/image-zip/qodat-$version.zip"))
//}

//
//java {
//    sourceCompatibility = JavaVersion.VERSION_11
//    targetCompatibility = JavaVersion.VERSION_11
//}

tasks.withType<Jar> {
    manifest {
        attributes("Main-Class" to "stan.qodat.Qodat")
    }
    from(sourceSets.main.get().output)
    dependsOn(configurations.runtimeClasspath)
    from({
        configurations.runtimeClasspath.get().filter { it.name.endsWith("jar") }.map { zipTree(it) }
    })
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = JavaVersion.VERSION_1_8.toString()
}
