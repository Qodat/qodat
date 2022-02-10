plugins{
    kotlin("jvm")
}

group = "stan.qodat"
version = "0.0.6"


repositories {
    mavenCentral()
    maven(url = "https://repo.runelite.net")
}

dependencies {
    implementation(group = "net.runelite", name = "cache", version = "1.8.9") {
        exclude(group = "com.google.common")
    }
}

sourceSets {
    named("main") {
        java.srcDir("src/main/kotlin")
    }
}