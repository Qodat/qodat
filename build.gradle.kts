import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {

    kotlin("jvm") version "1.4.32"
    application
//    id("org.openjfx.javafxplugin") version "0.0.9"
//    id("org.beryx.jlink") version "2.22.0"
}

group = "stan.qodat"
version = "0.0.3"

repositories {
    maven(url = "https://repo.runelite.net")
    mavenCentral()
    jcenter()
}

dependencies {
    implementation(group = "net.runelite", name = "cache", version = "1.7.7") {
        exclude(group = "com.google.common")
    }
    implementation("org.orbisgis:poly2tri-core:0.1.2")
    implementation("com.displee:rs-cache-library:6.8")
    testImplementation(kotlin("test-junit"))
}

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
    mainModule.set("stan.qodat")
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