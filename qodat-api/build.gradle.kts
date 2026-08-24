version = "0.0.2"

dependencies {
    api(libs.runelite.cache) {
        exclude(group = "com.google.guava")
    }
}
