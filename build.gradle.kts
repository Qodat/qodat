import org.gradle.internal.os.OperatingSystem

plugins {
    id("org.openjfx.javafxplugin") version "0.0.11"
    id("org.beryx.runtime") version "1.12.7"
    kotlin("jvm") version "1.6.0"
    kotlin("plugin.serialization") version "1.6.0"
    application
}

group = "stan.qodat"
version = "0.0.8"

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
//    implementation("no.tornado:tornadofx:1.7.19")
//    implementation("com.github.kotlin-graphics:gln:v0.5.2")
    implementation("org.jsoup:jsoup:1.11.3")
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
//    JavaFXPlatform.values().forEach { platform ->
//        val cfg = configurations.create("javafx_" + platform.classifier)
//        JavaFXModule.getJavaFXModules(javaFXOptions.modules).forEach { m ->
//            project.dependencies.add(cfg.name,
//                String.format("org.openjfx:%s:%s:%s", m.artifactName, javaFXOptions.version, platform.classifier));
//        }
//    }
//    implementation("com.github.kotlin-graphics:gln:v0.5.2")
    testImplementation(kotlin("test-junit"))
}


//jlink {
//    launcher {
//        name = "hellofx"
//    }
//}

sourceSets {
    named("main") {
        java.srcDir("src/main/kotlin")
    }
}

application {
//    mainModule.set("stan")
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

//    addModules("java.compiler", "javafx.media")

    targetPlatform("win", "/Users/stanvanderbend/Documents/jdk-17.0.2")
//    targetPlatform("mac")
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
//    withType(CreateStartScripts::class).forEach {script ->
//        script.doFirst {
//            script.classpath =  files("lib/*")
//        }
//    }
    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions.jvmTarget = "17"
    }
//    withType<Jar> {
//        duplicatesStrategy = DuplicatesStrategy.INCLUDE
//        manifest {
//            attributes("Main-Class" to "stan.qodat.Qodat")
//        }
//        from(sourceSets.main.get().output)
//        dependsOn(configurations.runtimeClasspath)
//        from({
//            configurations.runtimeClasspath.get().filter { it.name.endsWith("jar") }.map { zipTree(it) }
//        })
//    }
//    this["runtime"].doLast {
//        JavaFXPlatform.values().forEach { platform ->
//            val cfg = configurations["javafx_" + platform.classifier]
//            cfg.resolvedConfiguration.files.forEach { f ->
//                copy {
//                    from(f)
//                    into("build/image/hello-${platform.classifier}/lib")
//                }
//            }
//        }
//    }
}