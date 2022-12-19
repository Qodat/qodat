version = "0.0.1"

dependencies {
    api(group = "net.runelite", name = "cache", version = "1.9.6-SNAPSHOT") {
        exclude(group = "com.google.common")
    }
}
