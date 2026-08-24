package stan.qodat

import java.util.Properties

/**
 * Runtime app version from [version.properties], the JAR manifest, or `qodat.version`.
 */
object AppVersion {

    val value: String by lazy { load() }

    val windowTitle: String
        get() = "Qodat $value"

    internal fun parse(text: String): String? {
        val props = Properties()
        props.load(text.reader())
        return props.getProperty("version")?.trim()?.takeIf { it.isNotEmpty() }
    }

    private fun load(): String {
        AppVersion::class.java.getResourceAsStream("version.properties")?.use { stream ->
            parse(stream.bufferedReader().readText())?.let { return it }
        }
        AppVersion::class.java.`package`?.implementationVersion?.let { return it }
        return System.getProperty("qodat.version")?.takeIf { it.isNotBlank() } ?: "dev"
    }
}
