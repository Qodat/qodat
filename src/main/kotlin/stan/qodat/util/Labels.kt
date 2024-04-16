package stan.qodat.util

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import stan.qodat.Properties
import kotlin.io.path.createFile
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText

object LabelMapping {

    private val labelData by lazy { load() }

    private fun load(): MutableMap<String, String> {
        val labelData = mutableMapOf<String, String>()
        Properties.labelMappingPath.value?.let { path ->
            if (path.exists())
                labelData.putAll(Json.decodeFromString(path.readText()))
        }
        return labelData
    }

    private fun save() {
        Properties.labelMappingPath.value?.let { path ->
            if (!path.exists())
                path.createFile()
            path.writeText(Json.encodeToString(labelData))
        }
    }

    operator fun get(key: String): String? {
        return labelData[key]
    }

    operator fun set(key: String, value: String) {
        labelData[key] = value
        save()
    }
}
